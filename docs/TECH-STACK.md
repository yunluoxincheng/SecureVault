# SecureVault 技术选型文档

> 最终确定版 — 2026-03-18

## 一、选型总览

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **语言** | Kotlin | 2.3.20 | KMP 跨平台核心语言 |
| **UI 框架** | Compose Multiplatform | 1.10.x | 共享 UI 代码（~80%） |
| **业务逻辑** | Kotlin Multiplatform (KMP) | 2.3.20 | 共享核心逻辑（~90%） |
| **数据存储** | SQLDelight | 2.2.x | 跨平台类型安全 SQLite |
| **依赖注入** | Koin | 4.2.x | 跨平台 DI 框架 |
| **异步处理** | Kotlin Coroutines + Flow | 1.10.x | 原生协程与响应式流 |
| **导航** | Compose Navigation 3 | — | CMP 1.10 内置支持 |
| **序列化** | kotlinx-serialization | 1.8.x | 跨平台 JSON/CBOR |
| **构建工具** | Gradle | 9.3.x | KMP 项目标准构建 |
| **Android 插件** | AGP | 9.0.x~9.1.x | Android 构建 |

---

## 二、平台目标

| 平台 | 最低版本 | 说明 |
|------|---------|------|
| **Android** | API 29 (Android 10) | 主要开发平台，AutofillService |
| **iOS** | iOS 15 | CredentialProvider Extension |
| **Windows** | Windows 10 | Desktop 应用 |
| **macOS** | macOS 13 (arm64) / macOS 12 (x64) | Desktop 应用 |
| **Linux** | Ubuntu 20.04+ | Desktop 应用 |

---

## 三、加密层选型

| 组件 | 库 | 说明 |
|------|-----|------|
| **核心加密** | kotlin-multiplatform-libsodium (ionspin) | 跨平台 libsodium 绑定 |
| **AES-256-GCM** | 平台原生 + libsodium | Android: javax.crypto; iOS: CommonCrypto; Desktop: libsodium |
| **Argon2id** | 平台原生实现 (expect/actual) | Android: argon2kt 1.6.0; iOS/Desktop: libsodium Argon2 |
| **密钥存储** | 各平台原生 KeyStore | Android: AndroidKeyStore; iOS: Keychain; Desktop: 系统密钥存储 |
| **安全随机** | kotlin.random / SecureRandom | 平台原生安全随机数 |

### 加密库选型理由

**为什么选 libsodium (ionspin/kotlin-multiplatform-libsodium)：**

1. **跨平台一致性** — 同一套加密原语在所有平台上行为一致
2. **久经审计** — libsodium 是最广泛审计的加密库之一
3. **API 安全** — 难以误用的 API 设计（常量时间比较、自动擦除等）
4. **平台覆盖** — JVM、Native（iOS、Linux、macOS、Windows）全覆盖
5. **Argon2 内置** — libsodium 内含 Argon2id 实现

**Argon2 策略：**
- Android 优先使用 `argon2kt`（JNI 实现，性能更优）
- 其他平台使用 libsodium 的 `crypto_pwhash` (Argon2id)
- 通过 `expect/actual` 抽象为统一接口

---

## 四、平台适配层

| 平台 | 自动填充 | 密钥存储 | 生物识别 | 安全特性 |
|------|---------|---------|---------|---------|
| **Android** | AutofillService API | AndroidKeyStore | BiometricPrompt | FLAG_SECURE |
| **iOS** | ASCredentialProviderExtension | Keychain + Secure Enclave | LAContext (Face/Touch ID) | 无截屏保护（系统级） |
| **Desktop** | 剪贴板 + 全局快捷键 | Windows: DPAPI / macOS: Keychain / Linux: libsecret | 系统锁屏认证 | 进程内存保护 |

---

## 五、项目模块结构

```
SecureVault/
├── shared/                          # KMP 共享模块
│   ├── common/                      # 平台无关的公共代码
│   │   └── src/commonMain/          # 加密、存储、模型、用例
│   ├── android/                     # Android expect/actual
│   ├── ios/                         # iOS expect/actual
│   └── desktop/                     # Desktop expect/actual
│
├── composeApp/                      # Compose Multiplatform UI
│   ├── src/commonMain/              # 共享 UI（Screens, ViewModels, Theme）
│   ├── src/androidMain/             # Android 特定 UI（MainActivity）
│   ├── src/iosMain/                 # iOS 特定 UI
│   └── src/desktopMain/            # Desktop 特定 UI
│
├── androidApp/                      # Android 应用入口 + AutofillService
├── iosApp/                          # iOS 应用入口 + CredentialProvider
├── desktopApp/                      # Desktop 应用入口
│
├── gradle/
│   └── libs.versions.toml           # 版本目录
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 六、版本目录（草案）

```toml
[versions]
kotlin = "2.3.20"
compose-multiplatform = "1.10.2"
agp = "9.0.0"
sqldelight = "2.2.1"
koin = "4.2.0"
coroutines = "1.10.1"
serialization = "1.8.0"
libsodium = "0.9.2"
argon2kt = "1.6.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
sqldelight-driver-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-driver-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
sqldelight-driver-sqlite = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
libsodium-bindings = { module = "com.ionspin.kotlin:multiplatform-crypto-libsodium-bindings", version.ref = "libsodium" }
argon2kt = { module = "com.lambdapioneer.argon2kt:argon2kt", version = "1.6.0" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

---

## 七、关键技术决策

### 7.1 为什么不用 Voyager 导航

Compose Multiplatform 1.10.0 已内置 Navigation 3 支持，提供：
- 灵活的导航栈操作
- 跨平台一致的 API
- 官方维护和长期支持

不再需要引入第三方导航库。

### 7.2 为什么不用 Room

Room 是 Android 专用的 ORM。SQLDelight 提供：
- 真正的跨平台支持（Android、iOS、Desktop）
- SQL-first 的类型安全查询
- 编译时 SQL 验证
- 与 Kotlin Coroutines/Flow 无缝集成

### 7.3 为什么不用 Hilt/Dagger

Hilt/Dagger 是 Android 专用。Koin 提供：
- 纯 Kotlin DSL 的 DI
- 完整的 KMP 支持
- 轻量、无代码生成
- 4.2.0 支持 Compose Navigation 集成

### 7.4 为什么 Argon2 不统一用 libsodium

- Android 上 `argon2kt` (JNI) 性能优于 libsodium 的纯 Kotlin/JVM 绑定
- `argon2kt` 使用直接分配的 ByteBuffer，减少敏感数据在 JVM 堆上的暴露
- 通过 `expect/actual` 对外暴露统一接口，内部实现可以选择最优方案

### 7.5 Kotlin 版本兼容性

| 组件 | Kotlin 2.3.20 兼容性 |
|------|---------------------|
| Gradle | 7.6.3–9.3.0 ✅ |
| AGP | 8.2.2–9.0.0 ✅ |
| Compose Multiplatform | 1.10.x ✅ |
| SQLDelight | 2.2.x ✅ |
| Koin | 4.2.x ✅ |

---

## 八、与 SafeVault 技术栈对比

| 维度 | SafeVault (旧) | SecureVault (新) |
|------|---------------|-----------------|
| 语言 | Java 17 | Kotlin 2.3.20 |
| UI | Android XML + Fragment | Compose Multiplatform |
| 数据库 | Room | SQLDelight |
| DI | ServiceLocator | Koin |
| 异步 | LiveData + 手动线程 | Coroutines + Flow |
| 网络 | Retrofit + OkHttp | 无（纯离线） |
| 导航 | Navigation Component | Compose Navigation 3 |
| 序列化 | Gson | kotlinx-serialization |
| 加密 | BouncyCastle + argon2kt | libsodium + argon2kt |
| 平台 | 仅 Android | 全平台 |
