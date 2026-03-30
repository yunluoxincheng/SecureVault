## 1. KDF password handling (#7)

- [x] 1.1 Identify all master-password `String` usages in unlock/KDF paths
- [x] 1.2 Refactor toward shorter-lived or erasable representations without changing `pwhash` semantics
- [x] 1.3 Run `Argon2KdfTest` and full manual unlock on Android + Desktop

## 2. Android device key diagnostics (#15)

- [x] 2.1 Enumerate failure modes in `getDeviceKey` (or equivalent)
- [x] 2.2 Return/sealed errors distinguishing absent key vs decrypt failure; add safe logging hooks

## 3. Validation

- [x] 3.1 `./gradlew shared:common:desktopTest` (includes `Argon2KdfTest`), `:shared:common:compileAndroidMain`, `:composeApp:compileKotlinDesktop`; `detekt` fails due to existing invalid config in repo (not introduced by this change); `allTests` pulls iOS targets not buildable in this environment
- [x] 3.2 Android manual: fresh install, unlock, lock, unlock again
