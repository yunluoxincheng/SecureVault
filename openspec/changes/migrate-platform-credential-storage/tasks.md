## 0. Preconditions

- [ ] 0.1 Product/security sign-off on migration + rollback; update `docs/reference/SECURITY-ARCHITECTURE.md` draft sections
- [ ] 0.2 Per-platform test matrices (iOS when in scope; Desktop mandatory for current scope)

## 1. Implementation (apply stage)

- [ ] 1.1 Implement secure storage backends per platform
- [ ] 1.2 Implement one-time migration from XOR with verification step
- [ ] 1.3 Add feature flag and legacy read path for rollback drills only

## 2. Validation

- [ ] 2.1 Fresh install, migrate-from-legacy, and rollback drill on supported platforms
- [ ] 2.2 `./gradlew shared:common:allTests`, targeted desktop/iOS tasks when enabled
- [ ] 2.3 Document user-facing migration notes in release materials
