# 自动填充服务设计参考

> 从 SafeVault 提取的 AutofillService 设计经验，覆盖 Android、iOS 和 Desktop 三个平台。

---

## 一、跨平台自动填充架构

| 平台 | 技术 | 能力 |
|------|------|------|
| **Android** | AutofillService API (API 26+) | 系统级自动填充 + 保存检测 |
| **iOS** | ASCredentialProviderExtension | 系统级凭证提供 |
| **Desktop** | 全局快捷键 + 剪贴板 | 手动填充 |

### 共享逻辑

以下逻辑可以在 `shared/common` 中共享：

```
┌─────────────────────────────────────┐
│        共享层 (commonMain)           │
│                                     │
│  CredentialMatcher                  │
│  ├── matchByDomain(domain) → List   │
│  ├── matchByPackage(pkg) → List     │
│  └── normalizeDomain(url) → String  │
│                                     │
│  CredentialDecryptor                │
│  ├── decryptForFill(entry) → Cred   │
│  └── requiresAuth() → Boolean      │
│                                     │
│  DuplicateDetector                  │
│  ├── findExisting(domain, user)     │
│  └── shouldUpdate(old, new)         │
└─────────────────────────────────────┘
         ↑           ↑          ↑
    Android        iOS       Desktop
    actual         actual     actual
```

---

## 二、Android AutofillService 设计

### 2.1 处理管线

从 SafeVault 验证的四阶段管线：

```
FillRequest / SaveRequest
    ↓
┌──────────────────────┐
│ Stage 1: 解析 (Parse)│  AutofillParser
│ - 遍历 ViewNode 树   │
│ - 识别字段类型        │
│ - 提取元数据          │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Stage 2: 安全检查     │  SecurityFilter
│ - 排除自身应用        │
│ - 排除其他密码管理器  │
│ - 排除系统设置        │
│ - 排除不安全输入      │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Stage 3: 匹配 (Match)│  CredentialMatcher
│ - 域名精确匹配       │
│ - 子域名匹配         │
│ - 包名匹配           │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Stage 4: 构建 (Build)│  FillResponseBuilder
│ - 构建 Dataset 列表  │
│ - 添加 SaveInfo      │
│ - 锁定状态处理        │
└──────────────────────┘
```

### 2.2 字段类型识别

SafeVault 验证有效的字段检测优先级（从高到低）：

```kotlin
enum class FieldType {
    USERNAME, PASSWORD, EMAIL, PHONE, UNKNOWN
}

object FieldTypeDetector {

    // 检测优先级
    fun identify(node: ViewNode): FieldType {
        // 1. 排除无关字段（验证码、搜索框、OTP）
        if (isExcludedField(node)) return FieldType.UNKNOWN

        // 2. 系统 autofillHints（最可靠）
        node.autofillHints?.forEach { hint ->
            classifyByHint(hint)?.let { return it }
        }

        // 3. inputType 标志位
        classifyByInputType(node.inputType)?.let { return it }

        // 4. hint 文本
        classifyByText(node.hint?.toString())?.let { return it }

        // 5. View ID
        classifyByText(node.idEntry)?.let { return it }

        // 6. HTML 属性（WebView 场景）
        classifyByHtmlAttributes(node)?.let { return it }

        return FieldType.UNKNOWN
    }

    // 排除列表
    private val EXCLUDE_PATTERNS = listOf(
        "captcha", "验证码", "otp", "one.?time",
        "search", "搜索", "query",
        "sms", "短信", "code"
    )

    // 用户名关键词
    private val USERNAME_PATTERNS = listOf(
        "username", "user.?name", "account", "login",
        "用户名", "账号", "登录名"
    )

    // 密码关键词
    private val PASSWORD_PATTERNS = listOf(
        "password", "passwd", "pass.?word", "pin",
        "密码", "口令"
    )

    // 邮箱关键词
    private val EMAIL_PATTERNS = listOf(
        "email", "e.?mail", "邮箱", "邮件"
    )
}
```

### 2.3 域名匹配算法

```kotlin
object DomainMatcher {

    fun matchCredentials(
        domain: String?,
        packageName: String?,
        repository: PasswordRepository
    ): List<PasswordEntry> {
        // 优先域名匹配（Web 场景）
        if (!domain.isNullOrBlank()) {
            val results = matchByDomain(domain, repository)
            if (results.isNotEmpty()) return results
        }

        // 回退到包名匹配（App 场景）
        if (!packageName.isNullOrBlank()) {
            return matchByPackageName(packageName, repository)
        }

        return emptyList()
    }

    private fun matchByDomain(
        domain: String,
        repository: PasswordRepository
    ): List<PasswordEntry> {
        val normalized = normalizeDomain(domain)
        val rootDomain = extractRootDomain(normalized)

        return repository.searchByUrl("%$rootDomain%")
            .filter { entry ->
                val entryDomain = extractDomainFromUrl(entry.url ?: "")
                entryDomain == normalized || isSubdomainMatch(entryDomain, normalized)
            }
    }

    private fun matchByPackageName(
        packageName: String,
        repository: PasswordRepository
    ): List<PasswordEntry> {
        return repository.searchByUrl("%$packageName%")
    }

    fun normalizeDomain(domain: String): String {
        return domain.lowercase()
            .removePrefix("www.")
            .trim()
    }

    fun extractRootDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) {
            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        } else domain
    }

    fun isSubdomainMatch(a: String, b: String): Boolean {
        return extractRootDomain(a) == extractRootDomain(b)
    }
}
```

### 2.4 填充响应构建

```kotlin
class FillResponseBuilder {

    fun buildResponse(
        credentials: List<DecryptedCredential>,
        usernameIds: List<AutofillId>,
        passwordIds: List<AutofillId>,
        isLocked: Boolean,
        authIntentSender: IntentSender?
    ): FillResponse {
        val builder = FillResponse.Builder()

        if (isLocked) {
            // 锁定状态：只显示"打开保险库"选项
            authIntentSender?.let {
                builder.addDataset(createVaultDataset(usernameIds, passwordIds, it))
            }
        } else {
            // 每个匹配的凭证创建一个 Dataset
            for (credential in credentials) {
                builder.addDataset(
                    createCredentialDataset(credential, usernameIds, passwordIds)
                )
            }

            // 末尾追加"打开保险库"选项
            authIntentSender?.let {
                builder.addDataset(createVaultDataset(usernameIds, passwordIds, it))
            }
        }

        // 添加 SaveInfo 以检测新密码
        builder.setSaveInfo(createSaveInfo(usernameIds, passwordIds))

        return builder.build()
    }

    // 用户名显示脱敏
    fun maskUsername(username: String): String {
        if (username.length <= 4) return "****"
        return "${username.take(2)}${"*".repeat(username.length - 4)}${username.takeLast(2)}"
    }
}
```

### 2.5 保存检测流程

SafeVault 验证的保存检测和去重逻辑：

```kotlin
// AutofillService.onSaveRequest() 触发时:
class SaveDetector(
    private val repository: PasswordRepository,
    private val duplicateDetector: DuplicateDetector
) {

    data class SaveAction(
        val type: SaveActionType,
        val username: String,
        val password: String,
        val domain: String?,
        val packageName: String?,
        val existingEntryId: Long? = null
    )

    enum class SaveActionType {
        SAVE_NEW,       // 新凭证
        UPDATE_EXISTING // 已存在，密码已变
    }

    fun detectSaveAction(
        username: String,
        password: String,
        domain: String?,
        packageName: String?
    ): SaveAction? {
        if (password.isBlank()) return null

        val existing = duplicateDetector.findExisting(domain, packageName, username)

        return if (existing != null) {
            // 已有同域名同用户名的记录 → 提示更新
            SaveAction(SaveActionType.UPDATE_EXISTING, username, password,
                domain, packageName, existing.id)
        } else {
            // 新凭证 → 提示保存
            SaveAction(SaveActionType.SAVE_NEW, username, password,
                domain, packageName)
        }
    }
}
```

### 2.6 黑名单

从 SafeVault 提取的需要屏蔽的应用列表：

```kotlin
object AutofillBlocklist {
    val BLOCKED_PACKAGES = setOf(
        // 其他密码管理器
        "com.lastpass.lpandroid",
        "com.x8bit.bitwarden",
        "com.agilebits.onepassword",
        "com.dashlane",
        "com.callpod.android_apps.keeper",
        "io.enpass.app",

        // 系统应用
        "com.android.settings",
        "com.android.systemui",

        // 开发工具
        "com.android.shell",
        "com.android.adb"
    )

    fun isBlocked(packageName: String): Boolean {
        return packageName in BLOCKED_PACKAGES
    }
}
```

---

## 三、iOS CredentialProvider 设计

### 3.1 架构

iOS 使用 `ASCredentialProviderExtension` 提供系统级凭证填充：

```
┌─────────────────────────────────────┐
│  iOS Settings → Passwords →         │
│  AutoFill Passwords → SecureVault   │
└──────────────────┬──────────────────┘
                   ↓
┌──────────────────────────────────────┐
│  CredentialProviderExtension         │
│                                      │
│  prepareCredentialList()             │
│  ├── 查询匹配的凭证                  │
│  └── 显示凭证列表                    │
│                                      │
│  provideCredentialWithoutUserInteraction() │
│  ├── 检查 SessionManager.isUnlocked │
│  └── 如果已解锁，直接返回凭证        │
│                                      │
│  prepareInterfaceToProvideCredential() │
│  ├── 显示解锁 UI（主密码/Face ID）   │
│  └── 解锁后返回凭证                  │
└──────────────────────────────────────┘
```

### 3.2 关键实现点

```kotlin
// iosMain - KMP 桥接
actual class AutofillBridge {
    // iOS Extension 通过 KMP 调用共享逻辑
    actual fun matchCredentials(serviceIdentifier: String): List<CredentialInfo> {
        val domain = extractDomain(serviceIdentifier)
        return credentialMatcher.matchByDomain(domain)
            .map { entry -> CredentialInfo(entry.id, entry.title, entry.username) }
    }

    actual fun provideCredential(id: Long): CredentialData? {
        if (!sessionManager.isUnlocked) return null
        return sessionManager.withUnlockedSession { dataKey ->
            val entry = repository.getById(id) ?: return@withUnlockedSession null
            val password = aesGcmCipher.decryptField(entry.encryptedPassword, dataKey)
            val username = aesGcmCipher.decryptField(entry.encryptedUsername, dataKey)
            CredentialData(username, password)
        }
    }
}
```

### 3.3 iOS 与 Android 差异

| 特性 | Android AutofillService | iOS CredentialProvider |
|------|------------------------|----------------------|
| 保存检测 | `onSaveRequest()` 系统回调 | 无自动检测（需要 App Extension 或 Notification） |
| 表单解析 | `AssistStructure` 树 | `ASCredentialServiceIdentifier` (域名/包名) |
| 认证 | 自定义 Activity | Extension 内自定义 UI |
| 后台保活 | 前台服务（可选） | Extension 生命周期 |
| 安全模式 | `FLAG_SECURE` | 系统级保护 |

---

## 四、Desktop 自动填充策略

Desktop 平台没有系统级自动填充 API，采用以下替代方案：

### 4.1 快速复制模式

```kotlin
class DesktopAutoFillHelper(
    private val sessionManager: SessionManager,
    private val repository: PasswordRepository,
    private val clipboard: SecureClipboard
) {
    // 全局快捷键触发（Ctrl+Shift+P 或自定义）
    fun onQuickFillTriggered() {
        if (!sessionManager.isUnlocked) {
            showUnlockDialog()
            return
        }

        // 尝试识别当前活动窗口/浏览器 URL
        val activeWindow = getActiveWindowInfo()
        val matchedEntries = matchByWindowTitle(activeWindow)

        if (matchedEntries.size == 1) {
            // 只有一个匹配 → 直接复制密码
            autofillEntry(matchedEntries.first())
        } else {
            // 多个匹配或无匹配 → 显示选择弹窗
            showCredentialPicker(matchedEntries)
        }
    }

    private fun autofillEntry(entry: PasswordEntry) {
        // 1. 复制用户名到剪贴板
        clipboard.copy(entry.username, "Username")
        // 2. 等待用户粘贴
        // 3. 复制密码到剪贴板（可配置延迟或手动触发）
        clipboard.scheduleAutoClear(30_000)
    }
}
```

### 4.2 浏览器扩展（可选，Phase 6+）

未来可以开发浏览器扩展，通过 Native Messaging 与桌面应用通信：

```
浏览器扩展 → Native Messaging → SecureVault Desktop → 加密凭证
```

---

## 五、安全考虑

### 5.1 自动填充安全清单

| 检查项 | 实现 |
|-------|------|
| 不自动填充自身应用 | 检查 packageName |
| 不自动填充其他密码管理器 | 黑名单机制 |
| 锁定状态不泄露数据 | 仅显示"打开保险库"选项 |
| 密码脱敏显示 | `maskUsername()` |
| 保存流程需要认证 | 先解锁再保存 |
| 安全模式密码不显示明文 | 仅通过剪贴板/填充使用 |
| 剪贴板 30 秒自动清除 | `SecureClipboard` |
| UI 截图防护 | `FLAG_SECURE` (Android) |
| 会话超时检查 | 填充前检查 `SessionManager` |
