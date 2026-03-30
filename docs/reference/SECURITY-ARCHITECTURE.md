# 安全架构设计参考

> 从 SafeVault 提取的三层密钥体系和安全组件设计，适配为 SecureVault 离线场景。

---

## 一、密钥层次体系

### 1.1 架构总览

SecureVault 使用简化的三层密钥架构（移除了 SafeVault 的非对称密钥层，因为离线场景不需要分享加密）：

```
┌──────────────────────────────────────────────────┐
│ Level 3: 用户数据层                               │
│ ┌──────────────┐ ┌────────────┐ ┌─────────────┐ │
│ │ 密码条目      │ │ 安全笔记   │ │ 导出文件     │ │
│ │ (XChaCha20-Poly1305) │ │ (XChaCha20-Poly1305) │ │ (XChaCha20-Poly1305) │ │
│ └──────┬───────┘ └─────┬──────┘ └──────┬──────┘ │
│        └───────────────┼───────────────┘         │
│                        │ DataKey                  │
└────────────────────────┼─────────────────────────┘
                         │
┌────────────────────────┼─────────────────────────┐
│ Level 2: DataKey 层    │                          │
│                        │                          │
│  ┌─────────────────────┴──────────────────────┐  │
│  │              DataKey (AES-256)              │  │
│  │         随机生成，永不直接暴露               │  │
│  └──────┬─────────────────────────┬───────────┘  │
│         │                         │               │
│  PasswordKey 加密副本        DeviceKey 加密副本   │
│  (主密码恢复路径)            (生物识别快速路径)   │
│         │                         │               │
│  encrypted_data_key_password   encrypted_data_key_device │
│  (存储在 vault_config)        (存储在 vault_config)      │
└─────────┼─────────────────────────┼──────────────┘
          │                         │
┌─────────┼─────────────────────────┼──────────────┐
│ Level 1: 根密钥层                                 │
│                                                   │
│  ┌────────────────────┐  ┌─────────────────────┐ │
│  │    PasswordKey      │  │     DeviceKey        │ │
│  │                     │  │                      │ │
│  │ Argon2id(           │  │ 平台 KeyStore 生成   │ │
│  │   主密码,           │  │ 硬件绑定             │ │
│  │   salt,             │  │ 生物识别授权         │ │
│  │   自适应参数         │  │ 30 秒有效窗口        │ │
│  │ ) → 256-bit key     │  │ AES-256 key          │ │
│  └────────────────────┘  └─────────────────────┘ │
└──────────────────────────────────────────────────┘
```

### 1.2 与 SafeVault 架构的差异

| 层级 | SafeVault | SecureVault | 原因 |
|------|-----------|-------------|------|
| Level 1 | PasswordKey + DeviceKey | **相同** | 核心机制不变 |
| Level 2 | DataKey（双重加密） | **相同** | 核心机制不变 |
| Level 3（数据） | XChaCha20-Poly1305 加密 | **相同** | 核心机制不变 |
| Level 3（分享） | X25519/Ed25519/RSA | **移除** | 离线不需要密钥交换 |
| 云端密钥 | 固定 Argon2 参数 (128MB) | **移除** | 无云端同步 |
| HKDF | 分享密钥派生 | **移除** | 无分享功能 |

---

## 二、会话管理器 (SessionManager)

### 2.1 状态机

```
┌────────┐   输入主密码/生物识别    ┌──────────┐
│  锁定  │ ──────────────────────→ │  解锁中  │
│ LOCKED │                         │ UNLOCKING│
└────────┘                         └────┬─────┘
    ↑                                   │
    │                              验证成功 → 缓存 DataKey
    │                                   │
    │   超时/手动锁定/切后台      ┌─────▼────┐
    └──────────────────────────── │  已解锁  │
                                  │ UNLOCKED │
                                  └──────────┘
```

### 2.2 实现设计

> **注**：下列片段为结构示意。具体 API（如 `unlock`/`lock`、`CryptoConstants.Session` 超时、是否注入 `ConfigRepository`）以仓库源码 `shared/common/.../SessionManager.kt` 为准。跨线程行为见 **2.4**。

```kotlin
class SessionManager(
    private val configRepository: ConfigRepository
) {
    private var dataKey: SensitiveData<ByteArray>? = null
    private var lastAccessTime: Long = 0L
    private var lastBackgroundTime: Long = 0L

    val isUnlocked: Boolean
        get() = dataKey != null && !dataKey!!.isClosed()

    fun cacheDataKey(key: ByteArray) {
        dataKey?.close()  // 清除旧的
        dataKey = SensitiveData(key.copyOf())
        lastAccessTime = currentTimeMillis()
    }

    fun getDataKey(): ByteArray {
        check(isUnlocked) { "Vault is locked" }
        extendSession()
        return dataKey!!.get()
    }

    fun requireUnlocked() {
        if (!isUnlocked) throw SessionLockedException()
    }

    fun lock() {
        dataKey?.close()
        dataKey = null
        lastAccessTime = 0L
    }

    fun extendSession() {
        lastAccessTime = currentTimeMillis()
    }

    fun onAppBackground() {
        val timeout = configRepository.getAutoLockTimeout()
        if (timeout == -1L) {
            lock() // 严格立即：进入后台即锁定
            return
        }
        lastBackgroundTime = currentTimeMillis()
    }

    fun onAppForeground() {
        val timeout = configRepository.getAutoLockTimeout()
        val elapsed = currentTimeMillis() - lastBackgroundTime

        when {
            timeout == Long.MAX_VALUE -> { /* 永不锁定 */ }
            timeout >= 0L && elapsed >= timeout -> lock() // 后台超时锁定
        }
    }

    // 安全执行：在解锁状态下执行操作
    inline fun <T> withUnlockedSession(block: (ByteArray) -> T): T {
        requireUnlocked()
        return block(getDataKey())
    }
}

class SessionLockedException : Exception("Vault session is locked")
```

### 2.3 自动锁定策略

| 模式 | 超时值 | 说明 |
|------|-------|------|
| 后台后严格立即锁定 | -1 | 进入后台即锁定（不等待阈值） |
| 1 分钟 | 60,000 ms | 适合高安全需求 |
| 5 分钟 | 300,000 ms | 平衡安全与便利 |
| 15 分钟 | 900,000 ms | 适合桌面使用 |
| 30 分钟 | 1,800,000 ms | 低安全需求场景 |
| 永不自动锁定 | `Long.MAX_VALUE` | 不推荐 |

> 兼容说明：历史配置中的 `1000 ms`（旧“立即”近似值）在当前实现中会按“严格立即”语义处理。

### 2.4 并发与 Libsodium 初始化（实现）

| 主题 | 实现要点 |
|------|----------|
| **SessionManager 线程模型** | 所有公开**实例**方法在 `synchronized(this)` 下访问 `dataKey`、`isUnlocked`、后台计时与 `StateFlow` 更新，避免 UI、`KeyManager` 协程与 Autofill 并发交错破坏「已解锁 / 密钥已清除」的一致性。 |
| **Libsodium** | `LibsodiumManager.initialize()` 内使用 `Mutex` 做一次性初始化；Android 在 `SecureVaultApp.onCreate` 中于 `Dispatchers.Default` 协程预热；Desktop 在 `desktopApp` 入口 `runBlocking(Dispatchers.Default)` 中完成初始化后再进入 Compose 窗口。同步入口 `ensureInitialized()` 使用 `runBlocking(Dispatchers.Default)`，将重活调度到默认线程池。 |
| **保险库列表异步** | `VaultViewModel.loadEntries` 使用 `loadRequestId` 丢弃过期结果，并在 `finally` 中仅在 `requestId == loadRequestId` 时落下 `isLoading = false`，避免取消后永远加载中。`NavGraph` 中 `VaultRoute` 使用 `DisposableEffect`，在离开该路由组合时调用 `onLeaveVaultList()` 取消进行中的列表加载与防抖任务。 |

OpenSpec 能力规范：`openspec/specs/session-runtime/spec.md`、`openspec/specs/vault-ui/spec.md`。

---

## 三、密钥管理器 (KeyManager)

### 3.1 首次设置流程

```
用户设置主密码
    ↓
1. 生成随机 salt (16 bytes)
2. Argon2id(主密码, salt) → PasswordKey
3. 生成随机 DataKey (32 bytes, AES-256)
4. XChaCha20-Poly1305(DataKey, PasswordKey) → encrypted_data_key_password
5. 存储: salt, encrypted_data_key_password → vault_config
6. SessionManager.cacheDataKey(DataKey)
    ↓
保险库已创建并解锁
```

### 3.2 主密码解锁流程

```
用户输入主密码
    ↓
1. 读取 salt 从 vault_config
2. Argon2id(主密码, salt) → PasswordKey
3. 读取 encrypted_data_key_password 从 vault_config
4. XChaCha20-Poly1305 解密 → DataKey
5. 验证 DataKey 有效性（尝试解密一个校验值）
6. SessionManager.cacheDataKey(DataKey)
    ↓
保险库已解锁
```

### 3.3 生物识别设置流程

```
用户已解锁（DataKey 在缓存中）→ 用户启用生物识别
    ↓
1. PlatformKeyStore.generateDeviceKey()
   Android: AndroidKeyStore AES-256, 需要生物识别授权
   iOS: Keychain, Secure Enclave, LAContext
   Desktop: 系统密钥库
2. 获取 DeviceKey
3. XChaCha20-Poly1305(DataKey, DeviceKey) → encrypted_data_key_device
4. 存储: encrypted_data_key_device → vault_config
    ↓
生物识别已绑定
```

### 3.4 生物识别解锁流程

```
用户触发生物识别
    ↓
1. BiometricAuth.authenticate() → 成功
2. PlatformKeyStore.getDeviceKey() → DeviceKey
3. 读取 encrypted_data_key_device 从 vault_config
4. XChaCha20-Poly1305 解密 → DataKey
5. SessionManager.cacheDataKey(DataKey)
    ↓
保险库已解锁
```

### 3.5 实现

```kotlin
class KeyManager(
    private val argon2Kdf: Argon2Kdf,
    private val aesGcmCipher: AesGcmCipher,
    private val platformKeyStore: PlatformKeyStore,
    private val configRepository: ConfigRepository,
    private val sessionManager: SessionManager
) {
    // 首次设置
    suspend fun setupVault(masterPassword: CharArray): VaultResult<Unit> {
        return try {
            val salt = argon2Kdf.generateSalt()
            val passwordKey = argon2Kdf.deriveKey(masterPassword, salt)

            val dataKey = CryptoUtils.generateSecureRandom(32)

            val encryptedDataKey = aesGcmCipher.encrypt(dataKey, passwordKey)

            configRepository.setSalt(CryptoUtils.base64Encode(salt))
            configRepository.setEncryptedDataKeyPassword(encryptedDataKey.toStorageFormat())

            sessionManager.cacheDataKey(dataKey)

            MemorySanitizer.secureWipe(passwordKey)
            VaultResult.Success(Unit)
        } catch (e: Exception) {
            VaultResult.Error(VaultError.CryptoError(e.message ?: "Setup failed"))
        } finally {
            MemorySanitizer.secureWipe(masterPassword)
        }
    }

    // 主密码解锁
    suspend fun unlockWithPassword(masterPassword: CharArray): VaultResult<Unit> {
        return try {
            val salt = CryptoUtils.base64Decode(configRepository.getSalt())
            val passwordKey = argon2Kdf.deriveKey(masterPassword, salt)

            val encryptedDataKey = EncryptedData.fromStorageFormat(
                configRepository.getEncryptedDataKeyPassword()
            )
            val dataKey = aesGcmCipher.decrypt(encryptedDataKey, passwordKey)

            sessionManager.cacheDataKey(dataKey)

            MemorySanitizer.secureWipe(passwordKey)
            MemorySanitizer.secureWipe(dataKey)
            VaultResult.Success(Unit)
        } catch (e: Exception) {
            VaultResult.Error(VaultError.InvalidPassword)
        } finally {
            MemorySanitizer.secureWipe(masterPassword)
        }
    }

    // 生物识别注册
    suspend fun enrollBiometric(): VaultResult<Unit> {
        sessionManager.requireUnlocked()

        return try {
            val deviceKey = platformKeyStore.getOrCreateDeviceKey()
            val dataKey = sessionManager.getDataKey()
            val encryptedDataKey = aesGcmCipher.encrypt(dataKey, deviceKey)

            configRepository.setEncryptedDataKeyDevice(encryptedDataKey.toStorageFormat())
            configRepository.setBiometricEnabled(true)

            VaultResult.Success(Unit)
        } catch (e: Exception) {
            VaultResult.Error(VaultError.BiometricFailed)
        }
    }

    // 生物识别解锁
    suspend fun unlockWithBiometric(): VaultResult<Unit> {
        return try {
            val deviceKey = platformKeyStore.getDeviceKey()
                ?: return VaultResult.Error(VaultError.BiometricNotAvailable)

            val encryptedDataKey = EncryptedData.fromStorageFormat(
                configRepository.getEncryptedDataKeyDevice()
            )
            val dataKey = aesGcmCipher.decrypt(encryptedDataKey, deviceKey)

            sessionManager.cacheDataKey(dataKey)

            MemorySanitizer.secureWipe(dataKey)
            VaultResult.Success(Unit)
        } catch (e: Exception) {
            VaultResult.Error(VaultError.BiometricFailed)
        }
    }

    // 修改主密码
    suspend fun changeMasterPassword(
        currentPassword: CharArray,
        newPassword: CharArray
    ): VaultResult<Unit> {
        return try {
            // 1. 验证当前密码
            val currentResult = unlockWithPassword(currentPassword)
            if (currentResult is VaultResult.Error) return currentResult

            // 2. 用新密码重新加密 DataKey
            val newSalt = argon2Kdf.generateSalt()
            val newPasswordKey = argon2Kdf.deriveKey(newPassword, newSalt)
            val dataKey = sessionManager.getDataKey()
            val encryptedDataKey = aesGcmCipher.encrypt(dataKey, newPasswordKey)

            configRepository.setSalt(CryptoUtils.base64Encode(newSalt))
            configRepository.setEncryptedDataKeyPassword(encryptedDataKey.toStorageFormat())

            MemorySanitizer.secureWipe(newPasswordKey)
            VaultResult.Success(Unit)
        } catch (e: Exception) {
            VaultResult.Error(VaultError.CryptoError(e.message ?: "Change password failed"))
        } finally {
            MemorySanitizer.secureWipe(newPassword)
        }
    }
}
```

---

## 四、生物识别认证

### 4.1 状态管理

从 SafeVault 的 `BiometricState` 提取的防暴力破解机制：

```kotlin
class BiometricState {
    companion object {
        const val DEBOUNCE_WINDOW_MS = 1_000L     // 防抖窗口
        const val MAX_CONSECUTIVE_FAILURES = 3     // 最大连续失败次数
        const val LOCKOUT_DURATION_MS = 30_000L   // 锁定持续时间
    }

    private var consecutiveFailures = 0
    private var lockoutEndTime = 0L
    private var lastAuthTime = 0L

    fun isLockedOut(): Boolean =
        currentTimeMillis() < lockoutEndTime

    fun getRemainingLockoutTime(): Long =
        maxOf(0L, lockoutEndTime - currentTimeMillis())

    fun shouldDebouncePrompt(): Boolean =
        currentTimeMillis() - lastAuthTime < DEBOUNCE_WINDOW_MS

    fun recordSuccess() {
        consecutiveFailures = 0
        lockoutEndTime = 0L
        lastAuthTime = currentTimeMillis()
    }

    fun recordFailure() {
        consecutiveFailures++
        lastAuthTime = currentTimeMillis()
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            lockoutEndTime = currentTimeMillis() + LOCKOUT_DURATION_MS
            consecutiveFailures = 0
        }
    }

    fun reset() {
        consecutiveFailures = 0
        lockoutEndTime = 0L
        lastAuthTime = 0L
    }
}
```

### 4.2 跨平台接口

```kotlin
// commonMain
expect class BiometricAuth {
    fun isAvailable(): Boolean
    fun getReason(): BiometricUnavailableReason?
    suspend fun authenticate(
        title: String,
        subtitle: String
    ): BiometricResult
}

sealed class BiometricResult {
    object Success : BiometricResult()
    data class Failure(val error: String) : BiometricResult()
    object Cancelled : BiometricResult()
    object Lockout : BiometricResult()
}

enum class BiometricUnavailableReason {
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NO_BIOMETRICS_ENROLLED,
    SECURITY_UPDATE_REQUIRED
}
```

---

## 五、安全模式设计

### 5.1 核心原则

安全模式下保存的密码**绝不**以明文形式出现在 UI 层：

```
普通模式密码:
  UI 可以显示明文 → PasswordDetailScreen 显示 "MyP@ssword123"

安全模式密码:
  UI 只能看到引用 → PasswordDetailScreen 显示 "••••••••"
  只能通过 "复制" 或 "自动填充" 使用密码
```

### 5.2 实现机制

```kotlin
class SecurityModeManager(
    private val sessionManager: SessionManager,
    private val aesGcmCipher: AesGcmCipher,
    private val configRepository: ConfigRepository,
    private val clipboard: SecureClipboard
) {
    // 安全模式密码使用独立的 SecureModeKey
    // SecureModeKey = HKDF(DataKey, "securevault-secure-mode")
    private fun deriveSecureModeKey(dataKey: ByteArray): ByteArray {
        return hkdfExpand(
            prk = dataKey,
            info = "securevault-secure-mode".encodeToByteArray(),
            length = 32
        )
    }

    // "使用"安全密码 — 不返回明文，直接操作剪贴板
    suspend fun useSecurePassword(entryId: Long): VaultResult<Unit> {
        return sessionManager.withUnlockedSession { dataKey ->
            val secureModeKey = deriveSecureModeKey(dataKey)
            try {
                val entry = repository.getById(entryId)
                    ?: return@withUnlockedSession VaultResult.Error(VaultError.NotFound)

                // 解密到临时内存
                val password = aesGcmCipher.decryptField(
                    entry.encryptedPassword, secureModeKey
                )

                // 直接复制到剪贴板，不经过 UI
                clipboard.copy(password, "Password")
                clipboard.scheduleAutoClear(30_000)

                // 立即擦除明文
                // (Kotlin String 不可变，这是安全模式的已知限制)
                // 建议使用 CharArray 版本的 API

                VaultResult.Success(Unit)
            } finally {
                MemorySanitizer.secureWipe(secureModeKey)
            }
        }
    }

    // 导出安全密码库
    suspend fun exportSecureVault(): VaultResult<ExportData> {
        return sessionManager.withUnlockedSession { dataKey ->
            val exportKey = CryptoUtils.generateSecureRandom(32)
            try {
                val entries = repository.getAllSecureModeEntries()
                val serialized = serializeEntries(entries)
                val encrypted = aesGcmCipher.encrypt(serialized, exportKey)
                val encryptedExportKey = aesGcmCipher.encrypt(exportKey, dataKey)

                VaultResult.Success(ExportData(
                    version = "1.0",
                    type = "secure_export",
                    encryptedKey = encryptedExportKey.toStorageFormat(),
                    encryptedData = encrypted.toStorageFormat(),
                    createdAt = currentTimeMillis()
                ))
            } finally {
                MemorySanitizer.secureWipe(exportKey)
            }
        }
    }
}
```

---

## 六、安全检查清单

### 6.1 核心安全要求

| 检查项 | 实现方式 | 所在模块 |
|-------|---------|---------|
| 密码不以明文存储 | XChaCha20-Poly1305 逐字段加密 | `PasswordRepository` |
| 主密码不存储 | 仅用于派生 PasswordKey | `KeyManager` |
| DataKey 不持久化明文 | 仅在会话中缓存 | `SessionManager` |
| 敏感数据自动清零 | `SensitiveData` + `MemorySanitizer` | `security/` |
| 剪贴板自动清除 | 30 秒超时 | `SecureClipboard` |
| 屏幕截图防护 | `FLAG_SECURE` (Android) | `ScreenSecurity` |
| 会话超时锁定 | 可配置：严格立即 / 1/5/15/30 分钟 / 永不自动锁定 | `SessionManager` |
| 生物识别保护 | 平台 KeyStore 硬件绑定 | `PlatformKeyStore` |
| 生物识别防暴力 | 3 次失败后锁定 30 秒 | `BiometricState` |
| 安全填充 | 256 字节块随机填充 | `SecurePadding` |
| 内存多轮覆写 | 3 轮（2 随机 + 1 零） | `MemorySanitizer` |
| 安全模式密码不可见 | 独立密钥 + 不返回明文 | `SecurityModeManager` |

### 6.2 平台特定安全

| 平台 | 安全特性 |
|------|---------|
| **Android** | AndroidKeyStore (TEE/StrongBox), FLAG_SECURE, BiometricPrompt STRONG |
| **iOS** | Keychain + Secure Enclave, LAContext（**平台能力暂缓专项**） |
| **Desktop** | **当前周期以 Windows/DPAPI 为优先方向**；macOS Keychain / Linux libsecret 随桌面发行恢复再强化；进程内存保护 |

---

## 七、导出/导入与同源约束（2026-03-25）

### 7.1 导出加密链路

- `Encrypted` 与 `SecureMode` 导出都依赖当前会话中的 `DataKey`。
- `SecureMode` 在此基础上再引入一次性 `ExportKey`（`DataKey` 先加密 `ExportKey`，再由 `ExportKey` 加密导出数据）。

### 7.2 用户数据迁移文件包含什么

- 用户数据迁移文件用于恢复“解密能力”，内容是：`salt`、Argon2 参数、`encryptedDataKeyPassword` 等密钥恢复材料。
- 文件不包含 `DataKey` 明文。
- 导入迁移文件后，仍需用户输入正确主密码才能恢复同一套 `DataKey`。

### 7.3 相同主密码是否可互导

- 不能仅凭“主密码相同”互相导入。
- 原因：每个保险库初始化时的盐值和密钥材料不同，派生与解封结果不同。
- 当前实现增加了 `keyBinding` 同源校验，不同源的用户数据与加密导出文件会被拒绝导入，并返回可读错误。

### 7.4 Android release 与用户数据导入（2026-03-28）

- **debug 可导入、release 失败不一定是主密码错误。** Release 开启 R8 收缩后，若未保留 JNA 与序列化运行时所需结构，会在解密/Argon2 路径抛出异常；UI 曾统一映射为“请确认备份与主密码”，易误判。
- **JNA / libsodium 绑定：** `multiplatform-crypto-libsodium-bindings` 在 Android JVM 上通过 **JNA** 访问 native。R8 不得剥离 `com.sun.jna.**`（含 `Pointer.peer` 等），否则典型错误为：`can't obtain peer field ID for class com.sun.jna.Pointer`。对应规则维护在 `androidApp/proguard-rules.pro`。
- **kotlinx.serialization：** 用户数据与 `EncryptedData` 存储格式依赖 JSON 反序列化；release 需保留序列化器/descriptor 相关规则（同文件中的 kotlinx.serialization 段）。
- **导入实现要点：** `UserDataTransferManager.import` 对 JSON 做 **UTF-8 BOM 去除与 trim**，避免部分编辑器保存的文件无法解析；登录流程在写入配置后调用 **`KeyManager.clearVaultConfigCache()`** 再解锁，避免内存中仍缓存旧保险库元数据。

### 7.5 Android Autofill 待保存草稿（双路径与存储）

- **Intent 传递：** `AutofillSaveActivity` 在启动 `MainActivity` 时设置 `EXTRA_FROM_AUTOFILL_SAVE` 及 `AutofillIntentKeys` 中的标题/账号/密码/URL 等（见 `MainActivity.toAutofillDraftOrNull()`）。消费草稿后由 `clearAutofillSaveExtrasFromIntent()` 从 Activity intent 上移除。
- **持久化兜底：** `AutofillPendingSaveStore` 在进程可能被系统回收或 OEM 丢弃 extras 时保留同一份草稿；实现为 **EncryptedSharedPreferences**（`sv_autofill_pending_save_enc`），并对旧版明文 `sv_autofill_pending_save` 做一次性读回、加密写回与删除。
- **合并优先级：** `MainActivity.resolveAutofillDraftFromIntentAndStore` — **先 Intent，后 store**；Intent 有效时会 `AutofillPendingSaveStore.clear`，避免两份来源长期并存。

### 7.6 本地库性能与导入原子性（2026-03-30）

- **SQLite WAL：** Android、Desktop、iOS 驱动在库打开后尝试 `PRAGMA journal_mode=WAL`；若驱动或环境拒绝，实现以 `runCatching` 忽略失败，不单独阻断开库。
- **密码库批量导入：** `ImportManager` 在一次 SQL 事务中应用本批解析后的写入；任一写入抛错则**整批回滚**，不会在失败批中留下部分新行。导入结束或失败后均调用 `PasswordRepository.invalidateDecryptCache()`，避免会话缓存与磁盘不一致。
- **会话解密缓存：** `PasswordRepositoryImpl` 可按 `(id, updated_at)` 复用已解密的条目展示数据；`create` / `update` / `delete` / `clear`、批量导入结束、`KeyManager.lock` / `clear` / `clearVaultConfigCache` 会清空或按 id 剔除缓存。
- **主线程与 I/O：** 保险库相关 ViewModel 协程作用域使用 `Dispatchers.IO`，将阻塞型仓库与文件网关操作抬离 UI 线程（与 [AGENTS.md](../../AGENTS.md) 约定一致）。
- **Argon2 存量参数：** `changeMasterPassword` **不**将已存用户的 Argon2 代价位重写为「当前产品默认档」；仅 `setupVault` 与显式迁移/校准类流程（若将来提供）可改写落盘的 Argon2 元数据。规范：`openspec/specs/vault-config/spec.md`。
