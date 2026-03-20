# SecureVault 技术选型文档

> 当前实现基线（As-Is）+ 目标版本（To-Be）
> 更新时间：2026-03-19

---

## 相关文档

- [软件需求规格说明书（SRS）](SRS.md)
- [需求追踪矩阵（RTM）](RTM.md)
- [系统测试生命周期（STLC）](STLC.md)
- [系统级测试用例（SLTC）](SLTC.md)

## 一、选型总览

| 层级 | 技术 | 当前实现（As-Is） | 目标版本（To-Be） | 说明 |
|------|------|------------------|-------------------|------|
| 语言 | Kotlin | 2.3.20 | 2.3.20 | KMP 跨平台核心语言 |
| UI 框架 | Compose Multiplatform | 1.10.0 | 1.10.x | 共享 UI 代码 |
| 业务逻辑 | Kotlin Multiplatform | 2.3.20 | 2.3.20 | 共享核心逻辑 |
| 数据存储 | SQLDelight | 2.2.1 | 2.2.x | 跨平台类型安全 SQLite |
| 依赖注入 | Koin | 4.2.0 | 4.2.x | 跨平台 DI |
| 异步处理 | Coroutines + Flow | 1.10.1 | 1.10.x | 并发与响应式流 |
| 序列化 | kotlinx-serialization | 1.8.0 | 1.8.x | JSON 序列化 |
| 构建工具 | Gradle | 9.3.0 | 9.3.x | 由 Wrapper 控制 |
| Android 插件 | AGP | 9.0.0 | 9.0.x | Android 构建插件 |

> 说明：当前实现版本以 `gradle/libs.versions.toml` 与 `gradle/wrapper/gradle-wrapper.properties` 为准。

---

## 二、平台目标

| 平台 | 最低版本 | 当前状态 |
|------|---------|----------|
| Android | API 29 (Android 10) | 已支持 |
| iOS | iOS 15 | 已配置 KMP 目标 |
| Windows | Windows 10 | 已配置 Desktop 目标 |
| macOS | macOS 12+ | 已配置 Desktop 目标 |
| Linux | Ubuntu 20.04+ | 已配置 Desktop 目标 |

### Android SDK 基线（当前实现）

- `minSdk = 29`
- `targetSdk = 36`（Android 16）
- `compileSdk = 36`（Android 16）
- 已启用 `enableOnBackInvokedCallback` 以适配 API 36 返回行为

---

## 三、加密层选型

| 组件 | 当前实现（As-Is） | 目标（To-Be） | 说明 |
|------|------------------|---------------|------|
| 核心加密 | libsodium bindings (ionspin) | 保持 | 跨平台统一加密原语 |
| AEAD | XChaCha20-Poly1305（libsodium SecretBox） | 保持 | 使用随机 nonce |
| Argon2 KDF | Android/Desktop: libsodium pwhash；iOS: libsodium pwhash | Android 可评估 argon2kt | 统一接口 expect/actual |
| 密钥存储 | Android: AndroidKeyStore；iOS/Desktop: 应用层加密存储 | iOS/Desktop 迁移系统密钥存储 | 平台能力逐步完善 |
| 安全随机 | 平台安全随机 + libsodium random | 保持 | 生成 nonce、salt、key |

### Argon2 策略说明

- 当前实现优先保证跨平台一致性，Android、Desktop、iOS 都可用。
- Android 后续可单独评估 `argon2kt`，作为性能优化项，而非当前基线依赖。

---

## 四、平台能力现状

| 平台 | 自动填充 | 密钥存储 | 生物识别 |
|------|---------|---------|---------|
| Android | AutofillService（入口层） | AndroidKeyStore（已实现） | 当前共享层为占位实现 |
| iOS | CredentialProvider（规划/入口） | 当前为应用层加密存储 | 已有 LAContext 实现 |
| Desktop | 剪贴板/快捷键（规划） | 当前为应用层加密存储 | 占位实现 |

---

## 五、项目模块结构（当前）

```text
SecureVault/
├── shared/
│   ├── common/                      # 核心 KMP 模块
│   │   └── src/
│   │       ├── commonMain/          # 共享逻辑（crypto/security/storage/model/usecase）
│   │       ├── androidMain/         # Android actual 实现
│   │       ├── iosMain/             # iOS actual 实现
│   │       ├── desktopMain/         # Desktop actual 实现
│   │       └── commonTest/desktopTest
│   ├── android/                     # Android 平台模块（预留/扩展）
│   ├── ios/                         # iOS 平台模块（预留/扩展）
│   └── desktop/                     # Desktop 平台模块（预留/扩展）
├── composeApp/                      # Compose Multiplatform UI
├── androidApp/                      # Android 应用入口
├── iosApp/                          # iOS 应用入口
└── desktopApp/                      # Desktop 应用入口
```

---

## 六、版本目录（当前实际）

```toml
[versions]
kotlin = "2.3.20"
agp = "9.0.0"
compose-multiplatform = "1.10.0"
sqldelight = "2.2.1"
koin = "4.2.0"
coroutines = "1.10.1"
serialization = "1.8.0"
libsodium = "0.9.2"
argon2kt = "1.6.0"
```

> 完整定义见 `gradle/libs.versions.toml`。

---

## 七、关键技术决策（更新）

### 7.1 导航

- 当前实现使用 Compose Navigation 3（KMP 适配）：`navigation3-ui` + `NavigationState`/`Navigator` + `NavDisplay`。
- 路由已从字符串迁移为类型安全 `NavKey`（`NavRoute` / `MainTabRoute`）。
- Navigation 2（`navigation-compose`）及相关兼容代码已完成清理。

### 7.2 数据层

- 继续使用 SQLDelight 作为跨平台数据库方案。
- 保持 SQL-first 与编译期校验，避免引入平台绑定 ORM。

### 7.3 依赖注入

- 当前使用 Koin 4.2.0，满足 KMP 与 Compose 场景。
- 继续保持与 KMP / Compose 生态版本兼容，按小版本节奏升级。

### 7.4 安全基线

- Android 已使用 AndroidKeyStore 包装设备密钥。
- iOS/Desktop 当前为可用实现，后续升级到系统密钥存储（Keychain/DPAPI/libsecret）。

---

## 八、升级策略建议

- 近期策略：保持当前已对齐基线（Kotlin/AGP/Compose/SQLDelight/Koin），优先功能稳定与测试覆盖。
- 升级方式：按 Kotlin/AGP/Compose/SQLDelight/Koin 分批升级，每批单独回归。
- 时机建议：在 Phase 2 或独立 `chore/upgrade-stack` 分支执行。

### 8.1 AGP 9 兼容备注（当前）

- 目前已升级到 AGP `9.0.0` + Gradle `9.3.0`，并通过编译回归。
- 已移除临时兼容开关（`android.builtInKotlin` / `android.newDsl`），使用 AGP 9 默认模式。
- KMP Android 库模块已迁移到 `com.android.kotlin.multiplatform.library` 插件与 `kotlin { android { ... } }` DSL。
