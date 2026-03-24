> 中文版说明：本文件为机器翻译生成的中文版本，保留原始需求编号、测试ID与文档结构，建议与英文原文对照使用。

# SecureVault — 软件需求规范 (SRS)

> 从实现的 MVP 进行逆向工程。
> 修订版：1.1 |日期：2026-03-25
> 角色：安全架构师·高级产品经理·系统测试主管

**文档范围（2026-03-25）：** **当前发布与验证基线**为 **Android** 与 **Windows 上的 Desktop（JVM）**。**暂缓：** iOS 与 Apple 生态能力；**macOS / Linux** 桌面独立发行与平台专项。详见 [PLATFORM-SCOPE.md](PLATFORM-SCOPE.md)。文中涉及 iOS 或多桌面 OS 的表格仍保留用于追溯，**非当前周期验收项**，除非重新打开排期。

---

## 目录

- [1.系统概述](#1-系统概述)
- [2.系统状态模型](#2-系统状态模型)
- [3.核心业务流程](#3-core-business-flows)
- [4.功能需求](#4-功能需求)
- [5.安全要求](#5-安全要求)
- [6.非功能性需求](#6-非功能性需求)
- [7.验收标准](#7-验收标准)
- [8.测试覆盖率摘要](#8-测试覆盖率-摘要)

---

# 一、系统概述

## 1.1 系统目标

SecureVault 是一款使用 Kotlin Multiplatform 构建的离线优先密码管理器。**当前工程目标**为 **Android** 与 **Windows 上的 Desktop（JVM）**（开发、验证与发布）。**iOS** 与 **macOS/Linux 桌面**相关工作 **暂缓**（仓库中可仍存存根或预留代码）。系统将用户凭据存储在加密的本地数据库中。任何时候都不会发生网络通信。

**主要目标：**

| # | 目标 |
|---|------|
| G-1 | 允许用户存储、检索、组织和删除凭证条目 |
| G-2 | 使用经过身份验证的加密对所有静态敏感字段进行加密 |
| G-3 | 保护用户选择的主密码背后的加密密钥层次结构 |
| G-4 | 提供生物识别解锁作为主密码的替代方案 |
| G-5 | 提供安全模式，防止明文密码显示，并需要重新验证才能进行破坏性操作 |
| G-6 | 在可配置的不活动或后台持续时间后自动锁定保管库 |
| G-7 | 固定超时后自动清除剪贴板内容 |
| G-8 | 完全离线操作，零网络依赖 |

## 1.2 安全目标

| ID | 客观的 | 优先事项 |
|----|-----------|----------|
| SO-1 | 所有凭证字段（标题、用户名、密码、URL、注释、标签）在持久化之前均单独加密 | 批判的 |
| SO-2 | DataKey 永远不会以明文形式存在于磁盘上；仅当会话解锁时它才存在于内存中 | 批判的 |
| SO-3 | 主密码永远不会被存储；仅暂时使用派生密钥 | 批判的 |
| SO-4 | 临时加密材料（PasswordKey、SecureModeKey、密码 CharArrays）在使用后立即从内存中擦除 | 批判的 |
| SO-5 | 包含凭据的剪贴板内容将在 30 秒后自动清除 | 高的 |
| SO-6 | Android 上默认启用屏幕截图保护 | 高的 |
| SO-7 | 在任何情况下，安全模式密码都不会显示在 UI 中 | 高的 |
| SO-8 | 会话在可配置的不活动/后台超时后自动锁定 | 高的 |

## 1.3 目标平台

| 平台 | 地位 | 生物识别 | 剪贴板 | 截图保护 | 设备密钥存储 |
|----------|--------|-----------|-----------|----------------------|-------------------|
| 安卓 | **活跃**（发布基线） | 生物识别提示 (BIOMETRIC_STRONG) | 剪贴板管理器 | 标志_安全 | Android 密钥库（AES-256-GCM，硬件支持） |
| 桌面（JVM），以 Windows 为主 | **活跃**（发布基线；主要在 Windows 验证） | 不可用（存根返回 NotAvailable） | java.awt.Toolkit剪贴板 | 不可用（无操作） | 具有 XOR 混淆的 Java 首选项 |
| 桌面 macOS/Linux 发行 | **暂缓**（非当前发布目标） | — | — | — | — |
| iOS系统 | **暂缓** / 仅存根（不包含在构建中） | 不可用（存根） | 不可用（无操作存根） | 不可用（无操作） | 未实施 |

---

# 2. 系统状态模型

## 2.1 状态

系统在一组有限的状态下运行。存在两个状态层：**会话层**和**UI 覆盖层**。

### 2.1.1 会话状态

| 状态 | 定义 | 内存中的数据密钥 |
|-------|-----------|-------------------|
| **未设置** | 尚未创建保管库。 `vault_config` 中不存在加密的 DataKey。首次启动时的初始状态。 | 不 |
| **锁定** | 保险库存在（配置保留），但 DataKey 不在内存中。所有加密操作均不可用。 | 不 |
| **解锁** | DataKey 位于“SessionManager”中的“SensitiveData<ByteArray>”包装器中。所有保管库操作均可用。 | 是的 |

### 2.1.2 UI 叠加状态

这些与会话状态正交，并且仅在会话解锁时才有意义。

| 覆盖 | 定义 |
|---------|-----------|
| **安全模式关闭** | 规范操作。密码可以切换可见。编辑/删除是免费的。 |
| **安全模式开启** | 密码在 UI 中永久隐藏。编辑和删除需要重新身份验证（生物识别或主密码）。全局启用/禁用切换在会话之间保持不变。 |

### 2.1.3 导航界面

| 表面 | 活跃时 |
|---------|-------------|
| **授权** | 会话未设置或已锁定。显示入职、注册或登录屏幕。 |
| **主要的** | 会话已解锁。显示 Vault、生成器和设置选项卡。 |

系统强制执行：每当“KeyManagerState == Locked”且当前界面为“Main”时，导航将强制转到“Auth/Login”。

## 2.2 状态转换

```
┌─────────────┐
│  NOT_SETUP  │
└──────┬──────┘
       │ setupVault(password) succeeds
       ▼
┌─────────────┐   lock() / background timeout /   ┌──────────┐
│  UNLOCKED   │──────────────────────────────────► │  LOCKED  │
│             │◄──────────────────────────────────  │          │
└─────────────┘   unlockWithPassword(password)     └──────────┘
       │           or unlockWithBiometric()               │
       │                                                  │
       │ clear() (vault wipe)                             │
       ▼                                                  │
┌─────────────┐                                           │
│  NOT_SETUP  │◄──────────────────────────────────────────┘
└─────────────┘   clear() (vault wipe)
```

### 2.2.1 转换表

| # | 从 | 到 | 扳机 | 前提条件 | 行动 |
|---|------|----|---------|---------------|---------|
| T-1 | 不设置 | 解锁 | `setupVault(密码)` 成功 | 不存在保管库配置 | 生成盐；通过Argon2id导出PasswordKey；生成随机DataKey；用PasswordKey加密DataKey；将加密的 DataKey + Argon2 参数存储在 `vault_config` 中；将 DataKey 存储在 PlatformKeyStore 中；将DataKey加载到SessionManager中；擦除PasswordKey和本地DataKey副本；擦除密码 CharArray |
| T-2 | 锁定 | 解锁 | `unlockWithPassword(密码)` 成功 | Vault 配置存在且 `isSetup == true` | 从 `vault_config` 读取 salt + Argon2 配置；导出密码密钥；解密数据密钥；更新PlatformKeyStore；将DataKey加载到SessionManager中；擦除PasswordKey和本地DataKey副本；擦除密码 CharArray |
| T-3 | 锁定 | 解锁 | `unlockWithBiometric()` 成功 | Vault 配置存在； DeviceKey 存储在 PlatformKeyStore 中 | 从PlatformKeyStore中检索DataKey；将DataKey加载到SessionManager中；擦除本地 DataKey 副本 |
| T-4 | 解锁 | 锁定 | `lock()`（手动） | 会话已解锁 | 通过 `SensitiveData.close()` 擦除 DataKey；将会话状态设置为锁定；强制导航至身份验证/登录 |
| T-5 | 解锁 | 锁定 | `onAppBackground()` 当超时 == -1（立即） | 会话已解锁；锁定超时配置为立即 | 与T-4相同 |
| T-6 | 解锁 | 锁定 | 当后台持续时间 >= 超时时`onAppForeground()` | 会话已解锁；超时 > 0 | 与T-4相同 |
| T-7 | 解锁 | 锁定 | 当不活动>=超时时`checkAutoLock()` | 会话已解锁；超时 > 0 | 与T-4相同 |
| T-8 | 解锁 | 不设置 | `clear()`（库擦除） | — | 锁定会话；从PlatformKeyStore中删除DeviceKey；空库配置 |
| T-9 | 锁定 | 不设置 | `clear()`（库擦除） | — | 与T-8相同 |

### 2.2.2 安全模式转换

| # | 从 | 到 | 扳机 | 需要身份验证 |
|---|------|----|---------|---------------|
| SM-1 | 安全模式关闭 | 安全模式开启 | 用户在设置中打开 | 不 |
| SM-2 | 安全模式开启 | 安全模式关闭 | 用户在设置中关闭 | 是：生物识别，后备至主密码 |

## 2.3 每个州允许的操作

| 手术 | 不设置 | 锁定 | 解锁 | 解锁+安全模式 |
|-----------|:---------:|:------:|:--------:|:----------------------:|
| 完成入职 | 是的 | — | — | — |
| 创建保管库（注册） | 是的 | — | — | — |
| 输入主密码 | — | 是的 | — | — |
| 生物识别解锁 | — | 是（如果已注册） | — | — |
| 列出条目 | — | — | 是的 | 是的 |
| 查看条目详细信息 | — | — | 是的 | 是（密码始终被屏蔽） |
| 切换密码可见性 | — | — | 是的 | **禁止** |
| 将密码复制到剪贴板 | — | — | 是的 | 是（通过安全的“usePassword”路径） |
| 将用户名复制到剪贴板 | — | — | 是的 | 是的 |
| 添加条目 | — | — | 是的 | 是的 |
| 编辑条目 | — | — | 是的 | 需要重新授权 |
| 删除条目 | — | — | 是（需确认） | 需要重新授权+确认 |
| 搜索条目 | — | — | 是的 | 是的 |
| 生成密码 | — | — | 是的 | 是的 |
| 更改主密码 | — | — | 是的 | 是的 |
| 更改设置 | — | — | 是的 | 是的 |
| 锁金库 | — | — | 是的 | 是的 |
| 启用安全模式 | — | — | 是的 | — |
| 禁用安全模式 | — | — | — | 需要重新授权 |

---

# 3. 核心业务流程

## 3.1 添加密码

### 正常流量

| 步 | 演员 | 行动 | 系统响应 |
|------|-------|--------|-----------------|
| 1 | 用户 | 点击 Vault 屏幕上的“+”FAB | 系统导航到带有空表单的 AddEdit 屏幕 |
| 2 | 用户 | 填写必填字段：标题、用户名、密码。可选填写：URL、注释、类别、标签、收藏夹切换、安全模式切换 | 当所有三个必填字段均非空白时，系统启用“保存”按钮 |
| 3 | 用户 | 点击“保存” | 系统使用“id=null”、“createdAt=now”、“updatedAt=now”构造“PasswordEntry” |
| 4 | 系统 | 从 SessionManager 检索 DataKey | 返回 DataKey 的副本；刷新 `lastActivityTime` |
| 5 | 系统 | 单独加密每个字段 | 标题、用户名、url、注释、标签：通过 XChaCha20-Poly1305 使用 DataKey 加密。密码：使用 DataKey（标准）或 SecureModeKey 加密（如果“securityMode=true”） |
| 6 | 系统 | 将每个“EncryptedData”序列化为 JSON 存储格式 | 格式：`{"version":"v2","iv":"<base64>","ciphertext":"<base64>","tag":"<base64>"}` |
| 7 | 系统 | 将行插入到“password_entries”表中 | 明文列：`category`、`is_favorite`、`security_mode`、`created_at`、`updated_at`。所有内容列：加密的 JSON 字符串 |
| 8 | 系统 | 返回成功 | 导航至 Vault 屏幕；刷新条目列表；显示“已保存”信息栏 |

### 错误流

| 健康）状况 | 系统行为 |
|-----------|----------------|
| E-ADD-1：点击“保存”时会话被锁定 | `getDataKey()` 返回 null。错误消息：“保险库已锁定”。保存已中止。 |
| E-ADD-2：加密失败（例如，libsodium 未初始化） | 由“runCatching”捕获的异常。异常时显示的错误消息。条目未保留。 |
| E-ADD-3：数据库插入失败 | 捕获异常。错误消息：“保存失败”或异常消息。条目未保留。 |
| E-ADD-4：当“securityMode=true”时 SecurityModeManager 不可用 | 回退到直接使用 DataKey 加密密码（标准加密路径）。没有出现错误。 |

### 验证规则

| 场地 | 规则 |
|-------|------|
| 标题 | 必需的。必须非空白。没有强制执行最大长度。 |
| 用户名 | 必需的。必须非空白。没有强制执行最大长度。 |
| 密码 | 必需的。必须非空白。不强制执行最低复杂度。 |
| 网址 | 选修的。没有格式验证。 |
| 笔记 | 选修的。没有长度限制。 |
| 类别 | 如果空白则默认为“默认”。 |
| 标签 | 编辑时继承自现有条目；创建时为空列表。 |

## 3.2 找回密码

### 正常流量（标准输入）

| 步 | 演员 | 行动 | 系统响应 |
|------|-------|--------|-----------------|
| 1 | 用户 | 在保管库屏幕上点击密码卡 | 系统导航至“密码详细信息”屏幕 |
| 2 | 系统 | 从 SessionManager 检索 DataKey | 返回 DataKey 的副本 |
| 3 | 系统 | 按 ID 从“password_entries”中选择行 | 检索加密行 |
| 4 | 系统 | 解密所有字段 | 每个加密的 JSON 字符串 → 解析 → XChaCha20-Poly1305 解密。密码：使用 DataKey（标准）或 SecureModeKey（如果“security_mode=1”）解密 |
| 5 | 系统 | 将解密的“PasswordEntry”存储在 ViewModel StateFlow 中 | 内存中保存的所有明文字段 |
| 6 | 系统 | 渲染细节屏幕 | 密码默认显示为“••••••••••••” |
| 7 | 用户 | 点击眼睛图标 | 密码切换为纯文本显示。不存在自动隐藏计时器。保持可见，直到用户关闭或导航离开。 |
| 8 | 用户 | 点击复制图标 | 密码字符串已复制到剪贴板。自动清除计时器启动（30 秒）。消息：“密码已复制，将在30秒后清除” |

### 正常流程（安全模式进入）

| 步 | 演员 | 行动 | 系统响应 |
|------|-------|--------|-----------------|
| 1–5 | — | 与标准流量相同 | 相同 — 密码被解密到内存中 |
| 6 | 系统 | 渲染细节屏幕 | 密码显示为“••••••••••••”。没有眼睛图标。仅“使用”（使用）按钮可见。 |
| 7 | 用户 | 点击“使用”按钮 | 系统从数据库中重新读取加密后的密码；通过“SecurityModeManager.usePassword()”按需解密；将明文复制到剪贴板；从内存中擦除解密的字节；启动 30 秒自动清除计时器。消息：“密码已使用（已复制），将在30秒后清除” |

### 错误流

| 健康）状况 | 系统行为 |
|-----------|----------------|
| E-RET-1：会话已锁定 | `getDataKey()` 返回 null。消息：“保险库已锁定”。详细信息屏幕显示错误。 |
| E-RET-2：在数据库中找不到条目 | `getById()` 返回 null。没有显示条目。 |
| E-RET-3：解密失败（密钥错误、数据损坏） | 捕获异常。消息：“加载失败”或异常消息。 |
| E-RET-4：安全模式“usePassword”失败 | 捕获异常。消息：“密码使用失败”或异常消息。剪贴板未修改。 |

## 3.3 自动填充

### 状态：未实施

自动填充设置屏幕作为 UI 占位符存在。它包含两个具有仅限本地状态（不持久）的非功能性切换。屏幕上明确注明：“本页仅实现界面，功能将在后续版本接入。”

**不存在的东西：**
- 没有“AutofillService”子类
- 没有 Android 清单服务声明
- 没有自动填充数据集提供程序
- 无填充/保存请求处理
- 没有跨平台自动填充抽象

**有关自动填充的跨平台行为差异：**
所有平台的行为都相同——自动填充不可用。

## 3.4 删除密码

### 正常流程（标准进入，安全模式关闭）

| 步 | 演员 | 行动 | 系统响应 |
|------|-------|--------|-----------------|
| 1 | 用户 | 在密码详细信息屏幕上，点击“删除” | 确认对话框：“删除「{title}」后将无法恢复，是否继续？” |
| 2 | 用户 | 确认删除 | 系统调用 `passwordRepository.deleteById(id)` |
| 3 | 系统 | 执行“DELETE FROM password_entries WHERE id = ?” | 从数据库中删除的行 |
| 4 | 系统 | 返回成功 | 导航至 Vault 屏幕；刷新条目列表 |

### 正常流程（安全模式条目，每个条目“securityMode=true”）

| 步 | 演员 | 行动 | 系统响应 |
|------|-------|--------|-----------------|
| 1 | 用户 | 点击“删除” | 触发重新身份验证（不是确认对话框） |
| 2 | 系统 | 检查生物识别是否已启用且可用 | — |
| 2a | 系统 | 生物识别可用 → 显示生物识别提示 | — |
| 2b | 系统 | 生物识别不可用 → 显示主密码对话框 | — |
| 3 | 用户 | 认证成功 | `verifiedAction = SensitiveAction.Delete` |
| 4 | 系统 | 显示确认对话框（与标准流程步骤 1 相同） | — |
| 5 | 用户 | 确认 | 与标准流程步骤 2–4 相同 |

### 正常流程（全局安全模式开启）

| 步 | 系统行为 |
|------|----------------|
| 1 | 编辑和删除按钮在 UI 中完全隐藏。无法删除。 |

### 错误流

| 健康）状况 | 系统行为 |
|-----------|----------------|
| E-DEL-1：生物特征认证失败 | 消息：“生物识别验证失败”。用户仍停留在详细信息屏幕上。 |
| E-DEL-2：生物识别已取消 | 消息：“已取消生物识别验证”。用户仍停留在详细信息屏幕上。 |
| E-DEL-3：主密码不正确 | 消息：“主密码错误”。密码对话框保持打开状态。 |
| E-DEL-4：主密码为空 | 信息：“请输入主密码”。密码对话框保持打开状态。 |
| E-DEL-5：数据库删除失败 | 捕获异常。消息：“删除失败”或异常消息。条目未删除。 |
| E-DEL-6：条目 ID 为空 | `delete()` 立即返回（无操作）。 |

### 删除后清理

系统仅执行 SQL“DELETE”。 **不**执行以下操作：
- 删除前不会覆盖加密数据（SQLite 不保证物理擦除）
- 无 SecureModeKey 轮换
- ViewModel 状态下解密的“PasswordEntry”不会在内存中清理（一直持续到导航）
- 无删除审核日志

---

# 4. 功能需求

## 4.1 模块：密钥派生（Argon2id KDF）

### 输入
| 姓名 | 类型 | 约束条件 |
|------|------|-------------|
| 密码 | 字符数组 | 非空 |
| 盐 | 字节数组 | 16字节 |
| 配置 | Argon2配置 | 内存KB >= 8192，迭代次数>= 1，并行度>= 1，输出长度= 32 |

### 输出
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 派生密钥 | 字节数组 | 通过 Argon2id 派生的 32 字节密钥 |

### 处理逻辑
1. 系统将密码 CharArray 转换为 String，并使用 `algorithm = 2` (Argon2id) 将其传递给 libsodium 的 `PasswordHash.pwhash`。
2. 默认配置：128 MB 内存（`131072` KB）、3 次迭代、并行度 4、32 字节输出。
3. 派生完成或失败后，通过“finally”块中的“MemorySanitizer.wipe()”擦除密码 CharArray。

### 安全限制
- SEC-KDF-1：无论成功还是失败，密码 CharArray 在推导后都会被擦除。
- SEC-KDF-2：派生密钥由调用者负责在使用后擦除。

### 错误处理
- 如果 libsodium 未初始化，`pwhash` 会抛出异常。调用者将此捕获为“CryptoError”。
- 如果任何 Argon2 参数低于最小值，则行为由 libsodium 确定（通常会抛出异常）。

### 边缘情况
- EC-KDF-1：密码 CharArray 在内部转换为不可变的“String”。该字符串无法从内存中擦除，并且会一直保留在 JVM 字符串池中，直到垃圾回收为止。这是 JVM 平台上的一个已知限制。

## 4.2 模块：认证加密（XChaCha20-Poly1305）

### 输入（加密）
| 姓名 | 类型 | 约束条件 |
|------|------|-------------|
| 明文 | 字节数组 | 非空，最大 10 MB |
| 钥匙 | 字节数组 | 正好 32 字节 |

### 输出（加密）
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 加密数据 | 目的 | 包含：版本（“v2”）、iv（24字节）、密文、标签（16字节） |

### 输入（解密）
| 姓名 | 类型 | 约束条件 |
|------|------|-------------|
| 加密数据 | 加密数据 | 版本==“v2”，iv==24字节，标签==16字节 |
| 钥匙 | 字节数组 | 正好 32 字节 |

### 输出（解密）
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 明文 | 字节数组 | 原始明文 |

### 处理逻辑

**加密：**
1. 生成 24 字节随机数。
2. 调用 libsodium `SecretBox.easy(plaintext, nonce, key)` → 返回 `tag(16 bytes) ||密文`。
3. 将结果拆分为标签和密文。
4. 返回 `EncryptedData(version="v2", iv=nonce, ciphertext=ciphertext, tag=tag)`。

**解密：**
1. 验证版本==“v2”，随机数大小==24，标签大小==16。
2. 重新组装：`标签||密文`。
3. 调用libsodium `SecretBox.openEasy(tag||ciphertext, iv, key)` → 返回明文。
4. 如果身份验证失败，libsodium 会抛出异常。

**存储序列化：**
`EncryptedData` 对象序列化为 JSON：`{"version":"v2","iv":"<base64>","ciphertext":"<base64>","tag":"<base64>"}`。该 JSON 字符串存储在每个数据库列中。

### 安全限制
- SEC-ENC-1：每个加密操作都会生成一个新的随机数。随机数永远不会被同一个密钥重复使用。
- SEC-ENC-2：在每次加密/解密调用开始时验证密钥大小。如果 != 32 字节，则在任何加密操作之前抛出“IllegalArgumentException”。
- SEC-ENC-3：解密通过 Poly1305 标签验证真实性。密文被篡改会导致解密失败。

### 错误处理
| 健康）状况 | 行为 |
|-----------|----------|
| 密钥大小！= 32 | 抛出“IllegalArgumentException” |
| 明文空 | 抛出“IllegalArgumentException” |
| 版本！=“v2” | 抛出“IllegalArgumentException” |
| 随机数大小！= 24 | 抛出“IllegalArgumentException” |
| 标签大小！= 16 | 抛出“IllegalArgumentException” |
| 认证失败（数据被篡改） | libsodium 抛出异常（解密返回垃圾或异常，具体取决于绑定） |

### 边缘情况
- EC-ENC-1：该类名为“AesGcmCipher”，但通过 libsodium SecretBox 实现 XChaCha20-Poly1305。命名具有误导性；实际的算法是XChaCha20-Poly1305。

## 4.3 模块：会话管理器

### 输入
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 数据密钥 | 字节数组 | 要加载到会话中的 32 字节 DataKey |
| 锁定超时时间 | 长的 | 可配置的超时（以毫秒为单位） |

### 输出
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 会话状态 | StateFlow\<会话状态\> | 可观察状态：锁定或解锁 |
| 数据密钥复制 | 字节数组 | DataKey 的副本（由 `getDataKey()` 返回） |

### 状态转换

| 方法 | 前提 | 影响 |
|--------|-------------|--------|
| `解锁（数据密钥）` | — | 关闭之前的 SensitiveData 包装器（擦除旧密钥）；使用 dataKey 的副本创建新的 SensitiveData 包装器；设置“isUnlocked=true”；重置`lastActivityTime`；清除`backgroundEnteredAtMs`；发出“SessionState.Unlocked” |
| `锁()` | — | 关闭 SensitiveData 包装器（擦除 DataKey）；设置 `isUnlocked=false`;发出 `SessionState.Locked` |
| `获取数据密钥()` | 会话必须解锁 | 将 `lastActivityTime` 更新为现在；返回 `dataKey.get().copyOf()` |
| `onAppBackground()` | — | 如果解锁且超时== -1：立即锁定。否则：记录`backgroundEnteredAtMs`。 |
| `onAppForeground()` | — | 如果解锁且后台持续时间 >= 超时：锁定。否则：重置`lastActivityTime`。 |
| `检查自动锁定()` | — | 如果解锁并自上次活动以来已过去>=超时：锁定。 |

### 处理逻辑：锁定超时值

| 价值 | 意义 |
|-------|---------|
| -1 | 当应用程序进入后台时立即锁定 |
| 0 | 切勿自动锁定 |
| 60,000 | 1分钟 |
| 30万 | 5 分钟（默认） |
| 90万 | 15分钟 |
| 1,800,000 | 30分钟 |

### 安全限制
- SEC-SES-1：DataKey 存储在“SensitiveData<ByteArray>”包装器内。在“close()”上，包装器在关键字节上调用“MemorySanitizer.wipe()”并使引用无效。
- SEC-SES-2：`getDataKey()` 返回密钥的 **副本**，而不是引用。原件仍受保护。
- SEC-SES-3：“getDataKey()”隐式刷新“lastActivityTime”，延长会话。

### 错误处理
| 健康）状况 | 行为 |
|-----------|----------|
| 锁定时调用“getDataKey()” | `IllegalStateException("会话已锁定")` |
| 锁定时调用“requireUnlocked()” | `IllegalStateException("会话已锁定")` |

### 边缘情况
- EC-SES-1：“checkAutoLock()”已定义并测试，但在当前实现中**没有定期调用者**。除非应用程序进入后台并返回，否则前台不活动本身不会触发自动锁定。
- EC-SES-2：“extendSession()”存在，但**没有调用者**。用户交互（导航、滚动、点击）不会重置不活动计时器。只有“getDataKey()”隐式刷新计时器。
- EC-SES-3：在桌面上，永远不会调用“onAppBackground()”和“onAppForeground()”。桌面会话不会根据窗口焦点自动锁定或最小化事件。
- EC-SES-4：当超时设置为“0”（从不）时，不存在最大会话生存期。会议无限期开放。

## 4.4 模块：平台密钥库（DeviceKey管理）

### 输入
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 钥匙 | 字节数组 | 32字节DataKey存储 |

### 输出
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 钥匙 | 字节数组？ | 检索到的DataKey，如果未存储则为null |
| 有设备密钥 | 布尔值 | 当前是否存储了DeviceKey |
| 是否有硬件支持 | 布尔值 | 存储是否支持硬件 |

### 处理逻辑

**安卓：**
1. 在 Android KeyStore（可用时由硬件支持）中创建一个 256 位 AES-GCM 包装密钥，别名为“securevault_device_key”。
2. `storeDeviceKey`：使用包装密钥通过 AES-256-GCM 加密 DataKey。将生成的 IV + 加密 blob 存储在 SharedPreferences (`securevault.keystore`) 中。
3.`getDeviceKey`：从SharedPreferences中读取IV + blob；使用 KeyStore 包装密钥进行解密；返回数据键。
4. `deleteDeviceKey`：从 Android KeyStore 中删除包装密钥并清除 SharedPreferences。

**桌面：**
1. 生成随机“主密钥”并将其存储在 Java 首选项中。
2. `storeDeviceKey`：将DataKey与主密钥进行异或；将结果存储为 Java Preferences 中的 base64。
3.`getDeviceKey`：从Preferences中读取base64；解码；与主密钥进行异或；返回数据键。

### 安全限制
- SEC-PKS-1 (Android)：当设备支持时，包装密钥驻留在硬件支持的 KeyStore 中。如果没有 KeyStore 密钥，则无法提取 DataKey。
- SEC-PKS-2（桌面）：**已知的弱点。**桌面实现使用 XOR 以及以明文形式存储在 Java 首选项中的密钥。对 Preferences 文件具有读取权限的攻击者可以轻松恢复 DataKey。

### 边缘情况
- EC-PKS-1：如果 Android KeyStore 密钥无效（例如，用户更改设备锁定），则“getDeviceKey()”返回 null。用户必须使用主密码解锁才能恢复DeviceKey。

## 4.5 模块：安全模式管理器

### 输入
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 明文密码 | 细绳 | 要加密的密码 |
| 加密密码 | 细绳 | 加密密码（存储格式JSON） |
| 数据密钥 | 字节数组 | 32字节数据密钥 |
| 安全模式 | 布尔值 | 该条目是否使用安全模式加密 |

### 输出
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 加密结果 | 细绳 | 存储格式的加密密码 |
| 解密结果 | 细绳 | 解密后的明文密码 |

### 处理逻辑

**安全模式的关键层次结构：**
1. 随机生成32字节的SecureModeKey，并用DataKey加密。
2. 加密的 SecureModeKey 存储在密钥“encrypted_secure_mode_key”下的“vault_config”中。
3. 第一次使用时，如果不存在 SecureModeKey，则会创建一个并保留。

**加密（`encryptPasswordForStorage`）：**
- 如果`securityMode == false`：直接用DataKey加密。
- 如果`securityMode == true`：使用DataKey从配置中解密SecureModeKey；使用 SecureModeKey 加密密码；擦除 SecureModeKey。

**解密（`decryptPasswordForRead`）：**
- 如果`securityMode == false`：直接用DataKey解密。
- 如果`securityMode == true`：使用DataKey从配置中解密SecureModeKey；使用 SecureModeKey 解密密码；擦除 SecureModeKey。

**安全复制（`usePassword`）：**
1. 将密码解密为字节数组（使用基于 securityMode 标志的适当密钥）。
2. 将字节转换为字符串。
3. 通过 `SecureClipboard.copy()` 复制到剪贴板。
4. 安排自动清除（30 秒）。
5. 擦除“finally”块中解密的字节数组。
6. 如果使用了 SecureModeKey，请在“finally”块中将其擦除。

### 安全限制
- SEC-SM-1：每次加密/解密/使用操作后，SecureModeKey 都会从内存中擦除。
- SEC-SM-2：在“usePassword()”中，无论成功还是失败，解密的密码字节都会在“finally”块中擦除。
- SEC-SM-3：SecureModeKey 永远不会暴露给 UI 层。它仅在单个操作期间存在于“SecurityModeManager”中。

### 错误处理
| 健康）状况 | 行为 |
|-----------|----------|
| SecureModeKey 解密失败 | 异常传播给调用者。密码未返回。 |
| 配置存储库不可用 | `getOrCreateSecureModeKey` 抛出。调用者收到异常。 |

### 边缘情况
- EC-SM-1：“securityMode”标志是每个条目的。即使稍后关闭全局安全模式开关，使用“securityMode=true”创建的条目仍会使用 SecureModeKey 进行加密。全局切换仅控制 UI 限制，而不控制加密层。
- EC-SM-2：SecureModeKey 永远不会轮换。一旦创建，它就会在保管库的生命周期内持续存在。删除最后一个安全模式条目不会触发 SecureModeKey 清理。

## 4.6 模块：安全剪贴板

### 输入
| 姓名 | 类型 | 约束条件 |
|------|------|-------------|
| 文本 | 细绳 | 要复制的凭证 |
| 标签 | 细绳 | 剪贴板标签（默认：“密码”） |
| 延迟时间 | 长的 | 自动清除延迟（默认：30,000 ms） |

### 处理逻辑
1. 使用平台 API 将文本复制到系统剪贴板。
2. 启动一个基于协程的计时器“delayMs”毫秒。
3. 如果在定时器触发之前发生新的复制操作，则取消之前的定时器。
4. 当计时器触发时，用空字符串替换剪贴板内容。

### 安全限制
- SEC-CLIP-1：自动清除超时固定为 30 秒。它不是用户可配置的。
- SEC-CLIP-2：每个新副本都会取消任何挂起的自动清除计时器并启动一个新计时器。
- SEC-CLIP-3：清除用空文本替换剪贴板内容，而不仅仅是删除条目。

### 错误处理
- 剪贴板操作是即发即忘的。没有异常会传播给调用者。

### 边缘情况
- EC-CLIP-1 (iOS)：整个“SecureClipboard”实现是一个无操作存根。复制和清除什么也不做。
- EC-CLIP-2：`CryptoConstants.Clipboard.DEFAULT_CLEAR_TIMEOUT_MS` (30,000) 被定义为命名常量，但从未被代码引用。相反，30 秒的值被硬编码为“expect”函数的默认参数。

## 4.7 模块：内存消毒器

### 输入
| 姓名 | 类型 | 约束条件 |
|------|------|-------------|
| 数据 | ByteArray、CharArray 或 IntArray | 擦拭目标 |
| 通过 | INT | 覆盖次数（默认值：3） |

### 处理逻辑
1. 对于每个 pass（0 到 `passes-1`）：用 `pass % 256` (ByteArray)、`(pass % 256) + 1` 作为 char (CharArray) 或 `pass` (IntArray) 覆盖每个元素。
2. 最后一遍：将所有元素填零。
3. 验证：如果所有元素均为零，则“isWiped()”返回 true。

### 安全限制
- SEC-MEM-1：在 JVM 上尽最大努力进行擦除。垃圾收集器可能已重新定位数组，将原始字节留在已释放的堆区域中。
- SEC-MEM-2：“SensitiveData<T>”提供 RAII 风格的擦除 — 在“close()”上调用清理函数（调用“MemorySanitizer.wipe()”）。
- SEC-MEM-3：“SensitiveData.ofByteArray()”存储输入的**副本**。呼叫者保留擦除原件的责任。

### 边缘情况
- EC-MEM-1：通过次数必须 > 0，否则会抛出“IllegalArgumentException”。

## 4.8 模块：生物识别认证

### 输入
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 标题 | 细绳 | 提示标题 |
| 字幕 | 细绳 | 提示字幕 |

### 输出
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 生物识别结果 | 密封类 | 成功、失败、取消、不可用、错误（消息） |

### 处理逻辑
- **Android**：使用“BiometricPrompt”和“BIOMETRIC_STRONG |”设备凭证`。提示显示在主线程上。结果映射到“BiometricResult”。
- **桌面**：始终返回“BiometricResult.NotAvailable”。
- **iOS**：始终返回“BiometricResult.NotAvailable”。

### 安全限制
- SEC-BIO-1：生物识别解锁需要将 DeviceKey 存储在 PlatformKeyStore 中（这意味着用户必须使用主密码至少解锁一次）。
- SEC-BIO-2：在设置中启用生物识别需要首先成功的生物识别提示。

### 错误处理
| 生物识别结果 | 用户界面响应 |
|-----------------|-------------|
| 成功 | 继续操作 |
| 失败的 | 错误消息：“生物识别验证失败” |
| 取消 | 错误消息：“已取消生物识别验证” |
| 无法使用 | 回退到主密码对话框 |
| 错误（消息） | 显示错误信息 |

### 边缘情况
- EC-BIO-1：存在“BiometricState”速率限制器类（最多 5 次尝试、30 秒锁定、500 毫秒反跳），但**未连接到任何身份验证流程**。应用层暴力防护未激活。保护完全依赖于操作系统级别的生物识别提示。

## 4.9 模块：密码存储库

### 输入（创建）
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| 入口 | 密码输入 | 具有明文字段的域对象 |
| 数据密钥 | 字节数组 | 32字节数据密钥 |

### 输出（创建）
| 姓名 | 类型 | 描述 |
|------|------|-------------|
| ID | 长的 | 自动生成的行 ID |

### 处理逻辑

**创造：**
1. 单独加密每个内容字段（有关加密详细信息，请参阅§4.2）。
2. 对于密码字段：通过“SecurityModeManager”（如果可用）进行路由（使用 SecureModeKey 作为安全模式条目）。
3. 从标题的加密数据中提取 IV（用作行级 IV 参考）。
4. 插入到 `password_entries` 表中。
5. 通过`last_insert_rowid()`返回自增ID。

**更新：**
1. 验证数据库中是否存在条目 ID。
2. 使用新的随机数重新加密所有字段（每次加密都会生成一个新的随机数）。
3. 更新 `password_entries` 表中的行。

**删除：**
1. 执行“DELETE FROM password_entries WHERE id = ?”。
2.无预删除覆盖。没有钥匙轮换。

**搜索：**
1. 应用数据库级过滤器（类别、收藏夹、安全模式）。
2. 将**所有**匹配行解密到“PasswordEntry”对象中。
3. 在解密字段中执行客户端文本搜索：标题、用户名、url、注释、标签。
4. 返回匹配的条目。

### 安全限制
- SEC-REPO-1：每个字段都使用独立的随机数进行加密。没有两个字段共享随机数。
- SEC-REPO-2：所有读写操作都需要 DataKey（“deleteById”除外，它只需要行 ID）。
- SEC-REPO-3：搜索将所有候选行解密到内存中。任何时候内存中明文条目的数量等于搜索结果的数量。

### 错误处理
- 所有数据库操作都通过调用 ViewModel 包装在“runCatching”中。异常作为错误消息传播。

### 边缘情况
- EC-REPO-1：SQL 中的“searchByTitle”查询存在，但从未使用过（完全解密后搜索始终在客户端）。
- EC-REPO-2：“ generated_pa​​sswords”表以**明文**形式存储生成的密码。这些都没有加密。

## 4.10 模块：截图保护

### 处理逻辑
- **Android**：在活动窗口上设置`WindowManager.LayoutParams.FLAG_SECURE`。
- **桌面**：无操作。
- **iOS**：无操作。

### 安全限制
- SEC-SS-1：应用程序初始化时默认启用屏幕截图保护。
- SEC-SS-2：用户可以在设置中将其关闭。该首选项在“vault_config”中保留为“screenshot_allowed”。

## 4.11 模块：密码生成器

### 处理逻辑
1. 用户选择预设（强等）或配置自定义参数。
2. 系统根据字符集和长度生成随机密码。
3. 生成的密码显示给用户。
4. 用户可以复制到剪贴板（适用 30 秒自动清除）。
5. 生成的密码以 **明文** 形式存储在 ` generated_pa​​sswords` 表中，并带有强度分数和时间戳。
6.可以查看最近生成的密码（最后20个）。

### 安全限制
- SEC-GEN-1：**已知的弱点。**生成的密码以明文形式存储在“ generated_pa​​sswords” SQL 表中。具有数据库访问权限的攻击者可以读取所有最近生成的密码。

---

# 5. 安全要求

## 5.1 密钥管理规则

| ID | 规则 |
|----|------|
| KM-1 | 主密码绝不会以任何形式存储。它通过 Argon2id 转换为 PasswordKey，然后立即擦除。 |
| KM-2 | 密码密钥仅在对数据密钥进行单次加密或解密操作期间存在于内存中。它在创建它的同一函数中被擦除。 |
| KM-3 | DataKey 是唯一长期存在的密钥。当保管库解锁时，它驻留在“SensitiveData”包装器内的“SessionManager”中，并在锁定时被擦除。 |
| KM-4 | 在密码更改操作期间，DataKey 永远不会更改。仅重新生成其加密包装器（PasswordKey 加密的 blob）。 |
| KM-5 | SecureModeKey 按需派生，并在每次加密/解密/使用操作后擦除。它永远不会在操作过程中保留在内存中。 |
| 公里-6 | 在保管库擦除（`clear()`）中，会话被锁定，DeviceKey 将从 PlatformKeyStore 中删除，并且所有内存中的配置都将无效。数据库条目将永久无法访问。 |

## 5.2 内存擦除行为

### 擦除了什么（具有代码级保证）

| 材料 | 擦拭时 | 机制 |
|----------|-----------|-----------|
| 密码密钥（字节数组） | 加密/解密DataKey后 | 函数体中的“MemorySanitizer.wipe()” |
| DataKey本地副本（ByteArray） | 加载到SessionManager后 | 函数体中的“MemorySanitizer.wipe()” |
| 主密码（CharArray） | Argon2推导后 | `finally` 块中的 `MemorySanitizer.wipe()` |
| 安全模式密钥 (ByteArray) | 每次加密/解密/使用后 | `finally` 块中的 `MemorySanitizer.wipe()` |
| 解密“usePassword”中的密码字节 | 剪贴板复制后 | `finally` 块中的 `MemorySanitizer.wipe()` |
| 重新解锁时的先前 DataKey | 加载新密钥之前 | `SensitiveData.close()` |
| 会话锁定上的 DataKey | 在“lock()”调用上 | `SensitiveData.close()` → `MemorySanitizer.wipe()` |

### 哪些内容没有被擦除（已知的间隙）

| 材料 | 原因 |
|----------|--------|
| 从解密字段创建的 Kotlin `String` 对象 | 字符串在 JVM 上是不可变的；不能归零。每个 `decryptField()` 返回一个 `String`。 |
| ViewModel `StateFlow` 中解密的 `PasswordEntry` | 持续存在，直到用户离开或 ViewModel 被垃圾回收。 |
| `getDataKey()` 返回的 DataKey 副本 | 调用者（ViewModel）不会擦除返回的副本。 |
| 从 Argon2Kdf 中的密码 CharArray 创建的“String” | `password.concatToString()` 在传递给 libsodium 之前创建一个不可变的副本。 |
| 在“ generated_pa​​sswords”表中生成的密码 | 以明文形式存储。从未加密过。 |

## 5.3 剪贴板策略

| 规则 | 价值 |
|------|-------|
| 自动清除超时 | 30秒（固定） |
| 机制清晰 | 用空字符串替换剪贴板 |
| 重新复制时的计时器行为 | 先前的计时器被取消；新的 30 秒计时器启动 |
| 适用于 | 密码副本、用户名副本、生成密码副本 |
| 安卓行为 | 功能性 |
| 桌面行为 | 功能性 |
| iOS 行为 | 无操作（存根） |
| 用户可配置 | 不 |

## 5.4 加密约束

| 约束 | 价值 |
|------------|-------|
| 算法 | XChaCha20-Poly1305（libsodium SecretBox） |
| 钥匙尺寸 | 256 位（32 字节） |
| 随机数大小 | 192 位（24 字节） |
| 授权标签大小 | 128 位（16 字节） |
| 随机数生成 | 每个加密随机（每个字段都是新鲜的） |
| 存储格式 | 带有 Base64 编码的 iv、密文、标签的 JSON |
| 存储格式版本 | “v2” |
| 每字段加密 | 每个数据库列都使用自己的随机数独立加密 |
| 明文元数据列 | `category`、`is_favorite`、`security_mode`、`created_at`、`updated_at` |

## 5.5 认证约束

| 约束 | 细节 |
|------------|--------|
| 保险库解锁 | 主密码或生物识别（如果已注册） |
| 生物识别注册 | 至少需要一次主密码解锁（以填充 PlatformKeyStore） |
| 启用生物识别设置 | 需要成功的生物识别提示 |
| 禁用安全模式 | 需要生物识别或主密码 |
| 在安全模式下编辑 | 需要生物识别或主密码 |
| 在安全模式下删除 | 需要生物识别或主密码 |
| 密码尝试失败 | 无应用层速率限制。没有锁定。 |
| 生物识别尝试失败 | BiometricState 速率限制器存在，但未连接。操作系统级别的限制适用。 |

---

# 6. 非功能性需求

## 6.1 性能

| 公制 | 要求 | 基础 |
|--------|-------------|-------|
| NFR-PERF-1：KDF 延迟 | 使用默认配置（128 MB，3 iter）的 Argon2id 推导在设备的能力范围内完成。预期：在现代设备上为 0.5–3 秒。 | 通过“AdaptiveArgon2Config”配置，但默认为标准配置。 |
| NFR-PERF-2：每个字段的加密/解密 | 对于典型的凭证大小 (< 1 KB)，单字段 XChaCha20-Poly1305 操作可在 < 1 毫秒内完成。 | libsodium 原生性能。 |
| NFR-PERF-3：Vault 列表加载 | 解密所有条目以供显示。条目数呈线性。没有分页。每次加载时所有条目都会解密。 | 当前的实现会解密“getAll()”上的每一行。 |
| NFR-PERF-4：搜索延迟 | 总条目数呈线性（所有条目均已解密，然后在客户端进行过滤）。 | 没有服务器端搜索索引。 |

## 6.2 安全级别

| 公制 | 价值 |
|--------|-------|
| 加密算法强度 | 具有 XChaCha20-Poly1305 的 256 位密钥（在安全裕度方面被认为相当于 AES-256-GCM） |
| KDF实力 | Argon2id 具有 128 MB 内存，3 次迭代 — 在当前默认参数下可抵抗基于 GPU 的暴力破解 |
| 设备密钥保护（Android） | Android 密钥库中支持硬件的 AES-256-GCM |
| DeviceKey 保护（桌面） | **弱。** 在 Java 首选项中与明文密钥进行异或。 |
| 随机数生成 | **已知问题。** 对所有随机值（包括键、随机数和盐）使用“kotlin.random.Random.Default”（PRNG，而不是 CSPRNG）。 |

## 6.3 跨平台一致性

| 行为 | 安卓 | 桌面 | iOS（存根） |
|----------|---------|---------|------------|
| 加密/解密 | 相同（通用模块） | 完全相同的 | 完全相同的 |
| 生物识别 | 完整（生物识别提示） | 不可用 | 不可用 |
| 剪贴板自动清除 | 功能性 | 功能性 | 无操作 |
| 截图保护 | 标志_安全 | 无操作 | 无操作 |
| 设备密钥存储 | 硬件支持的密钥库 | Java 首选项 + 异或 | 未实施 |
| 自动锁定背景 | 功能性（MainActivity 中的生命周期挂钩） | **不起作用**（无生命周期挂钩） | 未实施 |
| 不活动时自动锁定 | **不起作用**（checkAutoLock 从未定期调用） | **不起作用** | 未实施 |

## 6.4 数据一致性（离线优先）

| 要求 | 细节 |
|-------------|--------|
| NFR-数据-1 | 所有数据都存储在本地 SQLite 中。无网络同步。 |
| NFR-数据-2 | SQLDelight 提供编译时 SQL 验证和生成的类型安全查询。 |
| NFR-数据-3 | 配置值（Vault 设置、Argon2 参数）存储在“vault_config”键值表中。 |
| NFR-数据-4 | 存储格式版本（v1 → v2）之间不存在数据迁移路径。 |
| NFR-数据-5 | 数据库操作是挂起函数，在“Dispatchers.Default”上执行。 |

---

# 7. 验收标准

## 7.1 保险库设置和解锁

### AC-1：首次创建保管库
```
GIVEN the app is launched for the first time
  AND no vault config exists in the database
WHEN the user completes onboarding and enters a master password on the Register screen
THEN the system derives a PasswordKey via Argon2id
  AND generates a random 32-byte DataKey
  AND encrypts the DataKey with the PasswordKey
  AND stores the encrypted DataKey + Argon2 parameters in vault_config
  AND stores the DataKey in PlatformKeyStore
  AND loads the DataKey into SessionManager
  AND wipes the PasswordKey and password from memory
  AND transitions to UNLOCKED state
  AND navigates to the Vault screen
```

### AC-2：密码解锁（成功）
```
GIVEN a vault exists (state == LOCKED)
WHEN the user enters the correct master password and submits
THEN the system derives the PasswordKey
  AND decrypts the DataKey successfully
  AND transitions to UNLOCKED state
  AND navigates to the Vault screen
```

### AC-3：密码解锁（失败）
```
GIVEN a vault exists (state == LOCKED)
WHEN the user enters an incorrect master password and submits
THEN decryption fails (authentication tag mismatch)
  AND the system displays "主密码错误"
  AND the state remains LOCKED
  AND the password CharArray is wiped from memory
```

### AC-4：生物识别解锁（成功）
```
GIVEN a vault exists (state == LOCKED)
  AND biometric is enabled in settings
  AND a DeviceKey exists in PlatformKeyStore
WHEN the user initiates biometric unlock
  AND the OS biometric prompt returns success
THEN the system retrieves the DataKey from PlatformKeyStore
  AND loads it into SessionManager
  AND transitions to UNLOCKED state
```

### AC-5：生物识别解锁（未注册）
```
GIVEN a vault exists (state == LOCKED)
  AND no DeviceKey exists in PlatformKeyStore (user never unlocked with password)
WHEN the user initiates biometric unlock
THEN the system displays "尚未准备生物识别解锁，请先用主密码登录一次"
  AND the state remains LOCKED
```

### AC-6：防止重复保管库设置
```
GIVEN a vault already exists
WHEN setupVault() is called
THEN the system returns VaultAlreadySetup error
  AND no keys are generated or stored
```

## 7.2 密码增删改查

### AC-7：添加密码条目
```
GIVEN the vault is UNLOCKED
WHEN the user fills title, username, and password (all non-blank) and taps Save
THEN each field is encrypted individually with a fresh random nonce
  AND the encrypted entry is inserted into password_entries
  AND the user is navigated to the Vault screen
  AND the Vault list refreshes to include the new entry
```

### AC-8：添加密码与安全模式
```
GIVEN the vault is UNLOCKED
  AND the user enables the security mode toggle for the entry
WHEN the user saves the entry
THEN the password field is encrypted with the SecureModeKey (not the DataKey)
  AND all other fields are encrypted with the DataKey
  AND security_mode is set to 1 in the database row
```

### AC-9：查看密码（标准输入）
```
GIVEN the vault is UNLOCKED
WHEN the user taps an entry in the list
THEN all fields are decrypted and displayed
  AND the password is shown as "••••••••••••"
  AND an eye icon is available to toggle visibility
```

### AC-10：查看密码（安全模式进入）
```
GIVEN the vault is UNLOCKED
  AND the entry has securityMode=true
WHEN the user views the entry detail
THEN the password is shown as "••••••••••••"
  AND no eye icon is displayed
  AND only a "使用" (Use) button is displayed
```

### AC-11：复制密码（标准输入）
```
GIVEN the user is on a standard entry's detail screen
WHEN the user taps the copy icon
THEN the password is copied to the system clipboard
  AND a 30-second auto-clear timer starts
  AND the message "密码已复制，将在 30 秒后清除" is displayed
```

### AC-12：使用密码（安全模式进入）
```
GIVEN the user is on a secure mode entry's detail screen
WHEN the user taps the "使用" button
THEN the system re-reads the encrypted password from the database
  AND decrypts it using the SecureModeKey
  AND copies the plaintext to the clipboard
  AND wipes the decrypted bytes from memory
  AND starts a 30-second auto-clear timer
  AND displays "密码已使用（已复制），将在 30 秒后清除"
```

### AC-13：删除密码（标准，无安全模式）
```
GIVEN the user is on a standard entry's detail screen
  AND global Secure Mode is OFF
WHEN the user taps "删除" and confirms the dialog
THEN the entry is deleted from password_entries
  AND the user is navigated to the Vault screen
  AND the list refreshes without the deleted entry
```

### AC-14：删除密码（安全模式进入 - 生物识别重新验证）
```
GIVEN the user is on a secure mode entry's detail screen
  AND biometric is enabled and available
WHEN the user taps "删除"
THEN a biometric prompt is shown
  AND upon biometric success, a confirmation dialog appears
  AND upon confirmation, the entry is deleted
```

### AC-15：删除密码（安全模式进入-密码重新验证）
```
GIVEN the user is on a secure mode entry's detail screen
  AND biometric is NOT available
WHEN the user taps "删除"
THEN a master password dialog appears
  AND upon entering the correct password, a confirmation dialog appears
  AND upon confirmation, the entry is deleted
```

### AC-16：全局安全模式下隐藏的编辑/删除
```
GIVEN global Secure Mode is ON
WHEN the user views any entry's detail screen
THEN the Edit and Delete buttons are not rendered
  AND no edit or delete action is available
```

## 7.3 自动锁定和会话

### AC-17：后台自动锁定（立即模式）
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to -1 (immediate)
WHEN the app goes to background (Activity.onStop on Android)
THEN the system locks the session immediately
  AND wipes the DataKey from memory
  AND the state transitions to LOCKED
```

### AC-18：后台自动锁定（定时模式）
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to 300,000 ms (5 minutes)
WHEN the app goes to background
  AND the app returns to foreground after 6 minutes
THEN the system detects elapsed time >= timeout
  AND locks the session
  AND forces navigation to the Login screen
```

### AC-19：超时内未触发自动锁定
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to 300,000 ms (5 minutes)
WHEN the app goes to background
  AND returns to foreground after 2 minutes
THEN the session remains UNLOCKED
  AND lastActivityTime is refreshed
```

### AC-20：从不自动锁定模式
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to 0 (never)
WHEN the app goes to background and returns after any duration
THEN the session remains UNLOCKED
```

### AC-21：手动锁
```
GIVEN the vault is UNLOCKED
WHEN the user taps "Lock Now" in settings
THEN the system locks the session immediately
  AND navigates to the Login screen
```

## 7.4 剪贴板安全

### AC-22：剪贴板自动清除
```
GIVEN the user copies a password to clipboard
WHEN 30 seconds elapse without another copy
THEN the clipboard is replaced with empty content
```

### AC-23：重新复制时剪贴板计时器重置
```
GIVEN the user copies password A
  AND 15 seconds later copies password B
WHEN 30 seconds elapse after copying password B
THEN the clipboard is cleared
  AND password A's 15-second-old timer was cancelled
```

## 7.5 主密码更改

### AC-24：密码更改成功
```
GIVEN the vault is UNLOCKED
WHEN the user provides the correct current password and a new password
THEN the system derives a new PasswordKey from the new password
  AND re-encrypts the SAME DataKey with the new PasswordKey
  AND updates vault_config with new salt and encrypted DataKey
  AND wipes all ephemeral keys from memory
  AND the session remains UNLOCKED
  AND all existing entries remain accessible without re-encryption
```

### AC-25：密码更改失败（当前密码错误）
```
GIVEN the vault is UNLOCKED
WHEN the user provides an incorrect current password
THEN decryption of the DataKey fails
  AND the system displays "主密码错误"
  AND no config changes are persisted
  AND the session remains UNLOCKED (unchanged)
```

## 7.6 安全模式切换

### AC-26：启用安全模式（无需身份验证）
```
GIVEN Secure Mode is currently OFF
WHEN the user toggles Secure Mode ON in settings
THEN the system persists security_mode_enabled=true in vault_config
  AND all password visibility toggles are hidden
  AND edit/delete buttons are hidden on entry detail screens
```

### AC-27：禁用安全模式（需要身份验证）
```
GIVEN Secure Mode is currently ON
WHEN the user toggles Secure Mode OFF
THEN the system requests biometric authentication (if available)
  AND upon biometric success (or correct master password fallback):
    - persists security_mode_enabled=false in vault_config
    - restores normal UI behavior
```

## 7.7 攻击场景

### AC-28：暴力破解主密码（无速率限制）
```
GIVEN the vault is LOCKED
WHEN an attacker submits N incorrect passwords in rapid succession
THEN each attempt is processed independently
  AND each attempt wipes the password CharArray after derivation
  AND no application-layer lockout or delay is imposed
  AND Argon2id's computational cost (128 MB, 3 iterations) is the only brute-force resistance
```

### AC-29：数据库盗窃（对加密条目的离线攻击）
```
GIVEN an attacker obtains a copy of the SQLite database
THEN the attacker can read: category, is_favorite, security_mode, timestamps, entry count
  AND all credential fields are encrypted with XChaCha20-Poly1305
  AND the DataKey is encrypted with Argon2id-derived key
  AND brute-forcing the master password requires Argon2id computation per attempt
  AND generated_passwords table contents are readable in plaintext
```

### AC-30：内存转储攻击（解锁会话）
```
GIVEN the vault is UNLOCKED
  AND an attacker obtains a memory dump of the process
THEN the DataKey is present in memory (inside SensitiveData wrapper)
  AND decrypted String fields from recently viewed entries are present (immutable strings cannot be wiped)
  AND DataKey copies returned to ViewModels may be present (not explicitly wiped)
```

### AC-31：桌面设备密钥提取
```
GIVEN the attacker has read access to the desktop user's Java Preferences
THEN the attacker can read the XOR master key in plaintext
  AND the XOR-obfuscated DataKey in plaintext
  AND can trivially recover the DataKey by XORing the two values
  AND can decrypt all vault entries
```

### AC-32：剪贴板嗅探
```
GIVEN the user copies a password to clipboard
  AND a malicious app reads the clipboard within 30 seconds
THEN the malicious app obtains the password in plaintext
  AND after 30 seconds, the clipboard is replaced with empty content
```

### AC-33：屏幕截图攻击（Android）
```
GIVEN screenshot protection is enabled (default)
WHEN a screenshot or screen recording is attempted
THEN the system prevents capture via FLAG_SECURE
  AND the captured content shows a blank/black screen
```

### AC-34：屏幕截图攻击（桌面）
```
GIVEN the app is running on desktop
WHEN a screenshot is taken
THEN the system does NOT prevent capture (no-op implementation)
  AND the screenshot contains the visible vault content
```

---

# 8. 测试覆盖率总结

## 8.1 关键路径覆盖矩阵

| 测试区 | 优先事项 | 覆盖 | 差距 |
|-----------|----------|---------|------|
| 避难所设置 (T-1) | 批判的 | KeyManager.setupVault 的单元测试 | — |
| 密码解锁（T-2） | 批判的 | KeyManager.unlockWithPassword 的单元测试 | 无速率限制测试（不存在） |
| 生物识别解锁 (T-3) | 批判的 | KeyManager.unlockWithBiometric 的单元测试 | 特定于平台的提示在单元测试中不可测试 |
| 会话锁定（T-4、T-5、T-6） | 批判的 | SessionManager 锁定/解锁/后台/前台的单元测试 | checkAutoLock() 已测试但从未在生产中调用 |
| 现场加密 | 批判的 | AesGcmCipher 加密/解密的单元测试 | — |
| 使用错误密钥进行现场解密 | 批判的 | 预期测试 | 验证身份验证标签拒绝 |
| 每条目安全模式加密 | 高的 | SecurityModeManager 的单元测试 | — |
| 密码更改（密钥重新包装） | 高的 | KeyManager.changeMasterPassword 的单元测试 | — |
| 内存擦除（MemorySanitizer） | 高的 | 擦除/isWiped 的单元测试 | JVM GC 行为不可测试 |
| 剪贴板自动清除 | 高的 | 平台相关 | 基于时间；难以进行单元测试 |
| 安全模式 UI 限制 | 高的 | 需要进行 UI 测试 | 编写 UI 测试 |
| 删除并重新验证 | 高的 | 需要集成测试 | 需要生物识别模拟 |
| 搜索（客户端解密） | 中等的 | PasswordRepositoryImpl.search 的单元测试 | — |
| Vault 擦除（透明） | 中等的 | KeyManager.clear 的单元测试 | — |
| 生成的密码明文存储 | 低的 | 已知的设计决策 | — |

## 8.2 特定于平台的测试矩阵

| 测试 | 安卓 | 桌面 | iOS系统 |
|------|---------|---------|-----|
| PlatformKeyStore（硬件支持） | 与 Android KeyStore 的集成测试 | 单元测试（异或逻辑） | 不适用 |
| SecureClipboard 自动清除 | 集成测试 | 集成测试 | 不适用（存根） |
| 生物识别验证提示 | 手动测试/仪器仪表 | N/A（始终不可用） | 不适用（存根） |
| 屏幕安全 FLAG_SECURE | 手动测试 | N/A（无操作） | 不适用（存根） |
| 生命周期自动锁定 | 与 Activity 生命周期的集成测试 | **无法测试**（没有生命周期挂钩） | 不适用 |

## 8.3 单元测试未涵盖的所需测试场景

| # | 设想 | 所需类型 | 原因 |
|---|----------|-------------|--------|
| TS-1 | 端到端：注册→添加条目→锁定→解锁→验证条目可读 | 一体化 | 验证完整的密钥生命周期 |
| TS-2 | 后台锁定计时精度 | 一体化 | 需要真实的时钟和生命周期事件 |
| TS-3 | 30 秒后清除剪贴板 | 一体化 | 需要真正的剪贴板和计时器 |
| TS-4 | 安全模式：密码在 UI 中永远不可见 | 用户界面测试 | 需要撰写测试断言 |
| TS-5 | 安全模式：编辑/删除需要重新验证 | 用户界面测试 | 需要生物识别模拟 |
| TS-6 | 更改主密码，然后重新锁定和重新解锁 | 一体化 | 验证新密码是否有效 |
| TS-7 | 100+条目搜索性能 | 表现 | 测量线性解密成本 |
| TS-8 | 多个协程并发访问 | 压力 | SessionManager线程安全 |
| TS-9 | 数据库损坏恢复 | 故障注入 | SQLite 文件损坏处理 |
| TS-10 | 设备锁定更改后 PlatformKeyStore 失效 | 集成（安卓） | KeyStore密钥可能会失效 |

## 8.4 需要解决的已知问题

| # | 问题 | 严重性 | 类别 |
|---|-------|----------|----------|
| KI-1 | `CryptoUtils.generateSecureRandom()` 使用 `kotlin.random.Random.Default` （PRNG，而不是 CSPRNG） | 批判的 | 安全 |
| KI-2 | Desktop PlatformKeyStore 在 Java 首选项中使用 XOR 与纯文本密钥 | 高的 | 安全 |
| KI-3 | Argon2Kdf 将密码 CharArray 转换为不可变 String | 高的 | 安全 |
| KI-4 | ` generated_pa​​sswords` 表以明文形式存储密码 | 高的 | 安全 |
| KI-5 | `checkAutoLock()` 没有定期调用者（前台不活动永远不会触发锁定） | 中等的 | 行为 |
| KI-6 | 桌面应用程序没有生命周期挂钩（自动锁定永远不会触发） | 中等的 | 平台 |
| KI-7 | “BiometricState”速率限制器未连接到身份验证流程 | 中等的 | 安全 |
| KI-8 | `getDataKey()` 的 ViewModel 调用者永远不会擦除返回的 DataKey 副本 | 中等的 | 安全 |
| KI-9 | 以明文形式存储的 Argon2 配置（降级攻击面） | 低的 | 安全 |
| KI-10 | `SecurePadding` 存在但未集成（密文长度泄漏明文长度） | 低的 | 安全 |
| KI-11 | `SessionState.Error` 和 `KeyManagerState.Ready` 已定义但从未发出 | 低的 | 代码卫生 |
| KI-12 | 当超时设置为 0（从不）时，没有绝对会话 TTL | 低的 | 行为 |
| KI-13 | 自动填充功能只是 UI 占位符 — 未实现 | 低的 | 特征差距 |
| KI-14 | iOS 平台完全是存根——剪贴板、生物识别、密钥库都是无操作的 | 低的 | 平台 |
