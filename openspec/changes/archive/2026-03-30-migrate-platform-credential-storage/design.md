## Context

Legacy XOR-wrapped platform secrets are a known weak spot. Migration touches device-specific stores and can brick access if mishandled.

## Goals / Non-Goals

- Goals: Stronger at-rest protection for platform-held secrets; single controlled migration; measurable rollback story.
- Non-Goals: Changing vault encryption (XChaCha20/Argon2 vault format) in the same change unless explicitly required — keep scope to platform wrapper.

## Decisions

- **Backup:** Require export or full-device backup before migration where feasible; document in release notes.
- **Feature flag:** Gate new storage read/write; allow reverting to legacy reader only under controlled conditions defined with security review.
- **Ordering:** Land after M1–M4 stabilize CSPRNG, Autofill, session, and vault-layer changes where dependencies exist.

## Risks / Trade-offs

- **Highest project risk in the plan** — allocate dedicated QA and staged rollout.

## Open Questions

- Exact iOS/Desktop API choices and minimum OS versions — finalize before coding.
