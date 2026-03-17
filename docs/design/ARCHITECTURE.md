# SecureVault 系统架构设计

> 版本 1.0 — 2026-03-18

## 一、项目定位

| 项目 | 说明 |
|------|------|
| **名称** | SecureVault |
| **定位** | 离线优先的全平台密码管理器 |
| **核心原则** | 零网络依赖、本地加密、全平台一致 |

### 核心功能

1. **密码库管理** — 加密存储密码，支持搜索、分类、收藏
2. **自动填充** — Android AutofillService / iOS CredentialProvider / Desktop 剪贴板
3. **智能保存** — 自动检测新密码并提示保存或更新
4. **安全模式** — 密码不可查看，只可使用（复制/填充），支持加密导出

---

## 二、分层架构

```
┌──────────────────────────────────────────────────────────────┐
│                    Platform Entry Points                      │
│  androidApp / iosApp / desktopApp                            │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                 UI Layer (composeApp)                         │
│  Screens ─── Components ─── Theme ─── Navigation             │
│  (Compose Multiplatform, 约 80% 共享代码)                     │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│              ViewModel Layer (composeApp)                     │
│  VaultViewModel ─── SettingsViewModel ─── SecurityViewModel  │
│  (StateFlow, Coroutines)                                     │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│              UseCase Layer (shared/common)                    │
│  UnlockVaultUseCase ─── AddPasswordUseCase                   │
│  SearchPasswordUseCase ─── ExportVaultUseCase                │
│  SecurityModeUseCase ─── ...                                 │
│  (纯 Kotlin, 100% 共享)                                      │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│             Domain Layer (shared/common)                      │
│  Models (PasswordEntry, VaultConfig, SecuritySettings)       │
│  Repository Interfaces (PasswordRepository, ConfigRepository)│
│  (纯 Kotlin, 100% 共享)                                      │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│              Data Layer (shared/common + platform)            │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │  SQLDelight  │  │ Crypto Engine│  │ Platform KeyStore  │  │
│  │  (共享 SQL)  │  │ (共享逻辑)   │  │ (expect/actual)    │  │
│  └─────────────┘  └──────────────┘  └────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## 三、模块职责

### 3.1 shared/common — 核心共享模块

所有平台无关的业务逻辑，约占总代码量的 90%。

```
shared/common/src/commonMain/kotlin/
├── crypto/                  # 加密引擎
│   ├── Argon2Kdf.kt         # Argon2id 密钥派生接口
│   ├── AesGcmCipher.kt      # AES-256-GCM 加密/解密
│   ├── SecurePadding.kt     # 安全随机填充
│   ├── CryptoConstants.kt  # 加密常量
│   └── CryptoUtils.kt      # 加密工具函数
│
├── security/                # 安全模块
│   ├── SessionManager.kt    # 会话管理（DataKey 缓存、超时锁定）
│   ├── SensitiveData.kt     # 敏感数据包装器（自动清零）
│   ├── MemorySanitizer.kt  # 内存安全擦除
│   ├── SecurityMode.kt     # 安全模式管理器
│   ├── KeyManager.kt       # 密钥管理接口
│   └── PlatformKeyStore.kt # expect 声明，各平台 actual 实现
│
├── storage/                 # 存储模块
│   ├── PasswordRepository.kt    # 密码仓库接口
│   ├── ConfigRepository.kt      # 配置仓库接口
│   ├── PasswordRepositoryImpl.kt # 实现
│   └── ExportManager.kt         # 导入/导出管理
│
├── model/                   # 数据模型
│   ├── PasswordEntry.kt     # 密码条目（明文）
│   ├── EncryptedPasswordEntry.kt # 密码条目（加密后）
│   ├── EncryptedData.kt     # 通用加密数据格式
│   ├── VaultConfig.kt       # 保险库配置
│   ├── SecuritySettings.kt  # 安全设置
│   └── ExportData.kt        # 导出数据格式
│
├── usecase/                 # 业务用例
│   ├── UnlockVaultUseCase.kt     # 解锁保险库
│   ├── LockVaultUseCase.kt       # 锁定保险库
│   ├── AddPasswordUseCase.kt     # 添加密码
│   ├── UpdatePasswordUseCase.kt  # 更新密码
│   ├── DeletePasswordUseCase.kt  # 删除密码
│   ├── SearchPasswordUseCase.kt  # 搜索密码
│   ├── GeneratePasswordUseCase.kt # 生成密码
│   ├── ExportVaultUseCase.kt     # 导出密码库
│   ├── ImportVaultUseCase.kt     # 导入密码库
│   ├── ToggleSecurityModeUseCase.kt # 切换安全模式
│   └── UseSecurePasswordUseCase.kt  # 使用安全模式密码
│
└── util/                    # 工具
    ├── PasswordStrengthCalculator.kt # 密码强度评估
    ├── PasswordGenerator.kt          # 密码生成器
    ├── ClipboardManager.kt           # 剪贴板管理（expect/actual）
    └── DateTimeUtils.kt             # 时间工具
```

### 3.2 shared/android — Android 平台实现

```
shared/android/src/androidMain/kotlin/
├── crypto/
│   └── AndroidArgon2Kdf.kt       # argon2kt JNI 实现
├── security/
│   ├── AndroidKeyStoreProvider.kt # AndroidKeyStore
│   ├── BiometricHelper.kt        # BiometricPrompt
│   └── ScreenSecurity.kt         # FLAG_SECURE
├── autofill/
│   ├── SecureVaultAutofillService.kt # AutofillService
│   ├── AutofillParser.kt            # 表单解析
│   ├── AutofillMatcher.kt           # 凭证匹配
│   └── FillResponseBuilder.kt       # 填充响应构建
└── storage/
    └── AndroidSqlDriverFactory.kt    # Android SQLite 驱动
```

### 3.3 shared/ios — iOS 平台实现

```
shared/ios/src/iosMain/kotlin/
├── crypto/
│   └── IosArgon2Kdf.kt           # libsodium crypto_pwhash
├── security/
│   ├── IosKeychainProvider.kt     # Keychain + Secure Enclave
│   └── BiometricHelper.kt        # LAContext (Face/Touch ID)
├── autofill/
│   └── CredentialProviderBridge.kt # iOS ASCredentialProvider 桥接
└── storage/
    └── IosSqlDriverFactory.kt      # iOS SQLite 驱动
```

### 3.4 shared/desktop — Desktop 平台实现

```
shared/desktop/src/desktopMain/kotlin/
├── crypto/
│   └── DesktopArgon2Kdf.kt       # libsodium crypto_pwhash
├── security/
│   ├── DesktopKeyStorage.kt      # DPAPI / Keychain / libsecret
│   └── SystemLockHelper.kt       # 系统锁屏认证
└── storage/
    └── DesktopSqlDriverFactory.kt # Desktop SQLite 驱动
```

### 3.5 composeApp — UI 层

```
composeApp/src/commonMain/kotlin/
├── ui/
│   ├── screens/
│   │   ├── VaultScreen.kt          # 密码库列表
│   │   ├── PasswordDetailScreen.kt # 密码详情
│   │   ├── AddEditPasswordScreen.kt # 添加/编辑
│   │   ├── GeneratorScreen.kt      # 密码生成器
│   │   ├── SettingsScreen.kt       # 设置
│   │   ├── SecurityModeScreen.kt   # 安全模式设置
│   │   ├── ExportImportScreen.kt   # 导入/导出
│   │   ├── UnlockScreen.kt         # 解锁界面
│   │   └── SetupScreen.kt          # 首次设置
│   ├── components/
│   │   ├── PasswordCard.kt         # 密码卡片
│   │   ├── PasswordStrengthBar.kt  # 强度指示器
│   │   ├── SearchBar.kt            # 搜索栏
│   │   ├── BiometricPromptUI.kt    # 生物识别提示
│   │   ├── SavePasswordDialog.kt   # 保存密码弹窗
│   │   ├── CategoryChips.kt        # 分类标签
│   │   └── LoadingOverlay.kt       # 加载遮罩
│   ├── theme/
│   │   ├── Theme.kt                # 主题定义（亮/暗/跟随系统）
│   │   ├── Color.kt                # 颜色
│   │   └── Typography.kt           # 字体
│   └── navigation/
│       └── NavGraph.kt             # 导航图
│
├── viewmodel/
│   ├── VaultViewModel.kt           # 密码库
│   ├── PasswordDetailViewModel.kt  # 密码详情
│   ├── AddEditPasswordViewModel.kt # 添加/编辑
│   ├── GeneratorViewModel.kt       # 密码生成器
│   ├── SettingsViewModel.kt        # 设置
│   ├── SecurityModeViewModel.kt    # 安全模式
│   ├── UnlockViewModel.kt          # 解锁
│   └── ExportImportViewModel.kt    # 导入/导出
│
└── di/
    └── AppModule.kt                # Koin 模块定义
```

---

## 四、核心数据流

### 4.1 解锁流程

```
用户输入主密码（或生物识别）
    ↓
┌─────────────────────────────────┐
│ UnlockVaultUseCase              │
│ 1. Argon2id(password, salt)     │
│    → PasswordKey                │
│ 2. AES-GCM 解密 encrypted_data_key │
│    → DataKey                    │
│ 3. SessionManager.cacheDataKey() │
│ 4. 标记 isUnlocked = true       │
└─────────────────────────────────┘
    ↓
密码库已解锁，可进行 CRUD 操作
```

### 4.2 密码保存流程

```
用户添加密码（或自动填充检测到新密码）
    ↓
┌─────────────────────────────────┐
│ AddPasswordUseCase              │
│ 1. SessionManager.requireUnlocked() │
│ 2. 获取 DataKey                  │
│ 3. 对每个字段:                   │
│    a) SecurePadding.pad()       │
│    b) 生成随机 IV (12 bytes)     │
│    c) AES-256-GCM 加密           │
│ 4. PasswordRepository.insert()  │
└─────────────────────────────────┘
```

### 4.3 安全模式流程

```
安全模式密码 — 只能"使用"，不能"查看"
    ↓
┌─────────────────────────────────┐
│ UseSecurePasswordUseCase        │
│ 1. SessionManager.requireUnlocked() │
│ 2. 解密密码到临时内存              │
│ 3. 复制到剪贴板（30 秒自动清除）   │
│    或直接自动填充                  │
│ 4. 立即擦除内存中的明文            │
│ 5. 不向 UI 层返回密码明文          │
└─────────────────────────────────┘
```

### 4.4 导出/导入流程

```
导出（安全模式）
    ↓
┌─────────────────────────────────┐
│ ExportVaultUseCase              │
│ 1. 生成随机 ExportKey            │
│ 2. 用 ExportKey 加密所有密码数据  │
│ 3. 用 DataKey 加密 ExportKey     │
│ 4. 打包为 SecureVaultExport 文件 │
│    {version, encrypted_key,     │
│     encrypted_data, iv, created}│
└─────────────────────────────────┘

导入
    ↓
┌─────────────────────────────────┐
│ ImportVaultUseCase              │
│ 1. 解析文件格式和版本             │
│ 2. 用 DataKey 解密 ExportKey     │
│ 3. 用 ExportKey 解密数据         │
│ 4. 合并或覆盖到本地密码库         │
└─────────────────────────────────┘
```

---

## 五、数据库设计 (SQLDelight)

### 5.1 Schema

```sql
-- 密码条目表
CREATE TABLE password_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    encrypted_title TEXT NOT NULL,
    encrypted_username TEXT NOT NULL,
    encrypted_password TEXT NOT NULL,
    encrypted_url TEXT,
    encrypted_notes TEXT,
    encrypted_tags TEXT,
    iv TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'default',
    is_favorite INTEGER NOT NULL DEFAULT 0,
    security_mode INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- 保险库配置表
CREATE TABLE vault_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

-- 密码生成历史表
CREATE TABLE generated_passwords (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    password TEXT NOT NULL,
    strength INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);

-- 索引
CREATE INDEX idx_password_entries_category ON password_entries(category);
CREATE INDEX idx_password_entries_favorite ON password_entries(is_favorite);
CREATE INDEX idx_password_entries_security ON password_entries(security_mode);
CREATE INDEX idx_password_entries_updated ON password_entries(updated_at DESC);
```

### 5.2 配置项预设

| key | 默认值 | 说明 |
|-----|-------|------|
| `salt` | (首次生成) | Argon2 盐值 (Base64) |
| `encrypted_data_key_password` | (首次生成) | PasswordKey 加密的 DataKey |
| `encrypted_data_key_device` | (首次生成) | DeviceKey 加密的 DataKey |
| `encryption_version` | `v2` | 加密版本 |
| `biometric_enabled` | `0` | 生物识别开关 |
| `auto_lock_timeout` | `300` | 自动锁定超时（秒） |
| `security_mode_enabled` | `0` | 安全模式全局开关 |
| `clipboard_clear_timeout` | `30` | 剪贴板清除超时（秒） |
| `theme_mode` | `system` | 主题模式 |
| `screenshot_protection` | `1` | 截屏保护 |

---

## 六、依赖注入设计 (Koin)

```kotlin
// 共享模块
val sharedModule = module {
    // 加密
    single<Argon2Kdf> { platformArgon2Kdf() }
    single { AesGcmCipher() }
    single { SecurePadding }
    single { KeyManager(get()) }

    // 安全
    single { SessionManager(get()) }
    single { SecurityModeManager(get(), get()) }

    // 存储
    single { createSqlDriver() }
    single { SecureVaultDatabase(get()) }
    single<PasswordRepository> { PasswordRepositoryImpl(get(), get(), get()) }
    single<ConfigRepository> { ConfigRepositoryImpl(get()) }

    // 用例
    factory { UnlockVaultUseCase(get(), get(), get()) }
    factory { AddPasswordUseCase(get(), get(), get()) }
    factory { SearchPasswordUseCase(get()) }
    factory { GeneratePasswordUseCase(get()) }
    factory { ExportVaultUseCase(get(), get(), get()) }
    factory { ImportVaultUseCase(get(), get(), get()) }
    factory { ToggleSecurityModeUseCase(get(), get()) }
    factory { UseSecurePasswordUseCase(get(), get(), get()) }
}

// UI 模块
val viewModelModule = module {
    viewModel { VaultViewModel(get(), get()) }
    viewModel { AddEditPasswordViewModel(get(), get(), get()) }
    viewModel { UnlockViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { GeneratorViewModel(get()) }
    viewModel { SecurityModeViewModel(get(), get()) }
    viewModel { ExportImportViewModel(get(), get()) }
}

// Android 特定
val androidModule = module {
    single<PlatformKeyStore> { AndroidKeyStoreProvider(get()) }
    single { BiometricHelper(get()) }
}
```

---

## 七、expect/actual 接口设计

### 7.1 密钥存储

```kotlin
// commonMain
expect class PlatformKeyStore {
    fun storeDeviceKey(key: ByteArray)
    fun getDeviceKey(): ByteArray?
    fun deleteDeviceKey()
    fun hasDeviceKey(): Boolean
    fun isHardwareBacked(): Boolean
}

// androidMain → AndroidKeyStore
// iosMain → Keychain + Secure Enclave
// desktopMain → DPAPI / Keychain / libsecret
```

### 7.2 Argon2 密钥派生

```kotlin
// commonMain
expect class Argon2Kdf {
    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        memoryKB: Int,
        iterations: Int,
        parallelism: Int,
        outputLength: Int = 32
    ): ByteArray

    fun generateSalt(length: Int = 16): ByteArray
}

// androidMain → argon2kt (JNI)
// iosMain, desktopMain → libsodium crypto_pwhash
```

### 7.3 生物识别

```kotlin
// commonMain
expect class BiometricAuth {
    fun isAvailable(): Boolean
    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (error: String) -> Unit
    )
}

// androidMain → BiometricPrompt
// iosMain → LAContext
// desktopMain → 系统锁屏 / 无操作
```

### 7.4 剪贴板

```kotlin
// commonMain
expect class SecureClipboard {
    fun copy(text: String, label: String = "Password")
    fun clear()
    fun scheduleAutoClear(delayMs: Long = 30_000)
}
```

### 7.5 屏幕安全

```kotlin
// commonMain
expect class ScreenSecurity {
    fun enableScreenshotProtection()
    fun disableScreenshotProtection()
}

// androidMain → FLAG_SECURE
// iosMain → no-op (系统级保护)
// desktopMain → no-op
```

---

## 八、错误处理策略

```kotlin
sealed class VaultResult<out T> {
    data class Success<T>(val data: T) : VaultResult<T>()
    data class Error(val error: VaultError) : VaultResult<Nothing>()
}

sealed class VaultError {
    object SessionLocked : VaultError()
    object InvalidPassword : VaultError()
    object BiometricFailed : VaultError()
    object BiometricNotAvailable : VaultError()
    object CorruptedData : VaultError()
    object ExportFailed : VaultError()
    object ImportFailed : VaultError()
    object ImportVersionMismatch : VaultError()
    data class CryptoError(val message: String) : VaultError()
    data class StorageError(val message: String) : VaultError()
    data class Unknown(val throwable: Throwable) : VaultError()
}
```

---

## 九、UI 主题设计

### 9.1 设计原则

- Material Design 3（Android 和 Desktop 原生感）
- 系统/亮色/暗色三种模式
- Android 12+ 动态颜色
- 紧凑型布局，适应手机和桌面
- 无障碍支持（对比度、触摸目标 ≥ 48dp）

### 9.2 核心界面

| 界面 | 说明 |
|------|------|
| UnlockScreen | 主密码输入 + 生物识别按钮 |
| SetupScreen | 首次使用设置主密码 |
| VaultScreen | 密码列表 + 搜索 + 分类过滤 + FAB |
| PasswordDetailScreen | 密码详情 + 复制 + 编辑 + 删除 |
| AddEditPasswordScreen | 添加/编辑密码 + 强度指示器 |
| GeneratorScreen | 密码生成器 + 配置 + 历史 |
| SettingsScreen | 设置列表（安全、外观、导入/导出） |
| SecurityModeScreen | 安全模式开关和说明 |
| ExportImportScreen | 导入/导出操作 |
