# Change: Integrate BiometricState with real biometric flows (product-gated)

## Why

`SECURITY-FIX-PLAN` §4 / item #9: `BiometricState` may not be wired to real biometric success/failure flows. Product must confirm whether app-level rate limiting and lockout are required; if yes, integrate in `UnlockViewModel` and Autofill biometric paths with clear thresholds and copy.

## What Changes

- Product decision: adopt / defer / scope of BiometricState.
- If adopted: invoke state updates from actual biometric callbacks; align failure counts and lockout with SRS expectations; tests for thresholds.

## Impact

- Affected specs: `biometric-auth` (new)
- Affected code (illustrative): `UnlockViewModel`, Autofill biometric entry, `BiometricState`

## References

- `docs/SECURITY-FIX-PLAN.md` — §4 BiometricState（#9）
