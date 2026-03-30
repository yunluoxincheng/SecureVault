## ADDED Requirements

### Requirement: Secure platform credential storage

For each supported platform that today uses a legacy XOR-based wrapper for OS-level secrets, the system SHALL migrate to platform-native secure storage (e.g. Keychain, OS credential manager) with a one-time migration that preserves user ability to unlock the vault. The migration SHALL be reversible only via an explicitly documented emergency path evaluated with security review.

#### Scenario: Successful migration

- **WHEN** an existing installation with legacy XOR-wrapped platform secrets runs the new version
- **THEN** secrets are re-wrapped into the secure store, verified, and subsequent runs use the new path without user re-enrollment

#### Scenario: Fresh install

- **WHEN** a new user installs the version with the new storage path
- **THEN** no legacy XOR write path is used for new secrets

### Requirement: Migration safety and rollback documentation

The migration SHALL NOT proceed without a documented backup/rollback strategy (including feature flags and optional legacy read). User-visible and operator documentation MUST describe behavior changes per `SECURITY-FIX-PLAN` §8.

#### Scenario: Rollback drill

- **WHEN** the rollback procedure is invoked in a test environment
- **THEN** vault unlock remains possible using the pre-agreed fallback path without silent data loss
