# Change: Session threading, Libsodium warmup, and Vault UI coroutine scope

## Why

Phase C / M3 (items #4, #6, #10): Reduce race conditions around `SessionManager`, avoid blocking the main thread during cryptographic initialization, and align `VaultViewModel` (and related UI) with structured concurrency and `loadRequestId` cancellation.

## What Changes

- **#4:** Define and document `SessionManager` threading model; use `Mutex` or equivalent single-threaded access where needed; avoid deadlocks with Autofill and background lock.
- **#6:** Warm up Libsodium on `Application` or before first crypto use asynchronously; minimize main-thread `runBlocking` in `LibsodiumManager` (or equivalent); ensure init completes before first encrypt/decrypt.
- **#10:** Use lifecycle-scoped coroutines (`viewModelScope` / `DisposableEffect` patterns) and align cancellation with `loadRequestId` so navigation does not leave stale loading state.

## Impact

- Affected specs: `session-runtime`, `vault-ui` (new)
- Affected code (illustrative): `SessionManager`, `LibsodiumManager`, `VaultViewModel`, navigation/Compose glue

## Out of scope

- Full **BiometricState** product integration (#9) — tracked separately pending product confirmation (`SECURITY-FIX-PLAN` §4).

## References

- `docs/SECURITY-FIX-PLAN.md` — 阶段 C / M3
