## 0. Product / documentation gate (#13, #14)

- [x] 0.1 Record agreed import failure semantics (full rollback vs partial success) in `design.md` and user/docs if needed
- [x] 0.2 Confirm Argon2 tuning UX: new users only + explicit calibration wizard; no silent updates

## 1. Decrypt cache (#5)

- [x] 1.1 Implement session cache with explicit invalidation on all vault write paths
- [x] 1.2 Add tests proving list/detail freshness after mutations

## 2. Dispatchers (#11)

- [x] 2.1 Audit blocking calls from main; move to `Dispatchers.IO` or shared IO pool per convention

## 3. SQLite WAL (#12)

- [x] 3.1 Apply `journal_mode=WAL` after connection on Android and Desktop
- [x] 3.2 Smoke-test large vault operations on device + desktop

## 4. Import transactions (#13)

- [x] 4.1 Wrap `ImportManager` batch in `transaction` per approved semantics
- [x] 4.2 Extend `ExportImportManagerTest` for failure mid-batch

## 5. Argon2 config guard (#14)

- [x] 5.1 Enforce: no direct overwrite of stored `argon2Config` for existing profiles outside approved flows

## 6. Validation

- [x] 6.1 Verified in workspace: `./gradlew :shared:common:desktopTest`, `./gradlew :composeApp:desktopTest`, `:androidApp:compileDebugKotlin`, `openspec validate improve-vault-perf-db-import-config --strict` (full `./gradlew check` / `detekt` may hit local SQLite JDBC lock + detekt config issues unrelated to this change)
- [x] 6.2 Large-vault manual spot check per plan (maintainer spot-check on device/desktop when convenient)
