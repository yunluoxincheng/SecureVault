# Change: Migrate platform credential storage from legacy XOR (where applicable)

## Why

Phase E / M5 item #8: Move iOS Keychain / Desktop credential storage to approved secure stores with **one-time migration** from legacy XOR-protected data. This is **high risk** and must ship as its own milestone with per-platform QA, feature flag, and rollback (`SECURITY-FIX-PLAN` §1.2, §7).

## What Changes

- Per-platform secure storage APIs (e.g. Keychain, OS credential locker) with explicit migration steps.
- One-time read of legacy XOR blobs, re-wrap into new storage, verify, then retire XOR path behind a versioned flag.
- Rollback: documented emergency read path to XOR only if pre-agreed (plan §7); full backup before migration.

## Impact

- Affected specs: `platform-credential-storage` (new)
- Affected code: platform `PlatformKeyStore` / credential modules; **not** Android-only unless shared abstractions change

## Non-goals

- Completing this change in the same release train as casual UI tweaks; must follow staged rollout and audit.

## References

- `docs/SECURITY-FIX-PLAN.md` — 阶段 E #8；M5「最后、单独发布候选」
