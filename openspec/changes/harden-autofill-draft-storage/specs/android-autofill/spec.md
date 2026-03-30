## ADDED Requirements

### Requirement: Encrypted staging for Autofill pending-save data

The Android Autofill pending-save staging mechanism SHALL NOT persist security-sensitive draft fields in plaintext `SharedPreferences` files. Legacy plaintext data MUST be migrated with a dual-read strategy, encrypted persistence, and removal or invalidation of plaintext copies after successful migration.

#### Scenario: Cold start without Intent extras

- **WHEN** the user returns to the app after process death and Intent extras are missing but a valid pending-save payload remains within TTL in encrypted storage
- **THEN** the main flow can still recover the draft consistently with `AutofillPendingSaveStore` semantics

#### Scenario: Resolution consistency

- **WHEN** both Intent-supplied draft data and store data are present
- **THEN** resolution follows the same priority rules as `resolveAutofillDraftFromIntentAndStore` (Intent-first or as implemented — document and test the chosen order)

#### Scenario: No durable plaintext after migration

- **WHEN** migration has completed for a device
- **THEN** sensitive Autofill draft material is not left in legacy plaintext preference files used for that purpose

### Requirement: No sensitive Autofill material in logs

The Autofill save and resolution path MUST NOT log passwords, TOTP secrets, or other vault credentials in plaintext.

#### Scenario: Error handling

- **WHEN** an error occurs during Autofill draft persistence or resolution
- **THEN** diagnostics exclude credential content and follow project logging rules
