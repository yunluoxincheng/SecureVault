## ADDED Requirements

### Requirement: Session manager concurrency safety

The system SHALL define and enforce a concurrency model for `SessionManager` such that concurrent access cannot corrupt session state or violate invariants expected by `KeyManager` and Autofill flows. Where shared mutable state exists, access MUST be serialized or confined (e.g. mutex with documented lock ordering) to avoid deadlocks.

#### Scenario: Concurrent unlock and lock

- **WHEN** lock and unlock paths are invoked close together (e.g. Autofill vs user navigation)
- **THEN** session state remains consistent and no data race leaves the vault marked unlocked while keys are cleared (or vice versa) incorrectly

### Requirement: Libsodium initialization before cryptographic use

The system SHALL ensure libsodium is initialized before any operation that requires it for encryption or decryption. Initialization work that can block MUST NOT run on the Android main thread in a way that causes unacceptable jank; the implementation MUST provide a deterministic readiness path for first-use crypto.

#### Scenario: First launch encrypt/decrypt

- **WHEN** the user performs the first cryptographic operation after cold start
- **THEN** libsodium is ready and the operation succeeds without requiring ad-hoc retries from the user

#### Scenario: Startup responsiveness

- **WHEN** the application process starts
- **THEN** heavy sodium loading is offloaded from the main thread per platform constraints (measure and adjust in implementation)
