## ADDED Requirements

### Requirement: Classified Android device key errors

On Android, operations that load or unwrap the device key SHALL distinguish at least: (1) no suitable key present, (2) ciphertext or integrity failure, (3) other system/keystore errors. User-facing messages MUST NOT expose key material or internal keystore blobs; diagnostic logs MUST be suitable for support without leaking secrets.

#### Scenario: Missing key

- **WHEN** no device key has been created yet
- **THEN** the caller receives a clear “not present” classification distinct from corruption

#### Scenario: Decrypt failure

- **WHEN** ciphertext cannot be decrypted or authenticated
- **THEN** the failure is classified separately from “not present” and does not imply the same recovery steps
