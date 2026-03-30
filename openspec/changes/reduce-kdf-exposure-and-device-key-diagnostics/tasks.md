## 1. KDF password handling (#7)

- [ ] 1.1 Identify all master-password `String` usages in unlock/KDF paths
- [ ] 1.2 Refactor toward shorter-lived or erasable representations without changing `pwhash` semantics
- [ ] 1.3 Run `Argon2KdfTest` and full manual unlock on Android + Desktop

## 2. Android device key diagnostics (#15)

- [ ] 2.1 Enumerate failure modes in `getDeviceKey` (or equivalent)
- [ ] 2.2 Return/sealed errors distinguishing absent key vs decrypt failure; add safe logging hooks

## 3. Validation

- [ ] 3.1 `./gradlew shared:common:allTests`, `./gradlew check`, `./gradlew detekt`
- [ ] 3.2 Android manual: fresh install, unlock, lock, unlock again
