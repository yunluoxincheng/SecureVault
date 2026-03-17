# SecureVault 开发计划

> 版本 1.0 — 2026-03-18

---

## 总览

| 阶段 | 内容 | 预估周期 | 里程碑 |
|------|------|---------|-------|
| Phase 1 | 核心架构与加密 | 4 周 | KMP 项目可编译，加密模块通过测试 |
| Phase 2 | Android MVP | 3 周 | Android 端密码 CRUD + 解锁可用 |
| Phase 3 | 安全模式 | 2 周 | 安全模式 + 导出/导入完成 |
| Phase 4 | Android 自动填充 | 2 周 | AutofillService 可用 |
| Phase 5 | iOS 支持 | 3 周 | iOS 端功能对齐 Android |
| Phase 6 | Desktop 支持 | 2 周 | Desktop 应用可用 |
| Phase 7 | 优化与测试 | 2 周 | 全平台测试通过，准备发布 |
| **总计** | | **~18 周** | |

---

## Phase 1: 核心架构与加密（4 周）

### 目标

搭建 KMP 项目骨架，实现并测试所有加密模块。

### 任务清单

#### Week 1: 项目搭建

- [x] 初始化 KMP 项目结构（shared/common, shared/android, shared/ios, shared/desktop）
- [x] 配置 `build.gradle.kts` 和 `libs.versions.toml`
- [x] 配置 Compose Multiplatform（composeApp 模块）
- [x] 配置 SQLDelight 跨平台数据库
- [x] 配置 Koin 依赖注入
- [x] 搭建 androidApp / iosApp / desktopApp 入口模块

#### Week 2: 加密核心

- [x] 实现 `AesGcmCipher` (XChaCha20-Poly1305 加密/解密)
- [x] 实现 `SecurePadding` (256 字节块随机填充)
- [x] 实现 `CryptoUtils` (安全随机、Base64、常量时间比较)
- [x] 实现 `CryptoConstants` (所有加密常量)
- [x] 实现 `EncryptedData` 数据格式（v2 存储格式）
- [x] 编写单元测试：加密/解密往返、填充/反填充、边界条件

#### Week 3: 密钥派生与存储

- [x] 定义 `Argon2Kdf` expect 接口
- [x] 实现 Android actual (`libsodium`)
- [x] 实现 Desktop actual (`libsodium`)
- [x] 实现 iOS actual (`libsodium`)
- [x] 实现 `AdaptiveArgon2Config` 自适应参数
- [x] 定义 `PlatformKeyStore` expect 接口
- [x] 实现 Android actual (`AndroidKeyStore`)
- [x] 编写单元测试：密钥派生参数验证、跨平台一致性

#### Week 4: 安全模块

- [x] 实现 `SensitiveData<T>` 敏感数据包装器
- [x] 实现 `MemorySanitizer` 内存安全擦除
- [x] 实现 `SessionManager` 会话管理
- [x] 实现 `KeyManager` 密钥管理（设置、解锁、修改密码）
- [x] 定义 `BiometricAuth` expect 接口
- [x] 实现 `BiometricState` 防暴力破解
- [x] 编写集成测试：完整解锁/锁定流程

### 验收标准

- [x] KMP 项目在 Android、Desktop 上编译通过
- [x] XChaCha20-Poly1305 加密/解密测试通过
- [x] Argon2id 密钥派生在 Android 和 Desktop 上行为一致
- [x] SessionManager 锁定/解锁状态机工作正确
- [x] SensitiveData 在 close() 后数据被清零

---

## Phase 2: Android MVP（3 周）

### 目标

实现 Android 端的基本密码管理功能。

### 任务清单

#### Week 5: 数据层

- [x] 实现 SQLDelight schema (password_entries, vault_config, generated_passwords)
- [x] 实现 `PasswordRepository` (CRUD, search, filter)
- [x] 实现 `ConfigRepository` (保险库配置读写)
- [x] 实现字段级加密/解密 (`PasswordRepository` 调用 `AesGcmCipher`)
- [x] 编写数据层测试

#### Week 6: UI 核心界面

- [ ] 实现 `UnlockScreen` (主密码输入 + 生物识别按钮)

- [ ] 实现 `SetupScreen` (首次设置主密码)
- [ ] 实现 `VaultScreen` (密码列表 + 搜索 + 分类)
- [ ] 实现 `PasswordDetailScreen` (查看密码详情 + 复制)
- [ ] 实现 `AddEditPasswordScreen` (添加/编辑 + 强度指示器)
- [ ] 实现 `SettingsScreen` (基本设置)
- [ ] 实现 Material Design 3 主题 (亮色/暗色/跟随系统)
- [ ] 配置 Compose Navigation 导航图

#### Week 7: ViewModel 与集成

- [ ] 实现所有 ViewModel (Vault, Detail, AddEdit, Unlock, Settings, Generator)
- [ ] 实现 `PasswordGenerator` (预设、自定义、历史)
- [ ] 实现 `PasswordStrengthCalculator`
- [ ] 实现 `SecureClipboard` (30 秒自动清除)
- [ ] 实现 Android `BiometricAuth` actual
- [ ] 实现 Android `ScreenSecurity` (FLAG_SECURE)
- [ ] 端到端集成测试

### 验收标准

- [ ] 首次设置主密码 → 密码库创建成功

- [ ] 主密码/生物识别解锁流程正常
- [ ] 密码 CRUD 操作正常
- [ ] 搜索和分类功能正常
- [ ] 密码复制 + 30 秒自动清除
- [ ] 亮色/暗色主题切换正常

---

## Phase 3: 安全模式（2 周）

### 目标

实现安全模式和导出/导入功能。

### 任务清单

#### Week 8: 安全模式

- [ ] 实现 `SecurityModeManager`
- [ ] 实现 `SecurityModeScreen` (开关界面、说明)
- [ ] 实现安全模式密码保存（独立 SecureModeKey 加密）
- [ ] 实现安全模式密码使用（不返回明文，直接剪贴板/填充）
- [ ] `PasswordDetailScreen` 适配安全模式（密码不可见，仅显示"使用"按钮）

- [ ] `VaultScreen` 标记安全模式条目（图标/标签区分）

#### Week 9: 导出/导入

- [ ] 实现 `ExportManager` (普通导出 + 安全模式导出)
- [ ] 实现 `ImportManager` (导入 + 去重 + 冲突处理)
- [ ] 实现 `ExportImportScreen` UI
- [ ] 安全模式导出：DataKey 加密 ExportKey → ExportKey 加密数据
- [ ] 安全模式导入：逆向解密流程
- [ ] 普通导出：可选加密或明文
- [ ] 文件选择器集成 (Android DocumentProvider)

- [ ] 编写测试：导出→导入往返验证

### 验收标准

- [ ] 安全模式密码在 UI 中始终显示为 "••••••••"

- [ ] 安全模式密码可以复制到剪贴板
- [ ] 安全模式导出文件只能在 App 内解密
- [ ] 普通导出/导入往返数据完整
- [ ] 导入时正确处理重复记录

---

## Phase 4: Android 自动填充（2 周）

### 目标

实现 Android AutofillService，支持自动填充和保存检测。

### 任务清单

#### Week 10: AutofillService

- [ ] 实现 `SecureVaultAutofillService` (onFillRequest, onSaveRequest)

- [ ] 实现 `AutofillParser` (ViewNode 遍历、字段类型识别)
- [ ] 实现 `CredentialMatcher` (域名/包名匹配)
- [ ] 实现 `FillResponseBuilder` (Dataset 构建、SaveInfo)
- [ ] 实现 `AutofillBlocklist` (黑名单)
- [ ] 配置 `AndroidManifest.xml` (AutofillService 声明)
- [ ] 配置 `autofill_service_configuration.xml`

#### Week 11: 保存 UI 与集成

- [ ] 实现 `AutofillSaveScreen` (保存新凭证 UI)
- [ ] 实现 `AutofillAuthScreen` (解锁后再保存)
- [ ] 实现 `CredentialSelectorScreen` (多凭证选择)

- [ ] 实现 `SaveDetector` (新凭证/更新检测)
- [ ] 锁定状态处理（显示"打开保险库"选项）
- [ ] 安全模式凭证的自动填充支持
- [ ] 在真实应用和浏览器中测试

### 验收标准

- [ ] Chrome/Firefox 中可以自动填充
- [ ] 第三方 App 中可以自动填充
- [ ] 检测到新密码时弹出保存提示
- [ ] 检测到密码变更时弹出更新提示
- [ ] 锁定状态下只显示"打开保险库"
- [ ] 不在自身和其他密码管理器中触发

---

## Phase 5: iOS 支持（3 周）

### 目标

iOS 端功能对齐 Android。

### 任务清单

#### Week 12: iOS 基础

- [ ] 配置 iOS 项目（Xcode, Swift/ObjC 桥接）
- [ ] 实现 iOS `PlatformKeyStore` actual (Keychain + Secure Enclave)
- [ ] 实现 iOS `Argon2Kdf` actual (libsodium)
- [ ] 实现 iOS `BiometricAuth` actual (LAContext)
- [ ] 实现 iOS `SecureClipboard` actual
- [ ] 实现 iOS `SqlDriverFactory`

#### Week 13: iOS UI 与功能

- [ ] 验证 Compose Multiplatform 在 iOS 上的表现

- [ ] 调整 iOS 特定 UI（导航手势、系统样式）
- [ ] 测试所有核心功能（解锁、CRUD、生成器、安全模式）
- [ ] 处理 iOS 内存管理差异（Kotlin/Native）

#### Week 14: iOS CredentialProvider

- [ ] 实现 `ASCredentialProviderExtension`
- [ ] 实现 Extension 与主应用的数据共享 (App Group)
- [ ] 实现凭证列表和选择界面
- [ ] 测试 Safari 和第三方 App 自动填充
- [ ] 处理 Extension 生命周期

### 验收标准

- [ ] iOS 端所有核心功能与 Android 一致

- [ ] Face ID / Touch ID 工作正常
- [ ] Safari 和第三方 App 自动填充正常
- [ ] 安全模式在 iOS 上工作正常

---

## Phase 6: Desktop 支持（2 周）

### 目标

Desktop 端（Windows、macOS、Linux）基本可用。

### 任务清单

#### Week 15: Desktop 基础

- [ ] 实现 Desktop `PlatformKeyStore` actual

  - Windows: DPAPI
  - macOS: Keychain
  - Linux: libsecret
- [ ] 实现 Desktop `Argon2Kdf` actual (libsodium)
- [ ] 实现 Desktop `SecureClipboard` actual

- [ ] 实现 Desktop `SqlDriverFactory`
- [ ] 配置 Desktop 窗口和主题

#### Week 16: Desktop 功能

- [ ] 验证 Compose Desktop 表现
- [ ] 实现全局快捷键（快速复制密码）
- [ ] 实现系统托盘图标（最小化到托盘）

- [ ] 测试所有功能（解锁、CRUD、安全模式、导出/导入）
- [ ] 打包安装包（Windows .msi/.exe, macOS .dmg, Linux .deb/.AppImage）

### 验收标准

- [ ] Windows/macOS/Linux 上可正常运行
- [ ] 全局快捷键快速填充
- [ ] 密钥存储使用系统安全方案

- [ ] 导出/导入在桌面端正常工作

---

## Phase 7: 优化与测试（2 周）

### 目标

全平台质量保证和性能优化。

### 任务清单

#### Week 17: 测试

- [ ] 共享模块单元测试（加密、安全、存储、用例）
- [ ] Android 集成测试（AutofillService、生物识别）
- [ ] iOS 集成测试（CredentialProvider、Face ID）
- [ ] Desktop 集成测试
- [ ] 性能测试：Argon2 在各平台的耗时
- [ ] 安全审计：密钥管理、内存安全、加密正确性

#### Week 18: 优化与发布准备

- [ ] 性能优化（启动速度、列表滚动、加密延迟）
- [ ] UI 打磨（动画、过渡、边缘情况）
- [ ] 无障碍支持验证
- [ ] 编写用户文档
- [ ] 编写 README
- [ ] 创建首个发布版本

### 验收标准

- [ ] 所有平台测试通过
- [ ] Argon2 解锁耗时 < 3 秒
- [ ] 密码列表滚动流畅（60fps）
- [ ] 无安全漏洞（加密审计通过）

---

## 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| Compose iOS 性能不足 | 中 | 高 | 预留原生 SwiftUI 回退方案 |
| libsodium KMP 绑定不稳定 | 低 | 高 | 备选 BouncyCastle (JVM) + CommonCrypto (iOS) |
| iOS CredentialProvider 限制 | 中 | 中 | 研究 App Group 数据共享机制 |
| Desktop 自动填充体验差 | 高 | 低 | 浏览器扩展作为后续优化 |
| Kotlin/Native 内存管理 | 中 | 中 | 测试覆盖 + 使用 `autoreleasepool` |

---

## 优先级说明

**必须有 (Must-have):**

- 密码 CRUD + 搜索 + 分类
- XChaCha20-Poly1305 加密
- 主密码解锁
- 生物识别解锁
- Android AutofillService
- 安全模式

**应该有 (Should-have):**

- iOS 支持
- Desktop 支持
- 导出/导入
- 密码生成器

**可以有 (Nice-to-have):**

- 浏览器扩展
- 自定义分类图标
- 密码泄露检测（本地字典）
- 多保险库支持
