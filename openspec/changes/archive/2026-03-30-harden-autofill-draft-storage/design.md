## Context

Android Autofill save drafts are staged between the Autofill UI and `MainActivity`. The product already documents two sources: Intent extras and `AutofillPendingSaveStore`. Hardening storage must not break cold start when OEMs drop extras.

## Goals / Non-Goals

- Goals: Remove durable plaintext persistence of sensitive draft fields; migrate legacy data safely; preserve behavior for “Intent only”, “store only”, and combined resolution order.
- Non-Goals: Redesigning the full Autofill UX (P3); changing vault encryption formats.

## Decisions

- **Migration:** Read plaintext prefs when present; write new values encrypted; delete or overwrite legacy keys after successful migration; document rollback (feature flag / versioned read path per `SECURITY-FIX-PLAN` 7).
- **Resolution order:** Stay aligned with `resolveAutofillDraftFromIntentAndStore` — implementation must match documented priority (see code and plan).
- **TTL:** Preserve existing validity/TTL semantics for stored payloads unless a spec change is explicitly approved.

## Risks / Trade-offs

- **Regression risk:** High — Autofill paths are device-dependent. Mitigation: instrumentation or scripted manual tests for `persistForLauncher` → kill process → `MainActivity` startup.

## Open Questions

- Resolved in apply: `androidx.security:security-crypto` **1.1.0-alpha06** (`MasterKey` + `EncryptedSharedPreferences.create(Context, …)`); `minSdk` 29 unchanged.
