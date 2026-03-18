# AGENTS.md - SecureVault 工作区指令

> 面向 AI 编程助手：仅保留“全局必需、可执行、可验证”的项目约定。

## 1) 项目概览

SecureVault 是离线优先的 Kotlin Multiplatform 密码管理器。

- 语言与构建：Kotlin 2.3.20、Gradle 9.3.x、AGP 9.0.x
- UI：Compose Multiplatform 1.10.x
- 数据：SQLDelight 2.2.1（`generateAsync = true`）
- 依赖：Koin、Coroutines、libsodium、argon2kt

## 2) 模块边界（必须遵守）

- `shared/common`：跨平台核心业务与安全逻辑（加密、会话、仓储、模型）
- `shared/android` / `shared/ios` / `shared/desktop`：平台实现与桥接
- `composeApp`：共享 UI、ViewModel、导航、DI
- `androidApp` / `desktopApp`：应用入口与平台壳
- `iosApp` 目录存在，但当前 `settings.gradle.kts` 未 `include(":iosApp")`，不要在本工作区使用 `iosApp:*` 任务

## 3) 构建与测试命令（已验证）

优先使用以下命令（按常用程度排序）：

```bash
./gradlew clean build
./gradlew assemble
./gradlew compileKotlin

./gradlew androidApp:assembleDebug
./gradlew desktopApp:run

./gradlew shared:common:allTests
./gradlew shared:common:desktopTest
./gradlew desktopApp:jvmTest
./gradlew androidApp:testDebugUnitTest

./gradlew check
./gradlew detekt
```

如需确认任务名，请先运行：

```bash
./gradlew :shared:common:tasks --all
./gradlew tasks --all
```

## 4) 代码与架构约定（高优先级）

- 分层方向固定：`UI(Compose) -> ViewModel -> UseCase/Repository -> DataSource`
- 优先修根因：避免只做表层补丁
- 与现有代码风格保持一致：4 空格缩进、避免无关重构
- `expect/actual` 必须在同一 KMP 模块对应 source set（如 `shared/common/src/commonMain` 与 `shared/common/src/androidMain`）
- 协程使用结构化并发，避免 `GlobalScope`
- 错误处理优先使用 `Result` / sealed 错误类型

## 5) 安全与加密事实（以实现为准）

- 当前实现使用 `XChaCha20-Poly1305`（libsodium `SecretBox`），不是 AES-GCM
- `EncryptedData` 与 `SecretBox` 互转时需要处理 `tag + ciphertext` 的拆分与拼接
- 敏感数据使用后需要清理（如 `ByteArray`/`CharArray` 覆写）
- 禁止日志输出密钥、明文密码、恢复短语等敏感信息

## 6) 常见易错点

- 将 `actual` 放到 sibling 模块会导致 `expect` 校验失败
- 使用不存在的根任务（如 `ktlintCheck`、`format`、`buildHealth`）会造成误导，请先查任务列表
- 直接依据旧文档实现前先对照源码，尤其是加密算法与模块结构

## 7) 参考文档

- 架构：`docs/design/ARCHITECTURE.md`
- 测试与性能：`docs/design/TESTING-AND-PERFORMANCE.md`
- 安全架构：`docs/reference/SECURITY-ARCHITECTURE.md`
- 加密算法：`docs/reference/CRYPTO-ALGORITHMS.md`
- 开发计划：`docs/DEVELOPMENT-PLAN.md`

## 8) Agent 工作建议

- 先验证再修改：命令、模块、算法都以工作区文件与任务清单为准
- 改动尽量小而精确，必要时补充对应文档
- 如果需求跨多个子域（UI/crypto/build），先分步骤执行并逐步验证
