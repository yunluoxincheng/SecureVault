---
description: "Use when planning test execution, validating code changes, choosing Gradle test tasks, or deciding build/check order in SecureVault."
name: "SecureVault Testing And Validation Strategy"
---
# SecureVault 测试与验证策略

- Start with the most specific validation that covers the changed code, then expand scope only if needed.
- Prefer this verification order:
  - Module or target-specific tests first (for example `shared:common:desktopTest`, `desktopApp:jvmTest`, `androidApp:testDebugUnitTest`).
  - Broader module tests next (`shared:common:allTests`).
  - Full checks last (`check`, `clean build`) when confidence or integration coverage is needed.
- If uncertain about task availability, verify task names before suggesting execution:
  - `./gradlew :shared:common:tasks --all`
  - `./gradlew tasks --all`
- Do not claim validation is complete unless at least one relevant task was executed successfully.
- If tests fail for unrelated pre-existing reasons, report them separately and do not silently broaden scope to unrelated fixes.
- Keep validation recommendations consistent with repository-verified tasks and current module boundaries.
