# Change: Shorten master-password string lifetime and improve Android device-key diagnostics

## Why

Phase E (partial) / M5 prerequisites: Item #7 — reduce how long master password material lives as `String` in hot paths without changing `PasswordHash.pwhash` semantics. Item #15 — Android `getDeviceKey` must distinguish “no key” from “decrypt failure” and emit diagnosable, non-sensitive logs.

## What Changes

- **#7:** Prefer `CharArray`/byte buffers or shorter-lived conversions; ensure zeroization where feasible; **no** accidental change to Argon2 input encoding or stored hash format — validate with `Argon2KdfTest` and full unlock flows.
- **#15:** Structured error classification for Android Keystore unwrap; user-safe messages; logs without key material.

## Impact

- Affected specs: `kdf-password-handling`, `android-device-key` (new)
- Affected code (illustrative): `Argon2Kdf`, unlock ViewModels, `PlatformKeyStore` / `getDeviceKey`

## References

- `docs/SECURITY-FIX-PLAN.md` — 阶段 E #7, #15；M5 中与 #8 分拆实施
