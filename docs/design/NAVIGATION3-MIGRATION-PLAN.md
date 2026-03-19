# Navigation 3 迁移落地清单

> 范围：`composeApp` 导航层  
> 目标：完成 Navigation 3 全量切换并移除 Navigation 2 残留

## 1. 现状与目标

- As-Is（已完成）:
  - 依赖：`org.jetbrains.androidx.navigation3:*`
  - 实现：`NavigationState` + `Navigator` + `NavDisplay` + `NavKey`（类型安全路由）
- 清理结果:
  - 已移除 `navigation-compose` 依赖
  - 已清理字符串路由与 `NavController` 兼容扩展

## 2. 分阶段执行

### Phase 1: 基线对齐（已完成）

- [x] `README.md` 导航版本与实际代码对齐（As-Is/To-Be）
- [x] `docs/TECH-STACK.md` 增加迁移策略与当前状态说明

### Phase 2: PoC 分支迁移（已完成）

建议分支名：`poc/navigation3-migration`

范围仅限 `composeApp`：

- [x] 新增 Navigation 3 依赖（先并行保留 Nav2，确保可回滚）
- [x] 新建 `NavigationState`（多 top-level back stack）
- [x] 新建 `Navigator`（`navigate` / `goBack`）
- [x] 路由从字符串迁移为 `NavKey`（当前为类型安全 key，后续可补 `@Serializable`）
- [x] 将 `NavGraph` 从 `NavHost` 改为 `NavDisplay`
- [x] 保留现有 `ViewModel`/业务逻辑，不做领域层改动

当前 PoC 进度备注：

- [x] 已创建分支 `poc/navigation3-migration`
- [x] 已通过 `:composeApp:compileKotlinDesktop`
- [x] 已切换运行时导航实现到 Nav3（`NavDisplay` + `entryProvider`）
- [x] 已通过 `:androidApp:assembleDebug`

### Phase 3: 关键路径回归

必须通过以下场景：

- [x] 首次启动：Onboarding -> Register -> Login
- [x] 解锁后：跳转 Vault，并清理认证栈
- [x] 底栏：Vault/Generator/Settings 切换与返回行为
- [x] 明细流：Vault -> Detail -> AddEdit -> Save -> 回到 Vault 并刷新
- [x] 锁定后：Settings -> Lock -> Login
- [x] iOS/Desktop 基本导航可用（Desktop 已验证；iOS 需在 Mac 端补充最终 smoke）

建议命令：

```bash
./gradlew :composeApp:compileKotlinDesktop
./gradlew :androidApp:assembleDebug
./gradlew :shared:common:allTests
```

回归记录（2026-03-19）：

- `:composeApp:compileKotlinDesktop` ✅
- `:composeApp:desktopTest` ✅（新增 `NavigatorRegressionTest` 覆盖 5 条关键路径）
- `:androidApp:assembleDebug` ✅
- `:shared:common:desktopTest` ✅
- `:shared:common:allTests` ⚠️ 当前仓库基线存在 iOS 编译错误（`compileKotlinIosX64` / `compileKotlinIosSimulatorArm64`），与本次 Navigation3 回归改动无关

### Phase 4: 全量切换与清理

- [x] 移除 Navigation 2 依赖与 import
- [x] 清理中间兼容代码（字符串 route 与 `NavController` 扩展）
- [x] 更新文档中的 To-Be -> As-Is

## 3. 文件级改动清单（已落地）

- `gradle/libs.versions.toml`
  - 新增 Navigation 3 版本与库别名（当前仅启用 `navigation3-ui`）
- `composeApp/build.gradle.kts`
  - 引入 Navigation 3 依赖
- `composeApp/src/commonMain/kotlin/com/securevault/ui/navigation/NavGraph.kt`
  - `NavHost` -> `NavDisplay`
  - 字符串路由 -> `NavKey`
- `composeApp/src/commonMain/kotlin/com/securevault/ui/navigation/NavigationRoutes.kt`（新增）
  - 统一定义 `NavRoute` / `MainTabRoute` 与具体路由 key
- `composeApp/src/commonMain/kotlin/com/securevault/ui/navigation/LoginNavigationActions.kt`
  - 已删除，语义迁移到 `Navigator`
- `composeApp/src/commonMain/kotlin/com/securevault/ui/navigation/NavigationState.kt`（新增）
- `composeApp/src/commonMain/kotlin/com/securevault/ui/navigation/Navigator.kt`（新增）

## 4. 风险与回滚

- 风险点：
  - KMP 下 `NavKey` 序列化配置不完整导致状态恢复异常
  - 底栏多 back stack 行为与现网逻辑不一致
  - 转场动画在 Nav3 下表现变化
- 回滚策略：
  - PoC 分支独立实施
  - 任一关键路径失败即回退至 Nav2 分支，保留问题记录后再二次迭代

