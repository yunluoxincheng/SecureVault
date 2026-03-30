## 1. Specification alignment

- [x] 1.1 Confirm Libsodium initialization ordering vs first `generateSecureRandom` call on each platform

## 2. Implementation (apply stage; not in this proposal PR if strictly spec-only repo policy)

- [x] 2.1 Replace `generateSecureRandom` internals with CSPRNG; keep `size`-byte contract
- [x] 2.2 Add or adjust unit tests in `CryptoUtilsTest` and related crypto tests per `SECURITY-FIX-PLAN` 阶段 A 验收

## 3. Validation

- [x] 3.1 `./gradlew shared:common:desktopTest` (passes). `shared:common:allTests` fails at iOS native compile in this workspace (pre-existing; scoped platforms Android + Desktop compile and tests OK).
- [x] 3.2 Root `./gradlew check` / `./gradlew detekt` fail here (SQLDelight migration verify JNI, detekt YAML vs plugin); crypto change validated via `desktopTest`, `:androidApp:compileDebugKotlin`, `:composeApp:compileKotlinDesktop`.
- [x] 3.3 Manual smoke: new install register → unlock → decrypt path (Android + Desktop as scoped)
