## ADDED Requirements

### Requirement: Argon2 parameters unchanged for existing users

The system MUST NOT silently overwrite persisted `VaultConfig` Argon2 (KDF) parameters for existing vaults. Changes to Argon2 cost parameters for an existing database are allowed only through explicit user-driven flows (e.g. new vault registration defaults or a dedicated “performance calibration” wizard), never as a side effect of unrelated code paths.

#### Scenario: App upgrade

- **WHEN** an existing user upgrades the application
- **THEN** their stored Argon2 parameters remain readable and are not replaced without explicit user consent through an approved flow

#### Scenario: New registration

- **WHEN** a new vault is created
- **THEN** current product defaults for Argon2 may be applied at creation time per existing metadata contracts
