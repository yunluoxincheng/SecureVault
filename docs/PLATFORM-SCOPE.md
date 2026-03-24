# SecureVault 平台范围决策说明

> 生效日期：2026-03-25  
> 状态：**当前执行范围**与**暂缓范围**以本文档为准；实现代码中可能仍保留 iOS / 多桌面目标，但不作为近期发布或专项排期目标。

---

## 英文摘要 (English summary)

Active product and engineering focus: **Android** and **Windows Desktop** (JVM / Compose Desktop), matching day-to-day development on Windows. **Paused (not in current roadmap):** iOS app and Apple ecosystem features, **macOS** and **Linux** desktop distribution, packaging, and platform-specific hardening beyond what the shared Desktop JVM build already provides. Rationale: lower debugging cost, fewer toolchain/VM dependencies, and reduced parallel platform work. Repository modules (`shared/ios`, `iosApp`, etc.) may remain for a future reopening; they are not validation or release targets until this document is updated.

---

## 一、决策结论

| 类别 | 平台 / 范围 | 状态 |
|------|-------------|------|
| 当前执行与发布目标 | Android（主力） | **活跃** |
| 当前执行与发布目标 | Desktop（JVM），以 **Windows 10/11** 为主要验证与调试环境 | **活跃** |
| 暂缓 | **iOS**（含 `iosApp`、Credential Provider、真机/模拟器专项） | **暂缓** |
| 暂缓 | **macOS** 桌面（独立打包、Keychain 路径、桌面端 mac 专项测试） | **暂缓** |
| 暂缓 | **Linux** 桌面（libsecret、.deb/.AppImage 等发行形态） | **暂缓** |

说明：**暂缓**不等于删除代码或禁止编译；表示不投入排期、不要求里程碑验收、文档与测试矩阵中的对应项标记为「非当前周期必做」。

---

## 二、背景与原因（详细）

### 2.1 开发与调试环境

当前主力开发环境为 **Windows**。在此环境下：

- **Android**：Android Studio、模拟器与真机调试链条成熟，与项目 AGP/KMP 基线一致。
- **Windows Desktop**：`desktopApp:run`、JVM 测试与 Compose Desktop 调试可直接在本机完成，反馈路径短。
- **iOS**：需要 macOS 与 Xcode，无法在 Windows 上完成原生调试与系统级能力（Keychain、Credential Provider Extension）的闭环验证。
- **macOS / Linux 桌面**：虽可与 Windows 共用 JVM Desktop 构建，但各 OS 的**密钥托管**（如 Keychain、libsecret）、**打包/签名**、**路径与权限**差异会显著增加并行维护与回归成本。

### 2.2 复杂度控制

同时推进 Android + Windows Desktop + iOS + 多 OS 桌面会导致：

- 多套 `expect/actual`、生命周期与自动锁策略（例如桌面窗口焦点）需分别验收；
- CI 与手工测试矩阵膨胀；
- 安全相关行为（剪贴板、密钥存储、截图防护）在各平台差异大，文档与 SLTC 需同步维护。

收窄到 **Android + Windows Desktop** 后，可将精力集中在：核心加密与数据层、Android Autofill、桌面端可用性与已知弱点（如 Desktop `PlatformKeyStore`）的迭代。

### 2.3 与仓库现状的关系

- `settings.gradle.kts` 未包含 `iosApp` 的情况与「iOS 暂缓」一致；后续若恢复 iOS，需单独里程碑。
- `shared/ios`、`iosMain` 等可保留为将来启用时的基座，但 **SRS/RTM/SLTC** 中针对 iOS 的验收项在暂缓期内视为「不适用当前发布」。
- Desktop 的 JVM 实现可在 Windows 上开发与测试；文档中 **DPAPI / macOS Keychain / libsecret** 等表述保留为**架构方向**，但 **macOS/Linux 发行与平台专属强化** 暂缓。

---

## 三、对其它文档的约束

- **开发计划**：见 [DEVELOPMENT-PLAN.md](DEVELOPMENT-PLAN.md) 中「平台范围决策」与阶段调整。
- **需求与测试**：SRS / RTM / SLTC / STLC 中涉及 iOS、macOS/Linux 桌面的条目：仍保留追溯价值时保留原文，并在章节或表头注明「当前周期以 Android + Windows Desktop 为验收基线」。
- **AGENTS.md**：助手默认不将 iOS 真机/模拟器任务、macOS/Linux 桌面发版作为当前工作假设。

---

## 四、恢复条件（供未来重启参考）

在具备以下条件时，可重新打开对应平台并更新本文档与开发计划：

1. **iOS**：固定 macOS + Xcode 版本基线；`iosApp` 纳入构建；Credential Provider 范围与隐私评审完成。
2. **macOS / Linux 桌面**：明确密钥存储实现与最低 OS 版本；CI 与安装包形态确定；SLTC 增加对应环境列。

---

## 五、修订历史

| 日期 | 变更 |
|------|------|
| 2026-03-25 | 初版：确立 Android + Windows Desktop 为当前范围；iOS、macOS、Linux 桌面暂缓。 |
