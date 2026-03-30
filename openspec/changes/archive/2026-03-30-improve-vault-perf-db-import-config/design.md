## Context

M4 bundles performance and durability improvements. Items #13 and #14 require explicit semantics and product sign-off before code changes ship.

## Goals / Non-Goals

- Goals: Faster repeated reads where safe; correct threading; WAL where supported; atomic import batches per agreed semantics; no accidental Argon2 parameter drift for existing vaults.
- Non-Goals: Changing Argon2 **on-disk** metadata format without migration; silent tuning of existing users’ KDF cost.

## Decisions

- **#13 Import semantics (approved):** Password-vault **batch import** runs inside a **single SQLite transaction**. If any applied write in that batch fails (including simulated/assertion failures), **the whole batch rolls back**—no rows from that import attempt remain committed. Fingerprint resolution (skip/overwrite vs new insert) is computed **before** the transactional write phase using the snapshot of existing entries taken at import start.
- **#5 Cache:** Centralize invalidation hooks on create/update/delete/rekey paths; add tests for list freshness after mutation.
- **#12 WAL:** Apply after connection open; verify Android + Desktop drivers; handle platforms where WAL is unsupported gracefully.

## Risks / Trade-offs

- **Cache:** Stale decrypted data is a security/UX failure — invalidate aggressively.
- **Transactions:** Long transactions may lock UI — batch size limits may be needed.

## Migration Plan

- **#14:** No migration of KDF parameters for existing users from this change alone; new users follow current registration defaults.

## Product notes (#14 — Argon2 tuning UX)

- **New vaults:** `setupVault` continues to apply current product-default Argon2 parameters at creation time.
- **Existing vaults:** KDF cost parameters are **not** changed as a side effect of **change master password** or other unrelated flows.
- **Future:** A dedicated optional flow (e.g. “performance calibration”) may change Argon2 for an existing vault only with explicit user consent; that flow is not part of this change.
