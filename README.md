# SecureVault

离线优先的全平台密码管理器。

## 项目定位

SecureVault 是一款注重安全性和隐私的密码管理应用，**完全离线运行**，不依赖任何云端服务。所有数据在本地加密存储。

## 核心功能

1. **密码库** — 加密存储密码，支持搜索、分类、收藏
2. **自动填充** — 系统级自动填充（Android AutofillService / iOS CredentialProvider）
3. **智能保存** — 自动检测新密码并提示保存或更新已有记录
4. **安全模式** — 密码不可查看，仅可通过剪贴板或自动填充使用，支持加密导出

## 目标平台

| 平台 | 最低版本 | 自动填充 |
|------|---------|---------|
| Android | API 29 (Android 10) | AutofillService |
| iOS | iOS 15 | CredentialProvider |
| Windows | Windows 10 | 全局快捷键 + 剪贴板 |
| macOS | macOS 13 | 全局快捷键 + 剪贴板 |
| Linux | Ubuntu 20.04+ | 全局快捷键 + 剪贴板 |

## Android 构建基线

- minSdk: 29
- targetSdk: 36（Android 16）
- compileSdk: 36（Android 16）

## Android UI 预览（Compose Preview）

- 预览代码位置：`composeApp/src/androidMain/kotlin/com/securevault/ui/preview/AndroidUiPreviews.kt`
- 预览覆盖范围：基础态、加载态、错误态、空态、边界态（含 Light/Dark 多预览）
- 关键依赖：
	- `compose.components.uiToolingPreview`（`@Preview` 注解）
	- `compose.uiTooling`（Android Studio 预览渲染运行支持）
- 若预览不显示：先执行 `Sync Project with Gradle Files`，再在 Preview 面板点击 `Build & Refresh`。

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 2.3.20 (KMP) |
| UI | Compose Multiplatform 1.10.x |
| 数据库 | SQLDelight 2.2.x |
| DI | Koin 4.2.x |
| 异步 | Kotlin Coroutines + Flow |
| 加密 | libsodium (KMP) + argon2kt (Android) |
| 导航 | Compose Navigation 3 |

## 安全架构

三层密钥体系：

```
Level 1: PasswordKey (Argon2id) + DeviceKey (平台 KeyStore)
Level 2: DataKey (AES-256, 双重加密)
Level 3: 用户数据 (XChaCha20-Poly1305, 逐字段加密)
```

- **Argon2id** 密钥派生（自适应参数）
- **XChaCha20-Poly1305** 数据加密（24 字节 nonce，128 位 Tag）
- **安全随机填充** 防止长度泄露
- **内存安全擦除** 3 轮覆写
- **剪贴板自动清除** 30 秒超时

## 项目结构

```
SecureVault/
├── shared/common/       # 跨平台共享逻辑（加密、安全、存储、模型、用例）
├── shared/android/      # Android 平台实现
├── shared/ios/          # iOS 平台实现
├── shared/desktop/      # Desktop 平台实现
├── composeApp/          # Compose Multiplatform UI
├── androidApp/          # Android 应用入口
├── iosApp/              # iOS 应用入口
├── desktopApp/          # Desktop 应用入口
└── docs/                # 设计文档
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [技术选型](docs/TECH-STACK.md) | 最终技术栈选型和理由 |
| [系统架构](docs/design/ARCHITECTURE.md) | 分层架构、模块职责、数据流 |
| [UI 设计规范](docs/design/UI-DESIGN-SPEC.md) | 颜色、字体、间距、组件、动效、响应式布局 |
| [测试策略与性能基准](docs/design/TESTING-AND-PERFORMANCE.md) | 测试分层、覆盖率目标、性能指标 |
| [加密算法参考](docs/reference/CRYPTO-ALGORITHMS.md) | 从 SafeVault 提取的加密算法设计 |
| [安全架构参考](docs/reference/SECURITY-ARCHITECTURE.md) | 三层密钥体系和安全组件设计 |
| [自动填充设计](docs/reference/AUTOFILL-DESIGN.md) | 跨平台自动填充服务设计 |
| [开发计划](docs/DEVELOPMENT-PLAN.md) | 7 阶段 18 周开发计划 |

## 前身项目

SecureVault 的设计经验和算法验证来自 [SafeVault](../SafeVault)（Android 原生密码管理器）。SecureVault 继承了其经过验证的：


- 三层密钥体系
- Argon2id 自适应密钥派生
- XChaCha20-Poly1305 字段级加密
- 安全填充和内存擦除机制
- AutofillService 设计模式

## License

TBD
