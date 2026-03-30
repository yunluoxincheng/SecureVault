## Context

Master passwords are high-value secrets. Android Keystore failures are often misclassified without distinguishing missing keys from bad ciphertext.

## Goals / Non-Goals

- Goals: Minimize `String` lifetime for password material where practical; clear diagnostics for Keystore without leaking secrets.
- Non-Goals: Changing the KDF algorithm or stored `PasswordHash` wire format; new cloud recovery.

## Decisions

- **#7:** Any API change must preserve byte-exact KDF inputs vs current behavior — golden tests required.
- **#15:** Map errors to user-visible categories (e.g. “no key”, “corrupt”, “system error”) without embedding stack traces with sensitive data in release builds.

## Risks

- **#7:** High — incorrect buffer handling can brick unlock. Mitigation: `Argon2KdfTest`, integration unlock tests, code review focused on KDF call chain only.
