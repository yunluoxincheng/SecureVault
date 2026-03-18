---
description: "Use when working on Kotlin Multiplatform structure, Gradle tasks, module wiring, source set placement, expect/actual implementation, or build command selection in SecureVault."
name: "SecureVault KMP Module And Task Boundaries"
---
# SecureVault KMP 模块与任务边界

- Follow module direction: UI(Compose) -> ViewModel -> UseCase/Repository -> DataSource.
- Keep `expect/actual` in matching source sets of the same KMP module (for example `shared/common/src/commonMain` with `shared/common/src/androidMain`).
- Do not move `actual` declarations to sibling modules.
- Prefer verified Gradle tasks from this repository when suggesting build/test commands:
  - `./gradlew clean build`
  - `./gradlew assemble`
  - `./gradlew compileKotlin`
  - `./gradlew shared:common:allTests`
  - `./gradlew shared:common:desktopTest`
  - `./gradlew desktopApp:jvmTest`
  - `./gradlew androidApp:testDebugUnitTest`
- If task names are uncertain, check task lists before suggesting commands:
  - `./gradlew :shared:common:tasks --all`
  - `./gradlew tasks --all`
- Do not use `iosApp:*` Gradle tasks in this workspace because `:iosApp` is not included in `settings.gradle.kts`.
- Avoid inventing non-existent root tasks (for example `ktlintCheck`, `format`, `buildHealth`) unless verified in current task list.
