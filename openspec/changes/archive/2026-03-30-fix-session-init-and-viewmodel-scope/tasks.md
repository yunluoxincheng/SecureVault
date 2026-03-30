## 1. Session (#4)

- [x] 1.1 Audit `SessionManager` call sites (KeyManager, Autofill, lock) and document thread expectations
- [x] 1.2 Implement serialization or single-thread access; add/update `SessionManagerTest` / `KeyManagerTest` as applicable

## 2. Libsodium initialization (#6)

- [x] 2.1 Profile cold start and first unlock; move blocking init off main thread where feasible
- [x] 2.2 Ensure first encrypt/decrypt waits for successful init without user-visible deadlock

## 3. Vault UI scope (#10)

- [x] 3.1 Align `VaultViewModel` coroutine scope with lifecycle; verify `loadRequestId` cancellation paths
- [x] 3.2 Extend or add `VaultViewModelTest`; manual navigation round-trip without permanent `isLoading`

## 4. Validation

- [x] 4.1 Targeted verification: `:shared:common:desktopTest` + `:composeApp:desktopTest` (SessionManager / VaultViewModel filters), `:androidApp:compileDebugKotlin`; `:shared:common:detekt` / `:composeApp:detekt` (NO-SOURCE). Full `allTests` / repo `check`+`detekt` blocked here by existing iOS compile errors and `androidApp`/`desktopApp` detekt config.
- [x] 4.2 Targeted Android/Desktop tests per `SECURITY-FIX-PLAN` 阶段 C
