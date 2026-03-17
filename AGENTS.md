# AGENTS.md - SecureVault 开发指南

> 为 AI 编程助手提供的开发规范与命令参考

---

## 一、项目概览

**SecureVault** 是一款离线优先的全平台密码管理器，采用 Kotlin Multiplatform (KMP) + Compose Multiplatform 技术栈。

### 技术栈
| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.3.20 |
| UI | Compose Multiplatform | 1.10.x |
| 数据库 | SQLDelight | 2.2.x |
| DI | Koin | 4.2.x |
| 异步 | Coroutines + Flow | 1.10.x |
| 加密 | libsodium (ionspin) + argon2kt | 0.9.2 / 1.6.0 |
| 构建 | Gradle | 9.3.x |
| AGP | Android Gradle Plugin | 9.0.x |

### 模块结构
```
SecureVault/
├── shared/
│   ├── common/     # 跨平台共享逻辑（加密、安全、存储、模型、用例）
│   ├── android/    # Android expect/actual 实现
│   ├── ios/        # iOS expect/actual 实现
│   └── desktop/    # Desktop expect/actual 实现
├── composeApp/     # Compose Multiplatform UI
├── androidApp/     # Android 应用入口 + AutofillService
├── iosApp/         # iOS 应用入口 + CredentialProvider
└── desktopApp/     # Desktop 应用入口
```

---

## 二、构建命令

### 基础命令
```bash
# 清洁构建
./gradlew clean build

# 仅编译（快速检查）
./gradlew assemble

# Android Debug 构建
./gradlew androidApp:assembleDebug

# Android Release 构建
./gradlew androidApp:assembleRelease

# Desktop 构建
./gradlew desktopApp:assemble

# iOS 构建（需要 macOS）
./gradlew iosApp:assemble
```

### 编译检查
```bash
# 编译所有目标
./gradlew compileKotlin

# Android 编译检查
./gradlew androidApp:compileDebugKotlin

# Desktop 编译检查
./gradlew desktopApp:compileKotlinJvm
```

---

## 三、测试命令

### 运行测试
```bash
# 运行所有测试
./gradlew allTests

# 仅运行 JVM 测试（commonTest + desktop）
./gradlew jvmTest

# Android 单元测试
./gradlew androidApp:testDebugUnitTest

# Desktop 测试
./gradlew desktopApp:jvmTest

# 运行单个测试类（示例）
./gradlew desktopApp:jvmTest --tests "com.securevault.crypto.AesGcmCipherTest"

# 运行单个测试方法（示例）
./gradlew desktopApp:jvmTest --tests "com.securevault.crypto.AesGcmCipherTest.encrypt_decrypt_roundtrip"

# 带日志输出运行测试
./gradlew allTests --info
```

### 测试覆盖率
```bash
# 生成覆盖率报告（需要配置 JaCoCo）
./gradlew koverHtmlReport

# 覆盖率 XML 报告（CI 使用）
./gradlew koverXmlReport
```

### 测试规范
- **单元测试**: 放在 `src/commonTest/kotlin/` 目录
- **集成测试**: 放在各平台 `src/androidTest/kotlin/` 或 `src/desktopTest/kotlin/`
- **测试框架**: kotlin.test + kotlinx-coroutines-test + Turbine (Flow 测试)
- **命名约定**: `<被测试类>Test.kt`，方法名使用下划线分隔描述行为

---

## 四、代码检查

### 格式化
```bash
# 格式化所有代码
./gradlew format

# 格式化 Kotlin 代码
./gradlew ktlintFormat

# 检查代码格式
./gradlew ktlintCheck
```

### 静态分析
```bash
# 运行所有检查
./gradlew check

# Detekt 静态分析
./gradlew detekt

# Android Lint
./gradlew androidApp:lint

# 编译时错误检查
./gradlew buildHealth
```

---

## 五、代码风格规范

### 导入规范
```kotlin
// 顺序：标准库 → KotlinX → 第三方 → 项目内
import kotlin.text.*
import kotlinx.coroutines.*
import io.ktor.client.*
import com.securevault.crypto.*
```
- 使用 `*` 导入仅限于同包下多个类
- 禁止使用模糊导入（如 `import java.util.*`）

### 命名规范
| 类型 | 规范 | 示例 |
|------|------|------|
| 类/接口 | PascalCase | `PasswordRepository`, `UseCase` |
| 函数/变量 | camelCase | `addPassword()`, `encryptedData` |
| 常量/枚举 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT`, `EncryptionVersion` |
| 私有常量 | camelCase | `maxRetryCount` |
| 测试类 | `<目标>Test` | `AesGcmCipherTest` |
| 用例类 | `<动作><对象>UseCase` | `AddPasswordUseCase` |
| ViewModel | `<功能>ViewModel` | `VaultViewModel` |
| Repository 实现 | `<接口名>Impl` | `PasswordRepositoryImpl` |

### 格式化规则
- 缩进：4 个空格（禁止 Tab）
- 行宽：120 字符
- 大括号：K&R 风格（左括号不换行）
- 空行：函数间 1 行，类间 2 行
- 链式调用：每行一个调用，`.`前置

### 类型与泛型
- 泛型命名：`T` (通用), `E` (实体), `R` (仓库), `U` (用例)
- 类型别名：复杂泛型使用 `typealias`
- 可空性：明确声明 `?`，避免 `!!` 除非有前置检查

### 错误处理
```kotlin
// 使用 Result 类型封装可能失败的操作
sealed class VaultResult<out T> {
    data class Success<T>(val data: T) : VaultResult<T>()
    data class Error(val error: VaultError) : VaultResult<Nothing>()
}

// 使用 sealed class 定义错误类型
sealed class VaultError {
    object SessionLocked : VaultError()
    object InvalidPassword : VaultError()
    data class CryptoError(val message: String) : VaultError()
}

// 协程中使用 try-catch 处理异常
fun runCatching(block: suspend () -> T): Result<T>
```

### 协程规范
- 作用域：使用 `CoroutineScope` 注入，避免 `GlobalScope`
- 调度器：使用 `Dispatchers.Default` (CPU), `Dispatchers.IO` (IO)
- 结构化并发：父子协程自动取消
- Flow 测试：使用 Turbine 库验证 Flow 发射

### 敏感数据处理
```kotlin
// 使用 CharArray 而非 String 存储密码
fun derivePassword(password: CharArray): ByteArray

// 使用后用 0 覆写内存
fun ByteArray.zeroOut() {
    for (i in indices) this[i] = 0
}

// 使用 try-finally 确保清理
val password = getPassword()
try {
    // 使用密码
} finally {
    password.zeroOut()
}
```

---

## 六、安全编码规范

### 加密实现
- 禁止硬编码密钥或盐值
- 所有加密使用 `CryptoResult` 封装错误
- AES-GCM 必须使用随机 IV（12 字节）
- Argon2 参数根据设备性能自适应

### 内存安全
- 敏感数据使用后立即覆写
- 避免在日志中打印敏感信息
- 禁用调试模式下的日志输出

### 数据存储
- 数据库文件存储在应用私有目录
- 导出的加密文件需要用户确认
- 禁止备份敏感数据到云存储

---

## 七、架构模式

### 分层架构
```
UI (composeApp) → ViewModel → UseCase → Repository → DataSource
```

### UseCase 规范
- 每个用例一个类，单一职责
- 输入参数使用 data class 封装
- 返回 `Flow<VaultResult<T>>` 支持响应式

### Repository 模式
- 接口定义在 `common`，实现在各平台
- 使用 `expect/actual` 处理平台差异
- 数据源切换对上层透明

---

## 八、Git 提交规范

### 提交格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型
| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（不影响功能） |
| `style` | 代码格式（不影响功能） |
| `test` | 测试相关 |
| `docs` | 文档更新 |
| `chore` | 构建/配置/工具 |
| `perf` | 性能优化 |
| `security` | 安全相关 |

### 示例
```
feat(crypto): 添加 AES-256-GCM 加密支持

实现 AesGcmCipher 类，支持 encrypt/decrypt 操作
使用 SecureRandom 生成 IV，确保每次加密结果不同

Closes #123
```

---

## 九、Cursor/Copilot 规则

当前项目无 `.cursor/rules/`、`.cursorrules` 或 `.github/copilot-instructions.md` 配置文件。

---

## 十、文档索引

| 文档 | 路径 |
|------|------|
| 技术选型 | `docs/TECH-STACK.md` |
| 系统架构 | `docs/design/ARCHITECTURE.md` |
| UI 设计规范 | `docs/design/UI-DESIGN-SPEC.md` |
| 测试策略 | `docs/design/TESTING-AND-PERFORMANCE.md` |
| 加密算法 | `docs/reference/CRYPTO-ALGORITHMS.md` |
| 安全架构 | `docs/reference/SECURITY-ARCHITECTURE.md` |
| 自动填充 | `docs/reference/AUTOFILL-DESIGN.md` |
| 开发计划 | `docs/DEVELOPMENT-PLAN.md` |

---

## 十一、常见问题

### 运行单测失败
1. 检查 `gradle.properties` 配置
2. 确保测试类路径正确
3. 查看 `build/reports/tests/` 详细报告

### 跨平台编译问题
1. 确保 Kotlin 版本与各插件兼容
2. 检查 `libs.versions.toml` 版本一致性
3. 清理 Gradle 缓存：`./gradlew --rerun-tasks`

### 加密测试不一致
1. 确认使用相同盐值和参数
2. 验证跨平台 libsodium 版本
3. 检查字节序（Endianness）差异
