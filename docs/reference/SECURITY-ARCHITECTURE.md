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
│ │ (AES-GCM)    │ │ (AES-GCM)  │ │ (AES-GCM)   │ │
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
| Level 3（数据） | AES-GCM 加密 | **相同** | 核心机制不变 |
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

    fun onAppBackgrounded() {
        lastBackgroundTime = currentTimeMillis()
    }

    fun onAppForegrounded() {
        val timeout = configRepository.getAutoLockTimeout()
        val elapsed = currentTimeMillis() - lastBackgroundTime

        when {
            timeout == 0L -> lock()                    // 立即锁定
            timeout == Long.MAX_VALUE -> { /* 永不锁定 */ }
            elapsed >= timeout -> lock()               // 超时锁定
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
| 立即锁定 | 0 ms | 切到后台立即锁定 |
| 1 分钟 | 60,000 ms | 适合高安全需求 |
| 5 分钟 | 300,000 ms | **默认值**，平衡安全与便利 |
| 15 分钟 | 900,000 ms | 适合桌面使用 |
| 30 分钟 | 1,800,000 ms | 低安全需求场景 |
| 永不锁定 | `Long.MAX_VALUE` | 不推荐 |

---

## 三、密钥管理器 (KeyManager)

### 3.1 首次设置流程

```
用户设置主密码
    ↓
1. 生成随机 salt (16 bytes)
2. Argon2id(主密码, salt) → PasswordKey
3. 生成随机 DataKey (32 bytes, AES-256)
4. AES-GCM(DataKey, PasswordKey) → encrypted_data_key_password
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
4. AES-GCM 解密 → DataKey
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
3. AES-GCM(DataKey, DeviceKey) → encrypted_data_key_device
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
4. AES-GCM 解密 → DataKey
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
| 密码不以明文存储 | AES-256-GCM 逐字段加密 | `PasswordRepository` |
| 主密码不存储 | 仅用于派生 PasswordKey | `KeyManager` |
| DataKey 不持久化明文 | 仅在会话中缓存 | `SessionManager` |
| 敏感数据自动清零 | `SensitiveData` + `MemorySanitizer` | `security/` |
| 剪贴板自动清除 | 30 秒超时 | `SecureClipboard` |
| 屏幕截图防护 | `FLAG_SECURE` (Android) | `ScreenSecurity` |
| 会话超时锁定 | 可配置 0~30 分钟 | `SessionManager` |
| 生物识别保护 | 平台 KeyStore 硬件绑定 | `PlatformKeyStore` |
| 生物识别防暴力 | 3 次失败后锁定 30 秒 | `BiometricState` |
| 安全填充 | 256 字节块随机填充 | `SecurePadding` |
| 内存多轮覆写 | 3 轮（2 随机 + 1 零） | `MemorySanitizer` |
| 安全模式密码不可见 | 独立密钥 + 不返回明文 | `SecurityModeManager` |

### 6.2 平台特定安全

| 平台 | 安全特性 |
|------|---------|
| **Android** | AndroidKeyStore (TEE/StrongBox), FLAG_SECURE, BiometricPrompt STRONG |
| **iOS** | Keychain + Secure Enclave, LAContext, 应用审核确保安全 |
| **Desktop** | DPAPI (Win) / Keychain (macOS) / libsecret (Linux), 进程内存保护 |
