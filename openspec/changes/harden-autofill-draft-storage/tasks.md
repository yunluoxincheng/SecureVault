## 1. Design confirmation

- [ ] 1.1 Trace `resolveAutofillDraftFromIntentAndStore`, `persistForLauncher`, `peekValidPayload`, and Intent extra keys; document resolution order in code comments or `SECURITY-ARCHITECTURE` if missing

## 2. Storage migration

- [ ] 2.1 Introduce encrypted prefs (or approved equivalent) for Autofill pending-save payloads
- [ ] 2.2 Implement dual-read from legacy plaintext prefs, encrypted write-back, and cleanup without duplicate plaintext files

## 3. Intent / memory path (optional slice)

- [ ] 3.1 If reducing Intent lifetime, verify compatibility with `clearAutofillSaveExtrasFromIntent` and OEM extra retention

## 4. Validation

- [ ] 4.1 Automated or manual: `persistForLauncher` → kill process → start `MainActivity`; cover “Intent only”, “store only”, and combined cases per plan
- [ ] 4.2 `./gradlew androidApp:testDebugUnitTest` (and relevant `androidTest` if present)
- [ ] 4.3 `./gradlew check`, `./gradlew detekt`
