# cryptography Specification

## Purpose

Cross-cutting cryptographic utilities in `shared/common`: secure random bytes, KDF/cipher usage, and storage formats. Behavior is normative for Android and Windows Desktop targets in the current platform scope.

## Requirements
### Requirement: Cryptographically secure random bytes

The system SHALL provide `CryptoUtils.generateSecureRandom(size: Int)` such that it returns exactly `size` bytes from a cryptographically secure pseudorandom number generator (CSPRNG) suitable for nonces, salts, and key material.

#### Scenario: Correct length

- **WHEN** callers request `generateSecureRandom(n)` for a non-negative supported `n`
- **THEN** the returned `ByteArray` has length `n`

#### Scenario: No plaintext secrets in logs

- **WHEN** random bytes are generated for cryptographic use
- **THEN** the implementation MUST NOT write raw random output or derived secrets to logs or non-volatile debug surfaces

