# Change: Harden Autofill draft storage and Intent handoff

## Why

Phase B / M2 (items #2, #3): Autofill save flows must not leave sensitive drafts in plaintext `SharedPreferences` or rely on long-lived cleartext Intent extras without a coordinated, testable strategy.

## What Changes

- Persist Autofill pending-save payloads using **EncryptedSharedPreferences** or an equivalent approved approach; migrate from legacy plaintext prefs with **dual-read**, encrypted write-back, and cleanup of plaintext artifacts.
- Align with existing **dual paths**: Intent extras (`AutofillSaveActivity` → `MainActivity`) and `AutofillPendingSaveStore`, consistent with `resolveAutofillDraftFromIntentAndStore` priority and `clearAutofillSaveExtrasFromIntent`.
- Optionally reduce Intent cleartext lifetime (e.g. in-memory holder) where compatible with OEM behavior and cold-start recovery.

## Impact

- Affected specs: `android-autofill` (new)
- Affected code (illustrative): `AutofillPendingSaveStore`, `AutofillSaveActivity`, `MainActivity`, `SecureVaultAutofillService`

## References

- `docs/SECURITY-FIX-PLAN.md` — 阶段 B；全局原则 Autofill 双路径
