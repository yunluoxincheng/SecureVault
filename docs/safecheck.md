# SecureVault 安全与质量审查备忘（与代码核对）

> **说明**：下文条目来自一次架构/代码审查，已对照当前仓库实现核对**是否成立**、**行号/路径**及**修复建议是否可行**。核对时代码位置以 `shared/common/src/commonMain/kotlin/...` 等为准；若后续重构，请以实际文件为准。

---

## P0 — 严重 / 高优先级

### 1. 随机数生成器非密码学安全（安全）— **成立（已修复）**

**原问题**：`generateSecureRandom` 曾使用 `kotlin.random.Random.Default.nextBytes`（非 CSPRNG）。

**当前实现**：`shared/common/src/commonMain/kotlin/com/securevault/crypto/CryptoUtils.kt` — `LibsodiumManager.ensureInitialized()` 后使用 libsodium `randombytes`（`LibsodiumRandom.buf` → `ByteArray`），与现有加密栈一致。

**结论**：nonce、salt、密钥材料等路径现从 CSPRNG 取随机字节。规范与变更记录：`openspec/specs/cryptography/spec.md`，归档变更 `openspec/changes/archive/2026-03-30-fix-csprng-secure-random`。

---

### 2. 通过 Intent 传递自动填充保存草稿（含密码）（安全）— **成立（场景限定）**

**代码**：`androidApp/src/main/kotlin/com/securevault/MainActivity.kt` 中 `toAutofillDraftOrNull()` 从 `Intent` 读取 `EXTRA_AUTOFILL_PASSWORD` 等；`logAutofillIntent` 仅记录长度，不记录明文。

**结论**：在「从 Autofill 进入保存流程」路径上，密码可能出现在 **Intent extras** 中；存在被日志/异常转储/进程间传递面放大的理论风险。工程已通过 `AutofillPendingSaveStore` 在部分机型上兜底进程重启（见该文件注释），但 Intent 路径仍存在。

**修复可行性**：**部分可行**。Android 没有通用「加密 Intent」标准做法。可行方向包括：单 Activity + **应用内内存持有者**（如 `remember` + 单例 holder）传递草稿、缩短 extras 生命周期并尽快 `removeExtra`（已有 `clearAutofillSaveExtrasFromIntent`）、对持久化草稿采用下文第 3 点。**Encrypted Bundle** 并非系统 API，需自管密钥与语义。

---

### 3. Autofill 临时草稿明文写入 SharedPreferences（安全）— **成立**

**代码**：`shared/common/src/androidMain/kotlin/com/securevault/autofill/AutofillPendingSaveStore.kt`（`persistPayload` 将 `KEY_PASSWORD` 等写入默认 `SharedPreferences`）。

**结论**：与设备 root/备份面相关；属明确的敏感数据落盘风险。

**修复可行性**：**可行**。`EncryptedSharedPreferences`（AndroidX Security）或 **仅内存**（进程存活即可，与第 2 点协同）均为常见方案；需处理多进程/冷启动与超时清理（文件内已有 `MAX_AGE_MS` 思路）。

---

### 4. SessionManager 并发与可见性（代码质量）— **部分成立（需更新表述）**

**代码**：`shared/common/src/commonMain/kotlin/com/securevault/security/SessionManager.kt`（`dataKey` 为 `SensitiveData<ByteArray>?`，另有 `isUnlocked`、时间戳等字段）。

**结论**：若**多线程**同时调用 `unlock`/`lock`/`getDataKey`，仍可能出现可见性或竞态。若业务保证仅在主线程或单线程调度器访问，则实际风险较低；文档原「仅缺 @Volatile」已不完整——当前实现已用 `SensitiveData` 封装，但**仍未**使用 `Mutex`/`@Volatile`/`synchronized`。

**修复可行性**：**可行**。若确认多线程访问：对关键区加 `Mutex` 或统一在单线程上下文访问；`@Volatile` 可配合简单标志位，但复杂状态仍建议互斥。

---

### 5. Vault 加载时重复查询与重复解密（性能）— **成立**

**代码**：`composeApp/src/commonMain/kotlin/com/securevault/viewmodel/VaultViewModel.kt` 中 `loadEntries()`：先 `search("", PasswordFilter(), dataKey)` 取全量以算分类，再 `search(targetState.query, ...)` 取筛选结果；两次路径均进入 `PasswordRepositoryImpl.search` → 逐行 `toDomain` → 全字段解密。

**结论**：条目量大时，解密次数约为 **2×行数**（外加筛选逻辑），与「N+1 SQL」不同，主要是**重复全量解密**。

**修复可行性**：**可行**。例如在内存中缓存「解锁会话内已解密列表」或缓存「分类列表」、或 Repository 提供「只解密标题/分类用于侧边栏」的专用查询（需权衡架构与安全）。

---

### 6. Libsodium 同步初始化阻塞调用线程（性能）— **成立**

**代码**：`shared/common/src/commonMain/kotlin/com/securevault/crypto/LibsodiumManager.kt` 中 `ensureInitialized()` 使用 `runBlocking { initialize() }`。

**结论**：若在主线程调用 `ensureInitialized()`，可能卡顿；加密入口多处 `suspend` 路径会先走 `ensureInitialized`。

**修复可行性**：**可行**。在 `Application.onCreate` 或首屏以前台优先级 `launch` 预热；或保证首次加密均在后台调度器；避免在 UI 线程直接调用 `ensureInitialized()`。

---

## P1 — 高优先级

### 7. Argon2 密码 `CharArray` → `String`（安全）— **成立**

**代码**：`shared/common/src/androidMain/kotlin/com/securevault/crypto/Argon2Kdf.kt`：`password.concatToString()` 传入 `PasswordHash.pwhash`，`finally` 中 `MemorySanitizer.wipe(password)` 仅擦除 **CharArray**。

**结论**：`String` 副本无法可靠擦除；与 JVM 字符串池/驻留相关的风险为**理论加强项**。

**修复可行性**：**部分可行**。若底层 API 必须接受 `String`，可接受「缩短存活时间 + 避免额外复制」；根本方案是换用接受 `ByteArray`/指针擦除的绑定，或 JNI 直调 libsodium。

---

### 8. iOS / Desktop 上 DeviceKey 使用 XOR + 本地「主密钥」（安全）— **成立**

**代码**：

- iOS：`shared/common/src/iosMain/kotlin/com/securevault/security/PlatformKeyStore.kt`（`xorWithKey` + `NSUserDefaults`）；
- Desktop：`shared/common/src/desktopMain/kotlin/com/securevault/security/PlatformKeyStore.kt`（`xorWithKey` + `java.util.prefs.Preferences`）。

**结论**：非 AEAD，无完整性；主密钥同样存于用户态存储，**弱于** Android 的 Keystore + AES-GCM 封装（见 `androidMain/.../PlatformKeyStore.kt`）。

**修复可行性**：**可行但工作量大**。iOS 侧用 Keychain；Desktop 用 DPAPI/Keychain/专用凭据存储等；需分平台设计与迁移。

---

### 9. BiometricState 并发与「未接入流程」（代码质量 / 安全）— **双重事实**

**代码**：`shared/common/src/commonMain/kotlin/com/securevault/security/BiometricState.kt`（可变状态无锁）。

**结论**：

1. **并发**：若仅在单线程（如 Main）使用，竞态风险低；多线程调用仍需同步。
2. **更关键**：项目内其它文档（如 SRS KI-7）已指出 **`BiometricState` 未接入实际生物识别认证流程**，故「防暴力」类属性在运行时**可能未生效**；若需应用层限速，应在 `UnlockViewModel` 等路径显式接入。

---

### 10. ViewModel 自建 `CoroutineScope` 未取消（代码质量）— **成立；原修复表述需改**

**代码**：多个 `composeApp/.../viewmodel/*ViewModel.kt` 使用 `CoroutineScope(SupervisorJob() + Dispatchers.Default)`，**无** `onCleared`。

**重要说明**：这些类**不是** `androidx.lifecycle.ViewModel`，而是通过 Koin `factory` + `koinInject()` 取得的普通类；在 `SecureVaultApp` 生命周期内通常**单例式存活**，`scope` 可能伴随进程长期运行。

**修复可行性**：**可行，但不应照搬「override onCleared」**。可选：

- 迁移到 **Lifecycle ViewModel**（KMP 官方多平台支持）+ `viewModelScope`；
- 或在导航/Application 层 `DisposableEffect` / 显式 `close()` 中 `scope.cancel()`；
- 或缩短 Koin 作用域（与导航绑定）。

---

### 11. 使用 `Dispatchers.Default` 做 I/O（性能）— **基本成立**

**结论**：`Default` 与阻塞 I/O 混用可能挤占 CPU 池；更常见做法是对阻塞 IO 使用 `Dispatchers.IO` 或专用调度器。以仓库中 SQLDelight 异步驱动用法为准逐步调整即可。

---

### 12. SQLite 未显式配置 WAL 等（性能）— **成立（分平台）**

**代码**：

- Android：`shared/common/src/androidMain/kotlin/com/securevault/data/SqlDriverFactory.kt`（默认 `AndroidSqliteDriver`，无额外 PRAGMA）；
- Desktop：`shared/common/src/desktopMain/kotlin/com/securevault/data/SqlDriverFactory.kt`（`JdbcSqliteDriver`，未设 WAL）。

**修复可行性**：**可行**。Android 可在打开后执行 `PRAGMA journal_mode=WAL` 等；JDBC 驱动需在连接上执行相同 PRAGMA（注意线程与 SQLDelight API）。

---

### 13. 导入逐条写入、事务粒度大（性能）— **成立**

**代码**：`shared/common/src/commonMain/kotlin/com/securevault/data/ImportManager.kt` 循环中调用 `passwordRepository.create`/`update`。

**结论**：每条一次逻辑提交，开销大。

**修复可行性**：**可行**。在 `ImportManager` 或 Repository 层使用 SQLDelight 的 `transaction { }` 包裹批量插入/更新（需读 SQLDelight 版本 API）。

---

### 14. Argon2 参数与「自适应」（性能 × 安全）— **基本成立；数值需更正**

**代码**：`shared/common/src/commonMain/kotlin/com/securevault/crypto/CryptoConstants.kt`（`DEFAULT_MEMORY_KB = 131072` 即 **128 MiB**，`DEFAULT_ITERATIONS = 3`）；`AdaptiveArgon2Config.kt` 提供档位与按内存选型。

**结论**：标准档内存占用高，低端机耗时可能较长；低配 `65536` KB（64 MiB）、`iterations = 2` 为安全与性能折中。

**说明**：文档原写「131MB」不精确，应为 **131072 KB ≈ 128 MiB**。

---

## P2 — 中优先级（条目仍建议保留，细节从略）

| 编号 | 主题 | 核对要点 |
|------|------|----------|
| 15 | Android `PlatformKeyStore.getDeviceKey` 吞异常 | `androidMain/.../PlatformKeyStore.kt` 中 `catch (_: Throwable) { null }` **成立**；丢失「损坏 vs 不存在」区分。 |
| 16 | KeyManager 与 SessionManager 状态 | 需结合 `KeyManager`/`SessionManager` 使用处做一致性审计，属架构债。 |
| 17 | AppContextHolder | 全局 holder **存在**；是否改为注入属工程卫生。 |
| 18 | Repository 职责 | `PasswordRepositoryImpl` 含加解密与字段逻辑 **属实**；是否抽取 Encryptor 属重构范围。 |
| 19 | 导入文件 `readBytes()` | `shared/android/.../AndroidDocumentVaultFileGateway.kt` **属实**；大文件 OOM 风险。 |
| 20 | 无分页 | `selectAll` 等 **属实**；条目增多时内存与首屏压力上升。 |
| 21 | Compose 稳定性 | `PasswordCard` 等未统一 `@Immutable`；属优化项。 |
| 22 | JVM 内存擦除 | `MemorySanitizer` **属实**；与 AGENTS 中「敏感数据使用后清理」一致，极限场景应文档化预期。 |
| 23 | MainActivity 日志 | 仅长度 **属实**；风险低于记录明文，仍可再收敛日志级别/生产关闭。 |
| 24 | 低配 Argon2 强度 | 与威胁模型相关；可通过提高迭代/内存下限或强制标准档策略折中。 |

---

## P3 — UI/UX

以下为体验项，与代码 spot-check 一致者可保留；实现状态若变化请以界面为准。原列 `#25–#34`（键盘遮挡、注册密码提示与校验不一致、`Navigator.goBack`、登录错误不清除、触控目标、无障碍语言、对话框、长文本、加载态、RTL）仍可作为 backlog。

**补充（已核对）**：

- **注册密码**：`RegisterScreen.kt` 中 `valid = password.length >= 8`，与「建议 ≥12 位」提示 **不一致** — **成立**。

---

## 正面评价（与实现核对后修订）

- **AEAD**：字段级存储由 `AesGcmCipher` 实现，但实现上使用的是 **libsodium `SecretBox`（XChaCha20-Poly1305）**，类名具有误导性；**并非**字面意义上的 AES-GCM 字段加密。
- **Android** 设备密钥封装使用 **Android Keystore + AES-GCM**（`androidMain/.../PlatformKeyStore.kt`），与上条不同层。
- **KDF**：Argon2 使用 libsodium `PasswordHash.pwhash`（算法 ID 对应 Argon2id）。
- 三层密钥、常量时间比较、会话与 UI 安全等设计在文档 `docs/reference/SECURITY-ARCHITECTURE.md` 中有展开，此处不重复。

---

## 建议修复顺序（更新）

| 阶段 | 内容 | 涉及编号 |
|------|------|----------|
| 第一批 | 密码学随机源、敏感数据落盘与传递面 | #1, #2, #3 |
| 第二批 | 并发与生命周期（Session、协程 scope、Libsodium 预热） | #4, #6, #10 |
| 第三批 | 性能（重复解密、调度器、SQLite、批量事务、Argon2 策略） | #5, #11, #12, #13, #14 |
| 第四批 | 平台密钥与 Argon2 字符串、Android Keystore 错误区分等 | #7, #8, #15 |
| 第五批 | P2 与 UI/UX | 其余 |

**执行级计划（里程碑、交付物、验收与回滚）**：见 [SECURITY-FIX-PLAN.md](SECURITY-FIX-PLAN.md)。

---

*本文件旨在与源码一致地记录风险与可行修复；实施前请再跑 `./gradlew check` 与平台相关测试。*

---

## 附录：安全修复对现有功能的影响与回归控制（深度核对）

目的：在**不改变密码学对外行为**（能继续解锁、解密历史数据、导入导出一致）的前提下，区分「**零兼容性风险**」「**需迁移/双读**」「**行为语义变化**」三类，并给出可执行的验证手段。

### 总原则

1. **存储格式与 KDF 输出不变**：凡涉及 `Argon2` 参数、`AesGcmCipher`/SecretBox 密文、`EncryptedData` 序列化格式的修改，一律视为**高风险**，除非做显式版本迁移与双路径解密。
2. **API 契约不变**：`CryptoUtils.generateSecureRandom(size)` 保持「返回 `size` 字节任意随机」；若仅替换实现为 CSPRNG，**不**影响已有密文。
3. **自动填充双路径**：当前 `AutofillSaveActivity` **同时**（a）写入 `AutofillPendingSaveStore`、（b）在启动 `MainActivity` 的 Intent 上放置 extras（见 `openAppForDraft`）。修复 #2/#3 时若去掉其一，必须与 `MainActivity.resolveAutofillDraftFromIntentAndStore` 的优先级（Intent 优先，其次 peek store）一致，否则会破坏「OEM 丢 extras / 冷启动」场景。

---

### 按编号的影响评估

| 编号 | 对现有功能的影响 | 兼容性 / 回归风险 | 建议的验证 |
|------|------------------|-------------------|------------|
| **#1** CSPRNG | 仅实现变更，**不改变**任何字节串格式与协议 | **低**。现有测试 `CryptoUtilsTest`（长度、两次调用不相等）仍应通过；CSPRNG 同样满足「非相等」 | 跑 `shared:common:allTests`；抽样加密往返 `AesGcmCipherTest`、注册/解锁集成测 |
| **#2** Intent | 若改为「仅内存 holder」且未同步改 `AutofillSaveActivity`，会 **丢草稿** | **中高**（流程耦合） | Android Instrumentation：模拟「保存→MainActivity」；覆盖 extras 被清、仅 store 可用场景 |
| **#3** EncryptedSharedPreferences | 首次需生成/解锁主密钥；**旧明文 prefs 需迁移** | **中**。迁移失败会表现为「保存后找不到草稿」 | 实现「先尝试解密存储，失败则读旧 XML 并回写加密」；加一条迁移后清除明文的测试或手工清单 |
| **#4** SessionManager 互斥 | 正常单线程路径下行为与现网一致；若加 `Mutex`，错误持锁可能导致卡死 | **低～中**（多线程场景） | 审计 `KeyManager`/`getDataKey` 调用线程；跑 `SessionManagerTest`、`KeyManagerTest`、Autofill 相关用例 |
| **#5** 解密缓存 | 若缓存未在增删改后失效，会出现 **列表陈旧** | **中**（逻辑） | `VaultViewModelTest`、`PasswordRepositoryImplTest`；手工：编辑条目后返回列表 |
| **#6** Libsodium 预热 | 仅改变初始化时序；若 UI 在预热前触发加密且处理不当，可能出现竞态 | **低**（若仍保证 `ensureInitialized` 完成后再用） | 冷启动首屏、首次解锁、首次导出各测一次；关注是否仍有主线程 `runBlocking` |
| **#7** Argon2 与 `String` | **不得**改变 `pwhash` 输入编码与输出字节，否则 **全体主密码失效** | **高**（若误改算法/参数） | 仅做「缩短 `String` 存活、不改 libsodium 调用语义」；跑 `Argon2KdfTest`、完整 `setupVault`/`unlock` |
| **#8** iOS/Desktop 密钥存储 | 必须 **一次性迁移**：旧 XOR 密文 → 新封装 | **高**（迁移） | 分平台集成测试；首次启动读旧数据、写新格式、二次启动仅新格式 |
| **#9** BiometricState 接入 | 可能增加「生物识别失败后的等待」，与现「仅系统对话框」行为不同 | **中**（产品/UX） | 与需求确认阈值；跑解锁与 Autofill 生物识别流程 |
| **#10** 取消 `scope` | 若过早 `cancel`，飞行中的 `loadEntries` 可能被取消，需与现有 `requestId`/错误处理一致 | **中** | 导航往返、快速切换 Tab；确认无「永久 loading」 |
| **#11** `Dispatchers.IO` | 一般 **改善** 阻塞 I/O，不更改结果语义 | **低** | 全量 `desktopTest`/`android` 单元与集成测 |
| **#12** WAL | 绝大多数场景仅提升并发；极少数文件系统或只读介质异常 | **低** | 桌面与真机各跑一次导入导出与并发读 |
| **#13** 导入事务 | 由「部分成功」变为「单批原子」时，**失败时回滚范围**可能变大 | **中**（语义） | 明确文档：整批失败 vs 逐条跳过；`ExportImportManagerTest` 扩展错误中途场景 |
| **#14** Argon2 档位 | 修改**已存** `VaultConfig` 中的参数会导致旧设备无法解锁 | **极高**（若动存量配置） | 仅对新注册用户或显式「重新校准」流程改参数；存量保持 salt+argon2 元数据不变 |

---

### 与自动化测试的对应关系（仓库内已有）

- **通用密码学与会话**：`CryptoUtilsTest`、`AesGcmCipherTest`、`Argon2KdfTest`、`SessionManagerTest`、`KeyManagerTest`、`SensitiveDataTest`。
- **仓库与导入导出**：`ExportImportManagerTest`、`PasswordRepositoryImplTest`、`UserDataVaultImportIntegrationTest`。
- **ViewModel / 导航**：`VaultViewModelTest`、`NavigatorRegressionTest`、若干 Vault 屏测试。
- **Android**：`SessionLifecycleLockTest`、`AppE2ESmokeTest`、Autofill 相关（若有增则需补 Intent/store 双路径）。

**结论（针对「是否会影响现有功能」）**：

- **#1、#6、#11、#12**：在保持语义与初始化顺序的前提下，**预期不影响**业务结果；以测试回归为准。
- **#2、#3、#8、#13、#14**：涉及流程或数据形态，**必须通过**迁移策略或产品确认后再合入。
- **#4、#5、#7、#9、#10**：中等风险，**可控制**在单分支内用测试与 Code Review 锁死边界条件。

---

### 建议的合入节奏（降低一次性风险）

1. **第一批**：#1（独立 PR）+ 全量测试；不与业务重构混写。
2. **#2+#3**：同一 Autofill 链路上下线评审，附带 Instrumentation 或手工脚本（保存草稿→杀进程→恢复）。
3. **#8**：单独里程碑，含迁移开关与回滚方案。
4. **#13、#14**：先写清「行为变更说明」，再改代码。

以上核对基于当前模块划分与测试布局；若增加新入口（如新 Activity、多进程），需重复做一次调用图审计。
