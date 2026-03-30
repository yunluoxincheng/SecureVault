## Context

M4 bundles performance and durability improvements. Items #13 and #14 require explicit semantics and product sign-off before code changes ship.

## Goals / Non-Goals

- Goals: Faster repeated reads where safe; correct threading; WAL where supported; atomic import batches per agreed semantics; no accidental Argon2 parameter drift for existing vaults.
- Non-Goals: Changing Argon2 **on-disk** metadata format without migration; silent tuning of existing users’ KDF cost.

## Decisions

- **#13 Import semantics:** Document in this design (and user-facing docs if visible) whether a failed mid-import rolls back the entire batch or leaves partial data — **must be chosen before implementation**. If rollback to “partial import allowed” is required by product, add configuration or versioned behavior per `SECURITY-FIX-PLAN` §7.
- **#5 Cache:** Centralize invalidation hooks on create/update/delete/rekey paths; add tests for list freshness after mutation.
- **#12 WAL:** Apply after connection open; verify Android + Desktop drivers; handle platforms where WAL is unsupported gracefully.

## Risks / Trade-offs

- **Cache:** Stale decrypted data is a security/UX failure — invalidate aggressively.
- **Transactions:** Long transactions may lock UI — batch size limits may be needed.

## Migration Plan

- **#14:** No migration of KDF parameters for existing users from this change alone; new users follow current registration defaults.
