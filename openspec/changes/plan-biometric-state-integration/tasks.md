## 0. Product gate

- [ ] 0.1 Confirm whether BiometricState integration ships in this roadmap; document thresholds and user-visible strings

## 1. Implementation (apply stage, if approved)

- [ ] 1.1 Wire `BiometricState` to real biometric success/failure in `UnlockViewModel` and Autofill paths
- [ ] 1.2 Add tests for failure counts and lockout behavior

## 2. Validation

- [ ] 2.1 Manual biometric pass/fail sequences on supported devices
- [ ] 2.2 `./gradlew check` and targeted module tests
