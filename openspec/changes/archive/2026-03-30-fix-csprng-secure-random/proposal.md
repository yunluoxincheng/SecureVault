# Change: Use CSPRNG for `CryptoUtils.generateSecureRandom`

## Why

`CryptoUtils.generateSecureRandom` must feed cryptographically secure random bytes for nonces, salts, and key material. The security plan (phase A / M1, item #1) requires replacing any non-CSPRNG source while preserving the public contract (`size` bytes returned).

## What Changes

- Implement `generateSecureRandom` using a CSPRNG (e.g. libsodium `randombytes` after Libsodium init, or per-platform secure APIs via `expect`/`actual`).
- **No change** to Argon2 metadata on disk, `EncryptedData` wire format, or KDF/cipher storage contracts.
- Tests: `CryptoUtilsTest`, spot-check `AesGcmCipherTest`, `Argon2KdfTest`; register/unlock/decrypt on fresh and upgraded installs.

## Impact

- Affected specs: `cryptography` (new)
- Affected code (illustrative): `shared/common` `CryptoUtils` and platform actuals; callers remain unchanged if API is stable.

## References

- `docs/SECURITY-FIX-PLAN.md` — 阶段 A / M1
- Global principle: 随机数 API 契约（返回 `size` 字节）
