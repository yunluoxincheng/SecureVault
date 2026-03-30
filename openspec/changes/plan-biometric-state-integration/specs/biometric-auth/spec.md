## ADDED Requirements

### Requirement: Consistent biometric failure handling when enabled

If the product enables app-level biometric attempt tracking, the system SHALL record successes and failures through the same `BiometricState` (or successor) for both primary unlock and Autofill biometric flows. Lockout thresholds and recovery messaging SHALL match product-approved specifications and MUST NOT brick access without a documented fallback (e.g. master password).

#### Scenario: Repeated failures

- **WHEN** biometric authentication fails repeatedly beyond the configured threshold
- **THEN** further biometric attempts are blocked per policy and the user can still unlock with an approved fallback method

#### Scenario: Success resets state

- **WHEN** biometric authentication succeeds
- **THEN** failure counters are reset per policy
