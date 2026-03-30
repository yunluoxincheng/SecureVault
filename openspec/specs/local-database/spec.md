# local-database Specification

## Purpose
TBD - created by archiving change improve-vault-perf-db-import-config. Update Purpose after archive.
## Requirements
### Requirement: SQLite WAL mode where supported

For local SQLite-backed vault storage, the system SHALL enable Write-Ahead Logging (`journal_mode=WAL`) after database connection establishment on platforms where the driver and API support it (Android and Desktop in current scope), using the correct pragma execution point for each platform.

#### Scenario: Connection established

- **WHEN** the vault database connection is opened for normal operation
- **THEN** WAL mode is applied per platform rules without breaking existing on-disk databases

#### Scenario: Unsupported configuration

- **WHEN** a platform cannot enable WAL
- **THEN** the app falls back safely and does not fail vault open solely due to pragma rejection (behavior to be validated per driver)

