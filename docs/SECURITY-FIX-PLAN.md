# SecureVault 安全修复计划

> **依据**：[docs/safecheck.md](safecheck.md) 中的问题清单、建议修复顺序，以及附录「安全修复对现有功能的影响与回归控制」。  
> **范围**：以安全相关条目（P0–P2 及附录中已评估项）为主；P3 UI/UX 可作为独立打磨批次，本计划仅列入口径。

---

## 1. 目标与非目标

### 1.1 目标

- 消除或降低已确认的密码学、敏感数据暴露、会话与并发类风险。
- 在**不破坏现有用户数据可解锁性**的前提下交付修复（见 [safecheck.md 附录](safecheck.md) 总原则）。
- 每一批次可独立验证、可回滚，并补充或更新自动化/手工回归项。

### 1.2 非目标（本计划不强制排期）

- 大规模 UI 改版（P3）——可另立「体验优化」里程碑。
- 无迁移方案的跨平台密钥存储重写（#8）在未准备好分平台测试与回滚前，不进入实现阶段。

---

## 2. 全局原则（实施前全员对齐）

| 原则 | 说明 |
|------|------|
| 密文与 KDF 契约 | 不改变已落盘的 Argon2 参数元数据、不改变 `AesGcmCipher`/SecretBox 与 `EncryptedData` 的存储格式，除非附带版本化迁移与双路径解密。 |
| 随机数 API | `CryptoUtils.generateSecureRandom(size)` 保持「返回 `size` 字节」的契约；仅替换底层为 CSPRNG。 |
| Autofill 双路径 | `AutofillSaveActivity` → `MainActivity` 存在 **Intent extras** 与 **AutofillPendingSaveStore** 两条路径；修改 #2/#3 时必须与 `resolveAutofillDraftFromIntentAndStore` 的优先级一致。 |
| 合入节奏 | 高风险项单独里程碑；#1 建议独立 PR，不与大范围重构混写（见 safecheck 附录）。 |

---

## 3. 分阶段计划

以下阶段与 [safecheck.md「建议修复顺序」](safecheck.md) 一致，并补充**交付物、验收与验证命令**。

---

### 阶段 A — 密码学随机源（#1）

| 项目 | 内容 |
|------|------|
| **背景** | `CryptoUtils.generateSecureRandom` 使用非 CSPRNG；影响 nonce、盐、密钥材料等。 |
| **交付物** | `expect/actual` 或 libsodium `randombytes`（在 Libsodium 已初始化前提下）实现 CSPRNG；保持现有 API。 |
| **验收标准** | 全平台编译通过；`CryptoUtilsTest` 通过；抽样 `AesGcmCipherTest`、`Argon2KdfTest` 通过；新旧安装均可注册/解锁/解密。 |
| **验证** | `./gradlew shared:common:allTests`；按需 `./gradlew androidApp:testDebugUnitTest`、`./gradlew desktopApp:jvmTest`。 |
| **风险** | 低（附录已评估）；回滚为恢复 `generateSecureRandom` 实现。 |
| **依赖** | 无；**建议作为首个合并的安全 PR**。 |

---

### 阶段 B — 自动填充敏感面（#2、#3）

| 项目 | 内容 |
|------|------|
| **背景** | Intent 明文传递草稿；`SharedPreferences` 明文持久化临时密码。 |
| **交付物** | （1）EncryptedSharedPreferences 或等价方案 + **从旧明文 prefs 迁移**（双读、写回加密、清理明文）；（2）可选：应用内内存 holder 减少 Intent 存活时间；与 `clearAutofillSaveExtrasFromIntent` 协同。 |
| **验收标准** | 正常路径：确认保存 → 进入主界面草稿一致；**冷启动 / OEM 丢 extras**：仅依赖 store 仍可恢复（在 TTL 内）；迁移后无重复明文文件。 |
| **验证** | Instrumentation 或手工脚本：`persistForLauncher` → 杀进程 → 启动 `MainActivity`；覆盖「仅 Intent」「仅 store」场景。 |
| **风险** | 中高；**#2 与 #3 应同链路评审、同批次或连续批次发布**。 |
| **依赖** | 阶段 A 不阻塞本阶段；若使用需随机数的密钥封装，建议 A 已合入。 |

---

### 阶段 C — 会话、初始化与协程生命周期（#4、#6、#10）

| 编号 | 交付物要点 | 验收与验证 |
|------|------------|------------|
| **#4** | 明确 `SessionManager` 的线程模型；必要时 `Mutex` 或统一单线程访问；避免死锁。 | `SessionManagerTest`、`KeyManagerTest`；Autofill 与后台锁场景。 |
| **#6** | `Application` 或首屏前异步预热 Libsodium；减少主线程 `runBlocking`；保证首次加密前初始化完成。 | 冷启动、首次解锁、首次导出；检查主线程卡顿。 |
| **#10** | 采用 Lifecycle `ViewModel` + `viewModelScope`，或 `DisposableEffect`/导航作用域内 `scope.cancel()`；与 `loadRequestId` 取消逻辑一致。 | `VaultViewModelTest`、导航往返、无永久 `isLoading`。 |

| **风险** | #10 为中等（过早 cancel 导致状态异常）；**需与现有 `requestId` 逻辑一起 Code Review**。 |
| **依赖** | 可与阶段 D 并行，但建议在重负载 UI 变更前完成 #6。 |

---

### 阶段 D — 性能与安全折中（#5、#11、#12、#13、#14）

| 编号 | 交付物要点 | 注意事项 |
|------|------------|----------|
| **#5** | 解锁会话内解密结果缓存或等价优化；**所有写库路径必须失效缓存**。 | 与 `VaultViewModel`、增删改流程对齐；补列表新鲜度测试。 |
| **#11** | 阻塞 I/O 使用 `Dispatchers.IO`（或项目约定调度器）。 | 全量 `desktopTest`/Android 测试回归。 |
| **#12** | Android/Desktop 在连接建立后执行 `PRAGMA journal_mode=WAL` 等（按平台 API）。 | 真机 + 桌面各跑一次 DB 场景。 |
| **#13** | `ImportManager` 批量 `transaction { }`。 | **先书面定义**：整批失败 vs 部分成功语义；扩展 `ExportImportManagerTest` 中途失败场景。 |
| **#14** | **禁止**直接改写已存用户的 `VaultConfig.argon2Config`；仅新注册或显式「性能校准」向导可调参。 | 与产品确认；避免存量无法解锁。 |

| **验证** | `./gradlew shared:common:desktopTest`、`ExportImportManagerTest`；大库手工抽查。 |
| **依赖** | #13、#14 需产品/文档签字后再动代码。 |

---

### 阶段 E — 平台密钥、Argon2 实现细节、Android Keystore 诊断（#7、#8、#15）

| 编号 | 交付物要点 | 风险 |
|------|------------|------|
| **#7** | 在**不改变** `PasswordHash.pwhash` 语义前提下缩短 `String` 存活；或换可擦除 API。 | 高若误触 KDF；以 `Argon2KdfTest` + 完整解锁为准。 |
| **#8** | iOS Keychain / Desktop 凭据存储 + **一次性迁移**旧 XOR 数据；特性开关与回滚。 | **高**；单独里程碑，分平台测试。 |
| **#15** | Android `getDeviceKey` 区分「无密钥」与「解密失败」，日志可诊断，不泄露密钥。 | 低～中；注意勿向用户暴露敏感细节。 |

---

### 阶段 F — P2 余项与 P3 UI/UX（可选）

- **P2**：导入文件大小限制、分页、`AppContextHolder` 注入化等——按技术债优先级插入里程碑。
- **P3**：键盘、`Navigator`、无障碍等——独立「体验」排期，本安全计划不阻塞。

---

## 4. BiometricState（#9）说明

- 当前类**可能未接入**实际生物识别流程（见项目 SRS KI-7 类描述）。
- **计划动作**：产品确认是否需要应用层限速；若接入，在 `UnlockViewModel`（及 Autofill 生物识别路径）显式调用，并设定阈值与文案。
- **验收**：失败次数与锁定与预期一致；不误伤正常用户。

---

## 5. 里程碑与合入顺序（汇总）

| 顺序 | 里程碑 | 包含项 | 备注 |
|------|--------|--------|------|
| M1 | 密码学基础 | #1 | 独立 PR + 全量测试 |
| M2 | Autofill 敏感数据 | #2、#3 | 同链路评审；迁移与双路径必测 |
| M3 | 运行时与 UI 并发 | #4、#6、#10 | 关注主线程与 loading 状态 |
| M4 | 性能与导入语义 | #5、#11、#12、#13、#14 | #13/#14 先文档后代码 |
| M5 | 深度安全加固 | #7、#15、#8 | #8 最后、单独发布候选 |

---

## 6. 回归与质量门禁（每阶段合并前）

**必跑（与 [AGENTS.md](../AGENTS.md) 一致）：**

```bash
./gradlew shared:common:allTests
./gradlew check
./gradlew detekt
```

**按需：**

- `./gradlew androidApp:assembleDebug`、`./gradlew desktopApp:run`
- Android：`androidApp:testDebugUnitTest`、关键 `androidTest`
- 桌面：`desktopApp:jvmTest`、`shared:common:desktopTest`

**安全相关手工烟测（每 major 里程碑）：** 新装注册 → 锁定解锁 → 增删改条目 → 导出/导入 →（Android）自动填充保存草稿（含杀进程恢复）。

---

## 7. 回滚策略

| 类型 | 策略 |
|------|------|
| #1 | Git 回退单 PR；无数据迁移。 |
| #2/#3 | 保留旧 prefs 读取分支至若干版本；特性开关关闭「仅加密存储」。 |
| #8 | 迁移前完整备份；开关回退到 XOR 读取路径（仅应急，需预演）。 |
| #13 | 若事务导致用户依赖「部分导入」，需配置项选择旧行为（如产品要求）。 |

---

## 8. 文档与沟通

- 实施 #13、#14、#8 时，更新 `docs/reference/SECURITY-ARCHITECTURE.md` 或发行说明中的**行为变更**与**迁移说明**。
- 在 [safecheck.md](safecheck.md) 中可将已完成条目标注「已修复版本」便于审计（可选）。

---

*本计划随实现细化可拆分为具体 issue/任务单；负责人与日期由团队按排期填写。*
