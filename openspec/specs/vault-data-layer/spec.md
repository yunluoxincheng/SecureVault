# vault-data-layer Specification

## Purpose
TBD - created by archiving change improve-vault-perf-db-import-config. Update Purpose after archive.
## Requirements
### Requirement: Session decrypt cache invalidation

If the implementation caches decrypted vault entry material in memory for performance during an unlocked session, the system MUST invalidate that cache on every operation that can change vault contents or keys, including inserts, updates, deletes, imports, exports that alter data, and session lock/rekey flows.

#### Scenario: Mutation after read

- **WHEN** an entry is updated or deleted after it was decrypted for display
- **THEN** subsequent reads reflect the new state and never return stale decrypted plaintext from an invalidated cache entry

### Requirement: Blocking I/O off main thread

Blocking file and database operations MUST run on a background dispatcher appropriate for I/O (e.g. `Dispatchers.IO` or a project-standard equivalent), not on the UI main thread, except where platform APIs strictly require main-thread calls (then keep the critical section minimal).

#### Scenario: Import/export and repository IO

- **WHEN** vault import, export, or large repository reads/writes execute
- **THEN** they do not perform long blocking work on the main thread

### Requirement: Atomic import batches

Batch import operations SHALL use a single database transaction (or equivalent atomic primitive) whose success/failure semantics are documented and tested. Tests MUST cover failure mid-batch per the chosen semantics.

#### Scenario: Mid-batch failure

- **WHEN** an error occurs during a multi-row import batch
- **THEN** the vault state matches the documented rollback or partial-success policy (no silent corruption)

