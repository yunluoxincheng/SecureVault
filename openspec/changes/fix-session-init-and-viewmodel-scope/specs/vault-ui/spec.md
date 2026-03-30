## ADDED Requirements

### Requirement: Vault screen async lifecycle

Vault-related ViewModels (including `VaultViewModel`) SHALL use structured concurrency tied to UI lifecycle (e.g. `viewModelScope` on Android, equivalent on Desktop) for asynchronous vault loads and mutations. In-flight work MUST be cancelled or ignored consistently with `loadRequestId` (or successor) so navigation does not display stale loading or wrong data.

#### Scenario: Navigation away during load

- **WHEN** the user navigates away while a vault load is in progress
- **THEN** the UI does not permanently stick in a loading state and does not apply results that are superseded by a newer request

#### Scenario: Rapid navigation

- **WHEN** multiple load requests are triggered in sequence
- **THEN** only results matching the active request identifier affect the displayed vault state
