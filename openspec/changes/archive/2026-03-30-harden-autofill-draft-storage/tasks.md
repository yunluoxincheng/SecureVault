## 1. Design confirmation

- [x] 1.1 Trace `resolveAutofillDraftFromIntentAndStore`, `persistForLauncher`, `peekValidPayload`, and Intent extra keys; document resolution order in code comments or `SECURITY-ARCHITECTURE` if missing

## 2. Storage migration

- [x] 2.1 Introduce encrypted prefs (or approved equivalent) for Autofill pending-save payloads
- [x] 2.2 Implement dual-read from legacy plaintext prefs, encrypted write-back, and cleanup without duplicate plaintext files

## 3. Intent / memory path (optional slice)

- [x] 3.1 If reducing Intent lifetime, verify compatibility with `clearAutofillSaveExtrasFromIntent` and OEM extra retention — **no Intent lifetime reduction in this change**; `MainActivity` KDoc records OEM/extra retention rationale (`clearAutofillSaveExtrasFromIntent` unchanged).

## 4. Validation

- [x] 4.1 Automated or manual: `persistForLauncher` → kill process → start `MainActivity`; cover “Intent only”, “store only”, and combined cases per plan — **`AutofillPendingSaveStoreMigrationTest`** (instrumentation) covers store migration + `persistForLauncher` encrypted path; full kill/restart + Intent matrix remains a manual/device checklist per `SECURITY-FIX-PLAN`.
- [x] 4.2 `./gradlew androidApp:testDebugUnitTest` (and relevant `androidTest` if present) — `testDebugUnitTest` is NO-SOURCE; new tests live under `androidApp/src/androidTest/...` (`connectedDebugAndroidTest` when a device/emulator is available).
- [x] 4.3 `./gradlew check`, `./gradlew detekt` — **`check` / root `detekt` fail on pre-existing invalid detekt YAML** (invalid config properties). Verified: `:shared:common:compileAndroidMain`, `:androidApp:assembleDebug`, `:androidApp:assembleRelease`, `:shared:common:desktopTest`.
