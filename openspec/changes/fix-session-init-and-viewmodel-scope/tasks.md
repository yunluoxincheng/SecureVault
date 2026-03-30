## 1. Session (#4)

- [ ] 1.1 Audit `SessionManager` call sites (KeyManager, Autofill, lock) and document thread expectations
- [ ] 1.2 Implement serialization or single-thread access; add/update `SessionManagerTest` / `KeyManagerTest` as applicable

## 2. Libsodium initialization (#6)

- [ ] 2.1 Profile cold start and first unlock; move blocking init off main thread where feasible
- [ ] 2.2 Ensure first encrypt/decrypt waits for successful init without user-visible deadlock

## 3. Vault UI scope (#10)

- [ ] 3.1 Align `VaultViewModel` coroutine scope with lifecycle; verify `loadRequestId` cancellation paths
- [ ] 3.2 Extend or add `VaultViewModelTest`; manual navigation round-trip without permanent `isLoading`

## 4. Validation

- [ ] 4.1 `./gradlew shared:common:allTests`, `./gradlew check`, `./gradlew detekt`
- [ ] 4.2 Targeted Android/Desktop tests per `SECURITY-FIX-PLAN` 阶段 C
