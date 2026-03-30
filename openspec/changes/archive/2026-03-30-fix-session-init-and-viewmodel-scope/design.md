## Context

`SessionManager` coordinates unlocked vault state; `LibsodiumManager` currently uses `runBlocking` for initialization. `VaultViewModel` uses request IDs to ignore stale async results — any scope/cancellation change must preserve that contract.

## Goals / Non-Goals

- Goals: Serializable session access; non-blocking app startup path for sodium; UI loading state matches active navigation and request lineage.
- Non-Goals: Large Navigator refactor (P3); unrelated ViewModels.

## Decisions

- **Session:** Prefer a single documented serialization primitive (e.g. `Mutex` + clear lock ordering) over ad-hoc `@Volatile`-only patterns if races are proven.
- **Init:** Move blocking sodium load off the main thread; gate crypto operations until ready (existing or explicit `await`/state).
- **UI:** Bind long-running work to `viewModelScope` (or platform equivalent) and cancel on `Clear`/navigation when appropriate; keep `loadRequestId` checks in `onCompletion` paths.

## Risks / Trade-offs

- **#10:** Premature cancellation can cause false empty/error UI — requires review with existing `requestId` tests (`VaultViewModelTest`).

## Open Questions

- Exact initialization API surface for “sodium ready” (callback vs `StateFlow`) — resolve in apply stage without changing vault formats.
