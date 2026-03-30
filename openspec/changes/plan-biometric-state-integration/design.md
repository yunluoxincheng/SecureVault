## Context

Biometric UX and threat model differ by OEM. This change is **blocked on product confirmation** before implementation.

## Goals / Non-Goals

- Goals: If enabled, consistent rate limiting between main unlock and Autofill; no accidental permanent lockout without recovery path messaging.
- Non-Goals: Replacing system biometric APIs; P3 keyboard/accessibility work.

## Open Questions

- Failure threshold and lock duration; whether to match system lockout only or add app-level limits.
