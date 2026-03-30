# Change: Vault decrypt cache, I/O dispatchers, SQLite WAL, transactional import, and Argon2 config guards

## Why

Phase D / M4 (items #5, #11, #12, #13, #14): Improve unlock-session performance and I/O safety without weakening security or silently bricking existing vaults.

## What Changes

- **#5:** Session-scoped decrypt result cache (or equivalent) with **mandatory invalidation** on every write path affecting vault entries.
- **#11:** Route blocking file/DB work through `Dispatchers.IO` (or project-standard dispatcher).
- **#12:** After DB connection, set `PRAGMA journal_mode=WAL` (and related pragmas as appropriate per platform API).
- **#13:** Wrap `ImportManager` batch operations in `transaction { }` with **documented** all-or-nothing vs partial semantics (product-approved); extend `ExportImportManagerTest` for mid-batch failure.
- **#14:** Forbid mutating persisted `VaultConfig.argon2Config` for existing users except via new registration or an explicit “performance calibration” flow — never silent mass re-parameterization.

## Impact

- Affected specs: `vault-data-layer`, `local-database`, `vault-config` (new)
- Affected code (illustrative): `VaultViewModel`, repositories, SQLDelight drivers, `ImportManager`, vault config persistence

## References

- `docs/SECURITY-FIX-PLAN.md` — 阶段 D / M4；#13/#14 先文档后代码
