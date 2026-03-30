## ADDED Requirements

### Requirement: Minimize master password string lifetime

The system SHALL minimize the duration master password material remains in immutable `String` form on hot paths used for KDF and unlock, without altering the cryptographic semantics of `PasswordHash.pwhash` or stored vault compatibility. Where feasible, implementations SHOULD use erasable buffers (`CharArray` / `ByteArray`) and clear them after use.

#### Scenario: Unlock success unchanged

- **WHEN** a user enters the correct master password on an existing vault
- **THEN** unlock succeeds exactly as before this hardening (same KDF inputs and outcomes)

#### Scenario: No plaintext passwords in logs

- **WHEN** unlock or KDF errors occur
- **THEN** logs and user-visible diagnostics do not contain the master password or KDF output
