# SecureVault — Software Testing Life Cycle (STLC)

> Revision: 1.0 | Date: 2026-03-21
> Author: Senior QA Engineer
> Classification: Security-Critical System
> Input Documents: SRS v1.0, SLTC v1.0

---

# 1. Requirement Analysis Phase

## 1.1 Phase Objective

Identify every testable behavior in the SecureVault system, classify it by risk, and confirm that no requirement is untestable or ambiguous.

## 1.2 Inputs

| Input | Source | Purpose |
|-------|--------|---------|
| SRS v1.0 | `docs/SRS.md` | System behavior, state model, security objectives, acceptance criteria |
| SLTC v1.0 | `docs/SLTC.md` | 165 system-level test cases across 11 modules |
| Source code | `shared/common/`, `composeApp/`, `androidApp/`, `desktopApp/` | Ground truth for behavior when SRS is ambiguous |
| Known Issues list | SRS §8.4 (KI-1 through KI-14) | Pre-existing defects that constrain test expectations |

## 1.3 Requirement Coverage List

Each SRS requirement is classified as **Testable**, **Partially Testable**, or **Not Testable** with justification.

### 1.3.1 Security Objectives (SO-1 through SO-8)

| Req ID | Requirement | Testable? | Test Method | Notes |
|--------|-------------|-----------|-------------|-------|
| SO-1 | Individual field encryption at rest | Yes | Unit: encrypt each field, read raw DB, verify no plaintext | Covered by REPO-002, FLOW-ADD-010 |
| SO-2 | DataKey never on disk in plaintext | Yes | Unit: inspect vault_config after setup. Integration: read DB after lock. | Covered by FLOW-SETUP-002, SEC-005 |
| SO-3 | Master password never stored | Yes | Unit: verify password CharArray wiped after derivation | Covered by KDF-006, KDF-007 |
| SO-4 | Ephemeral material wiped after use | Partially | Unit: verify ByteArray/CharArray zeroed. JVM GC may retain copies — not verifiable at test level. | Covered by SMM-004, SMM-005. JVM limitation documented. |
| SO-5 | Clipboard 30-second auto-clear | Yes | Integration: copy, wait 30s, read clipboard | Covered by CLIP-002. Timing-sensitive. |
| SO-6 | Screenshot protection default ON (Android) | Partially | Android instrumentation: verify FLAG_SECURE. Cannot programmatically verify screenshot output. | Covered by SS-001. Manual verification needed. |
| SO-7 | SecureMode passwords never displayed | Yes | UI test: assert no Text composable renders password string | Covered by FLOW-RET-007 |
| SO-8 | Session auto-lock after timeout | Partially | Unit: SessionManager background/foreground logic testable. Periodic checkAutoLock is NOT called in production — behavior gap. | Covered by SES-006..SES-011. Gap documented as KI-5. |

### 1.3.2 Core Functional Requirements

| Area | Testable Requirements | Total Test Cases (SLTC) |
|------|-----------------------|-------------------------|
| Key Derivation (Argon2id) | Correct derivation, determinism, password wipe, error on bad config | 10 |
| Authenticated Encryption | Roundtrip, nonce uniqueness, tamper detection, key validation | 18 |
| Session Management | Lock/unlock state, timeout logic, background/foreground, DataKey wipe | 15 |
| Platform Key Store | Store/retrieve/delete, platform-specific behavior | 8 |
| Security Mode | Encrypt/decrypt with SecureModeKey, usePassword wipe, toggle auth | 11 |
| Secure Clipboard | Copy, auto-clear, timer reset, platform stubs | 9 |
| Memory Sanitizer | Multi-pass wipe, SensitiveData lifecycle | 11 |
| Biometric Auth | Success/failure/cancel, platform stubs, rate-limiter (unwired) | 9 |
| Password Repository | CRUD, search, field encryption, security-mode routing | 18 |
| Screenshot Protection | FLAG_SECURE default, toggle persistence | 4 |
| Password Generator | Generate, copy, plaintext storage risk | 4 |

### 1.3.3 Untestable or Partially Testable Items

| Item | Reason | Mitigation |
|------|--------|------------|
| JVM GC retaining wiped arrays in freed heap | Cannot inspect freed memory from application code | Document as accepted risk. Static analysis can flag allocation patterns. |
| Immutable String persistence in string pool (KI-3) | Cannot wipe `String` objects on JVM | Document as known issue. Code review gate: no new `password.toString()` calls. |
| Desktop auto-lock (KI-6) | No lifecycle hooks exist in desktop `Main.kt` | Test as "expected failure" — verify it does NOT lock, file defect. |
| Autofill (not implemented) | UI placeholder only | Verify placeholder renders. Mark autofill as out-of-scope for functional testing. |
| iOS platform behavior | All implementations are stubs | Verify stubs return expected defaults (NotAvailable, no-op). No functional testing. |

## 1.4 Initial Risk List

Risks are ranked by `Impact × Likelihood` for a security-critical offline password manager.

| Risk ID | Risk | Impact | Likelihood | Severity | Affected Modules | Mitigation |
|---------|------|--------|------------|----------|-------------------|------------|
| R-01 | PRNG used instead of CSPRNG for all key/nonce generation (KI-1) | CRITICAL | Confirmed (present in code) | CRITICAL | All crypto | Block release until `CryptoUtils.generateSecureRandom` uses `SecureRandom` or libsodium random. Test: SEC-011. |
| R-02 | Desktop DataKey trivially recoverable from Java Preferences (KI-2) | HIGH | Confirmed | HIGH | PlatformKeyStore (Desktop) | Test: PKS-008, SEC-010. Document as known limitation or replace with OS keychain. |
| R-03 | No application-layer brute-force protection on unlock | HIGH | Medium (Argon2id provides computational resistance) | HIGH | KeyManager, UnlockViewModel | Test: SEC-001, FLOW-UNLOCK-003. Accept Argon2id as sole defense or implement BiometricState rate-limiter. |
| R-04 | Plaintext generated passwords in DB (KI-4) | HIGH | Confirmed | HIGH | Password Generator, Database | Test: GEN-003, SEC-009. Encrypt or remove generated_passwords table. |
| R-05 | Clipboard window (0–30s) exposes password to other apps | MEDIUM | High (any clipboard-reading app) | HIGH | SecureClipboard | Test: SEC-007. Accept as inherent platform limitation. Document in user-facing security policy. |
| R-06 | Foreground inactivity never triggers auto-lock (KI-5) | MEDIUM | Confirmed | MEDIUM | SessionManager | Test: SES-010, SES-011. Implement periodic checkAutoLock coroutine. |
| R-07 | Desktop has no lifecycle auto-lock (KI-6) | MEDIUM | Confirmed | MEDIUM | Desktop Main.kt | Test: XPLAT-004. Add window focus/blur listeners. |
| R-08 | DataKey copies not wiped by ViewModels (KI-8) | MEDIUM | Confirmed | MEDIUM | All ViewModels | Code review. Cannot test at system level (GC non-determinism). |
| R-09 | No data migration between storage format versions (NFR-DATA-4) | MEDIUM | Low (only one version exists) | LOW | EncryptedData | No test needed now. Add migration tests if v3 format is introduced. |
| R-10 | Argon2 config downgrade via DB tampering (KI-9) | LOW | Low (requires DB write access) | LOW | vault_config | Test scenario: modify Argon2 params in raw DB, verify unlock still requires correct password. |

## 1.5 Outputs

| Output | Status |
|--------|--------|
| Requirement Coverage List | §1.3 — all 11 modules mapped, 165 test cases linked |
| Initial Risk List | §1.4 — 10 risks identified, 3 CRITICAL/HIGH confirmed |
| Untestable Items | §1.3.3 — 5 items documented with mitigations |

---

# 2. Test Planning Phase

## 2.1 Phase Objective

Define the scope, strategy, resources, schedule, and risk mitigation approach for all testing activities.

## 2.2 Scope of Testing

### 2.2.1 In Scope

| Category | Scope |
|----------|-------|
| Platforms | Android (API 26+), Desktop (JVM 17+) |
| Modules | All 11 modules from SRS §4 |
| Test Types | Unit, Integration, UI, Security, Performance, Cross-platform |
| States | NOT_SETUP, LOCKED, UNLOCKED, UNLOCKED+SECURE_MODE |
| Core Flows | Vault Setup, Vault Unlock, Add Password, Retrieve Password, Delete Password, Password Change, Secure Mode Toggle |
| Security | Key lifecycle, encryption correctness, tamper detection, memory wipe, clipboard security, screenshot protection |

### 2.2.2 Out of Scope

| Item | Reason |
|------|--------|
| iOS functional testing | All iOS implementations are stubs. Only verify stubs return expected defaults. |
| Autofill feature | Not implemented. Verify placeholder screen renders; no functional tests. |
| Network security testing | System is offline-only. No network communication exists. |
| Third-party library internals | libsodium, SQLDelight, Koin are assumed correct. Test integration boundaries only. |
| UI pixel-perfect rendering | Compose Multiplatform visual consistency is out of scope. Focus on behavior. |

### 2.2.3 Assumptions

1. libsodium's XChaCha20-Poly1305 implementation is cryptographically correct.
2. Android KeyStore hardware backing is functional on test devices with TEE/StrongBox.
3. Test devices have biometric hardware enrolled for biometric tests.
4. SQLite database file is accessible for raw inspection in debug builds.

## 2.3 Testing Strategy

### 2.3.1 Test Levels

```
┌─────────────────────────────────────────────────────┐
│  Level 5: Security Penetration (Manual)             │
│  Target: Attack scenarios AC-28 through AC-34       │
├─────────────────────────────────────────────────────┤
│  Level 4: Cross-Platform (Automated + Manual)       │
│  Target: XPLAT-001 through XPLAT-005               │
├─────────────────────────────────────────────────────┤
│  Level 3: System/Integration (Automated)            │
│  Target: FLOW-*, STATE-*, SEC-*, EDGE-*             │
├─────────────────────────────────────────────────────┤
│  Level 2: Module Integration (Automated)            │
│  Target: KeyManager + SessionManager + Repository   │
├─────────────────────────────────────────────────────┤
│  Level 1: Unit (Automated)                          │
│  Target: KDF-*, ENC-*, SES-*, MEM-*, CLIP-*, etc.  │
└─────────────────────────────────────────────────────┘
```

### 2.3.2 Test Types and Techniques

| Test Type | Technique | Tools | Targets |
|-----------|-----------|-------|---------|
| **Unit** | Equivalence partitioning, boundary value analysis | kotlin.test, JUnit 5, kotlinx-coroutines-test | All 11 modules in `shared/common` |
| **Integration** | State transition testing, flow-based | Gradle test tasks (`shared:common:allTests`, `desktopApp:jvmTest`) | Key lifecycle (setup→unlock→lock→unlock), CRUD with encryption |
| **UI** | Compose UI testing, state-based assertions | Compose Test (AndroidComposeTestRule) | Secure Mode masking, button visibility, navigation enforcement |
| **Security — Cryptographic** | Known-answer tests, negative key tests, tamper injection | Custom test fixtures | ENC-009..ENC-015, SMM-009, REPO-014 |
| **Security — Memory** | Post-operation array inspection | Custom assertions (`isWiped()`) | KDF-006, KDF-007, MEM-001..MEM-011, SES-015 |
| **Security — Attack Simulation** | Manual + scripted | adb, memory profiler, DB browser | SEC-001 through SEC-015 |
| **Performance** | Load testing with N entries | Gradle + custom benchmark | Search decryption latency, Argon2 derivation time |
| **Cross-platform** | Feature parity matrix | Run same test suite on Android + Desktop | XPLAT-001..XPLAT-005 |

### 2.3.3 Security Testing Strategy (Per-Phase)

Security testing is not a phase — it runs in every phase:

| STLC Phase | Security Activities |
|------------|---------------------|
| Requirement Analysis | Identify KI-1 through KI-14 as security risks. Classify each by attack surface. |
| Test Planning | Define attack scenarios. Assign penetration tests. Plan memory inspection setup. |
| Test Case Design | Write tamper injection tests (ENC-010..012). Write brute-force simulation (SEC-001). Write clipboard timing tests (CLIP-002..006). Write DataKey wipe verification (SES-015). |
| Environment Setup | Configure debug builds with raw DB access. Install memory profiler. Set up mock biometric. Configure FLAG_SECURE verification. |
| Test Execution | Execute all SEC-* and KDF-006/007 tests. Run manual penetration tests. Verify PRNG weakness (SEC-011). Verify desktop key extraction (SEC-010). |
| Test Closure | Report CRITICAL security findings (R-01 PRNG, R-02 desktop key). Define security-specific exit criteria. No release if CRITICAL security tests fail. |

## 2.4 Resource Planning

### 2.4.1 Team Roles

| Role | Count | Responsibilities |
|------|-------|------------------|
| QA Lead / Security Tester | 1 | Test planning, security test design, penetration testing, risk tracking |
| Android QA Engineer | 1 | Android unit/integration/UI tests, device-specific biometric and KeyStore tests |
| Desktop QA Engineer | 1 | Desktop unit/integration tests, Java Preferences key extraction verification |
| Automation Engineer | 1 | CI pipeline, Gradle test configuration, Compose UI test framework |

### 2.4.2 Test Infrastructure

| Resource | Purpose | Configuration |
|----------|---------|---------------|
| Android physical device (Pixel 6+) | Biometric, KeyStore hardware-backed, FLAG_SECURE | API 33+, fingerprint enrolled, developer options ON |
| Android emulator (API 26) | Minimum SDK boundary testing | No biometric, software KeyStore |
| Desktop (JVM 17, Windows/macOS) | Desktop-specific tests | Java Preferences accessible, clipboard access |
| CI server | Automated test execution | `./gradlew shared:common:allTests`, `./gradlew desktopApp:jvmTest`, `./gradlew androidApp:testDebugUnitTest` |
| SQLite DB browser | Raw database inspection for security tests | Access to debug-build database files |
| Memory profiler (Android Studio Profiler / VisualVM) | Memory wipe verification for attack simulation | Heap dump capability |

## 2.5 Effort Estimation

| Activity | Test Cases | Estimated Effort | Priority |
|----------|-----------|------------------|----------|
| Level 1: Unit tests (all modules) | 89 | 8 person-days | P0 |
| Level 2: Module integration tests | 16 | 4 person-days | P0 |
| Level 3: System/flow tests | 30 | 5 person-days | P0 |
| Level 4: Cross-platform parity | 5 | 2 person-days | P1 |
| Level 5: Security penetration | 15 | 5 person-days | P0 |
| UI tests (Compose) | 10 | 3 person-days | P1 |
| Performance benchmarking | 5 | 2 person-days | P2 |
| Environment setup & CI | — | 3 person-days | P0 |
| **Total** | **165+** | **32 person-days** | — |

## 2.6 Risk Mitigation Plan

| Risk ID | Mitigation Action | Owner | Deadline |
|---------|-------------------|-------|----------|
| R-01 (PRNG) | Replace `kotlin.random.Random.Default` with `LibsodiumRandom` or platform `SecureRandom`. Verify via SEC-011. | Dev Lead | Before any release |
| R-02 (Desktop key) | Document as known limitation. Add warning in desktop security settings. Verify via PKS-008. | Product Owner | Before desktop release |
| R-03 (No brute-force lock) | Wire `BiometricState` rate-limiter into `UnlockViewModel`. Verify via SEC-001. | Dev | Sprint N+1 |
| R-04 (Plaintext gen passwords) | Encrypt `generated_passwords` table or remove history feature. Verify via GEN-003. | Dev | Sprint N+1 |
| R-06 (No periodic auto-lock) | Add coroutine timer calling `checkAutoLock()` every 30 seconds. Verify via SES-010, SES-011. | Dev | Sprint N+1 |
| R-07 (Desktop no lifecycle) | Add `WindowFocusListener` to desktop `Main.kt`. Verify via XPLAT-004. | Dev | Sprint N+2 |

## 2.7 Outputs

| Output | Status |
|--------|--------|
| Test Plan | This document (§2) |
| Scope definition | §2.2 |
| Strategy | §2.3 |
| Resource plan | §2.4 |
| Effort estimation | §2.5 |
| Risk mitigation plan | §2.6 |

---

# 3. Test Case Design Phase

## 3.1 Phase Objective

Design deterministic, executable test cases for every testable requirement, including test data, with explicit traceability to SRS requirements.

## 3.2 Test Design Techniques

| Technique | Where Applied | Examples |
|-----------|---------------|----------|
| **Equivalence Partitioning** | Input validation across modules | Valid key (32 bytes) vs. short key (16 bytes) vs. long key (64 bytes). Valid password vs. empty vs. Unicode. |
| **Boundary Value Analysis** | Timeout logic, array sizes, Argon2 config minimums | Timeout exactly at boundary (EDGE-001). Argon2 minimum config (KDF-010). Max 10 MB plaintext (ENC-016). |
| **State Transition Testing** | Session state machine, Secure Mode transitions | All 9 transitions from SRS §2.2.1. Secure Mode SM-1 and SM-2. STATE-001 through STATE-011. |
| **Decision Table Testing** | Background/foreground lock logic, Secure Mode auth routing | Timeout = -1 / 0 / positive. Biometric available/unavailable + securityMode on/off. |
| **Negative Testing** | Every error path from SRS §3 | Wrong key decryption (ENC-009). Wrong password unlock (FLOW-UNLOCK-002). Tampered ciphertext (ENC-010..012). |
| **Security-Specific: Tamper Injection** | Encryption module | Flip bit in ciphertext, tag, IV. Swap ciphertext between entries. Modify Argon2 params in DB. |
| **Security-Specific: Privilege Escalation** | State enforcement | Attempt vault operations in LOCKED state. Attempt edit/delete without re-auth in Secure Mode. |

## 3.3 Test Data Strategy

### 3.3.1 Static Test Data (Deterministic, Reusable)

| Data Set | Contents | Used By |
|----------|----------|---------|
| `TD-PASSWORD-SET` | 5 passwords: "simple", "P@ss123!#$", "中文密码测试", "a" (min), 10,000-char string (max) | KDF-*, FLOW-ADD-*, FLOW-UNLOCK-* |
| `TD-KEY-SET` | 3 keys: valid 32-byte, short 16-byte, long 64-byte | ENC-006, ENC-007, ENC-009 |
| `TD-SALT-SET` | 2 salts: 16-byte salt A, 16-byte salt B (distinct) | KDF-003, KDF-004 |
| `TD-ENTRY-SET` | 10 PasswordEntry objects: mix of categories, favorites, securityMode flags, nullable URL/notes | REPO-*, FLOW-ADD-*, FLOW-RET-*, FLOW-DEL-* |
| `TD-ARGON2-CONFIG-SET` | 3 configs: default (128MB/3/4/32), minimum (8MB/1/1/32), custom (64MB/2/2/32) | KDF-001, KDF-010 |
| `TD-ENCRYPTED-DATA-SET` | Pre-encrypted blobs for tamper injection: valid, bit-flipped ciphertext, bit-flipped tag, bit-flipped IV, wrong version | ENC-010..ENC-015, ENC-017, ENC-018 |

### 3.3.2 Dynamic Test Data (Generated at Runtime)

| Data | Generated How | Used By |
|------|---------------|---------|
| Random nonces | By encryption module (24-byte random per call) | Verified for uniqueness in ENC-002 |
| Argon2 salt | By `argon2Kdf.generateSalt()` (16 bytes) | FLOW-SETUP-001, FLOW-PW-001 |
| DataKey | By `CryptoUtils.generateSecureRandom(32)` | FLOW-SETUP-001 |
| SecureModeKey | By `CryptoUtils.generateSecureRandom(32)` | SMM-002 |
| Timestamps | `System.currentTimeMillis()` | Entry creation, session activity tracking |

### 3.3.3 Security Test Data

| Data | Purpose | Used By |
|------|---------|---------|
| 100 incorrect passwords | Brute-force simulation | SEC-001 |
| Tampered EncryptedData (5 variants) | Authentication tag verification | ENC-010..ENC-012 |
| Raw vault_config dump | DataKey extraction attempt | SEC-002, SEC-010 |
| Raw generated_passwords dump | Plaintext exposure verification | SEC-009 |
| Modified Argon2 params in DB | Config downgrade attack | R-10 scenario |

## 3.4 Coverage Criteria

### 3.4.1 Minimum Coverage Requirements

| Criterion | Target | Measurement |
|-----------|--------|-------------|
| Requirement coverage | 100% of testable SRS requirements have >= 1 test case | Traceability matrix in SLTC |
| State transition coverage | 100% of 9 session transitions + 2 Secure Mode transitions | STATE-* tests |
| Core flow coverage | >= 5 test cases per core flow (setup, unlock, add, retrieve, delete, password change) | FLOW-* tests: 8+8+10+10+10+8 = 54 |
| Error path coverage | >= 1 test per error condition in SRS §3 | E-ADD-1..4, E-RET-1..4, E-DEL-1..6 mapped |
| Security scenario coverage | 100% of AC-28 through AC-34 attack scenarios | SEC-001..SEC-015 |
| Module function coverage | >= 3 tests per public function | SLTC verified |
| Platform coverage | Android + Desktop for every platform-dependent module | XPLAT-* tests |

### 3.4.2 Security-Specific Coverage Requirements

| Criterion | Target |
|-----------|--------|
| Tamper detection | Test for ciphertext, tag, and IV tampering (3 separate tests) |
| Wrong-key decryption | Test for every key type: DataKey, SecureModeKey, PasswordKey |
| Memory wipe verification | Test for every ephemeral key type: PasswordKey, DataKey copy, SecureModeKey, password CharArray |
| Clipboard timing | Test at <30s (still present), =30s (cleared), and timer reset |
| State enforcement | Test every forbidden operation per state (LOCKED state blocking vault operations, Secure Mode blocking visibility) |
| Platform weakness | Test desktop key extraction (PKS-008, SEC-010) |

## 3.5 Test Case Artifacts

| Artifact | Location | Count |
|----------|----------|-------|
| System-Level Test Cases | `docs/SLTC.md` | 165 test cases |
| Test Data Definitions | §3.3 above | 6 static sets, 5 dynamic generators, 5 security sets |
| Traceability Matrix | `docs/SLTC.md` (Traceability Matrix section) | 17 SRS-to-test mappings |

## 3.6 Outputs

| Output | Status |
|--------|--------|
| Test cases | 165 in `docs/SLTC.md` |
| Test data | Defined in §3.3 |
| Coverage criteria | Defined in §3.4 |
| Traceability matrix | In SLTC document |

---

# 4. Test Environment Setup Phase

## 4.1 Phase Objective

Establish all required platforms, tools, configurations, and offline conditions for executing the full test suite.

## 4.2 Platform Environments

### 4.2.1 Android Environment

| Component | Specification |
|-----------|---------------|
| **Physical device** | Google Pixel 6 or newer, Android 13+ (API 33+), fingerprint enrolled |
| **Emulator (minimum SDK)** | API 26 (Android 8.0), x86_64, no biometric, 4 GB RAM |
| **Emulator (target SDK)** | API 34, x86_64, fingerprint enrolled via `adb emu finger touch` |
| **Build variant** | `debug` (for raw DB access and profiler attachment) |
| **Build command** | `./gradlew androidApp:assembleDebug` |
| **Test command** | `./gradlew androidApp:testDebugUnitTest` |
| **Instrumentation test command** | `./gradlew androidApp:connectedDebugAndroidTest` |
| **DB inspection** | `adb pull /data/data/com.securevault/databases/` → open with DB Browser for SQLite |
| **Memory profiler** | Android Studio Profiler → heap dump during UNLOCKED state |
| **Clipboard verification** | `adb shell cmd clipboard get` (API 33+) or clipboard manager app |

### 4.2.2 Desktop (JVM) Environment

| Component | Specification |
|-----------|---------------|
| **OS** | Windows 10/11 or macOS 12+ |
| **JVM** | OpenJDK 17+ |
| **Build command** | `./gradlew desktopApp:run` (launch) |
| **Test command** | `./gradlew desktopApp:jvmTest` |
| **Common module tests** | `./gradlew shared:common:desktopTest` |
| **DB inspection** | SQLite file in app data directory |
| **Java Preferences inspection** | Windows: `regedit` → `HKEY_CURRENT_USER\Software\JavaSoft\Prefs`; macOS: `~/Library/Preferences/com.apple.java.util.prefs.plist` |
| **Memory profiler** | VisualVM or IntelliJ Profiler → heap dump |
| **Clipboard verification** | Programmatic: `Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor)` |

### 4.2.3 iOS Environment

| Component | Specification |
|-----------|---------------|
| Status | **Stub only.** Not included in `settings.gradle.kts`. |
| Testing | Verify stub classes return expected defaults (`NotAvailable`, `false`, no-op). Run via `shared:common:iosTest` if target is enabled. |

### 4.2.4 CI Environment

| Component | Specification |
|-----------|---------------|
| CI platform | GitHub Actions or local Gradle |
| Unit test job | `./gradlew shared:common:allTests` — runs commonTest, androidUnitTest, desktopTest |
| Desktop test job | `./gradlew desktopApp:jvmTest` |
| Android unit test job | `./gradlew androidApp:testDebugUnitTest` |
| Static analysis | `./gradlew detekt` |
| Build verification | `./gradlew clean build` |
| Trigger | On every push and pull request |

## 4.3 Offline Conditions

SecureVault is offline-first. Testing must verify that **no network access is required or attempted**.

| Condition | How to Enforce | Verification |
|-----------|----------------|--------------|
| Android airplane mode | Enable airplane mode on device/emulator before test suite | All tests pass without network |
| Desktop no internet | Disable network adapter or use firewall to block all outbound | All tests pass. No DNS lookups, no HTTP calls. |
| Verify no network calls | Run with HTTP proxy (e.g., mitmproxy) configured as system proxy | Proxy log shows zero requests from app |

## 4.4 Security-Related Environment Setup

| Setup | Purpose | How |
|-------|---------|-----|
| Raw DB access (debug build) | Read encrypted columns, verify no plaintext leakage | Debug build does not encrypt SQLite file. Pull via `adb` or file manager. |
| Java Preferences access (Desktop) | Verify XOR key extraction attack (SEC-010) | Read via regedit (Windows) or plist (macOS) |
| Heap dump capability | Verify memory wipe (SEC-005, AC-30) | Android Studio Profiler or VisualVM. Capture heap dump while UNLOCKED, search for DataKey bytes. |
| Mock biometric | Test biometric flows in emulator | `adb emu finger touch 1` to simulate fingerprint success; `adb emu finger touch 99999` for failure |
| Tampered DB fixture | Test tamper detection and config downgrade | Pre-modified SQLite file with bit-flipped ciphertext, altered Argon2 params |
| Clipboard monitor | Verify 30-second auto-clear timing | Background process that polls clipboard every second and logs contents with timestamps |

## 4.5 Environment Readiness Checklist

Every item must be verified GREEN before test execution begins.

| # | Check | Android Physical | Android Emulator (API 26) | Android Emulator (API 34) | Desktop (Windows) | Desktop (macOS) |
|---|-------|:---:|:---:|:---:|:---:|:---:|
| 1 | Device/machine available and powered on | ☐ | ☐ | ☐ | ☐ | ☐ |
| 2 | Correct OS/API version confirmed | ☐ | ☐ | ☐ | ☐ | ☐ |
| 3 | JDK 17+ installed | ☐ | ☐ | ☐ | ☐ | ☐ |
| 4 | `./gradlew clean build` succeeds | ☐ | ☐ | ☐ | ☐ | ☐ |
| 5 | Debug APK installs and launches | ☐ | ☐ | ☐ | N/A | N/A |
| 6 | Desktop app launches | N/A | N/A | N/A | ☐ | ☐ |
| 7 | Biometric enrolled (where applicable) | ☐ | N/A | ☐ | N/A | N/A |
| 8 | Raw DB accessible | ☐ | ☐ | ☐ | ☐ | ☐ |
| 9 | Java Preferences accessible | N/A | N/A | N/A | ☐ | ☐ |
| 10 | Memory profiler attached | ☐ | N/A | ☐ | ☐ | ☐ |
| 11 | Clipboard monitoring active | ☐ | ☐ | ☐ | ☐ | ☐ |
| 12 | Network disabled (offline verification) | ☐ | ☐ | ☐ | ☐ | ☐ |
| 13 | Tampered DB fixture prepared | ☐ | ☐ | ☐ | ☐ | ☐ |
| 14 | CI pipeline green (unit tests passing) | ☐ | ☐ | ☐ | ☐ | ☐ |
| 15 | Fresh app install (no prior data) | ☐ | ☐ | ☐ | ☐ | ☐ |

## 4.6 Outputs

| Output | Status |
|--------|--------|
| Environment readiness checklist | §4.5 — 15 checks across 5 platforms |
| Platform specifications | §4.2 |
| Offline verification procedure | §4.3 |
| Security environment setup | §4.4 |

---

# 5. Test Execution Phase

## 5.1 Phase Objective

Execute all 165 test cases in priority order, record results, report defects, and manage regression.

## 5.2 Execution Process

### 5.2.1 Execution Sequence

Tests run in strict dependency order. No level starts until the previous level achieves its pass threshold.

| Wave | Level | Test IDs | Pass Threshold | Automated? |
|------|-------|----------|----------------|------------|
| **Wave 1** | Unit (L1) | KDF-*, ENC-*, SES-*, MEM-*, PKS-*, SMM-*, CLIP-*, BIO-*, REPO-*, SS-*, GEN-* | 100% CRITICAL, 95% HIGH | Yes (Gradle) |
| **Wave 2** | Integration (L2) | FLOW-SETUP-*, FLOW-UNLOCK-*, FLOW-PW-* | 100% CRITICAL, 90% HIGH | Yes (Gradle) |
| **Wave 3** | System/Flow (L3) | FLOW-ADD-*, FLOW-RET-*, FLOW-DEL-*, STATE-*, EDGE-* | 100% CRITICAL, 90% overall | Mostly (some manual UI) |
| **Wave 4** | Security (L5) | SEC-001 through SEC-015 | 100% CRITICAL | Mixed (automated + manual) |
| **Wave 5** | Cross-platform (L4) | XPLAT-001 through XPLAT-005 | 100% | Automated |
| **Wave 6** | Performance | Benchmark tests | Within defined thresholds | Automated |

### 5.2.2 Daily Execution Rhythm

```
09:00  CI runs Wave 1 (unit) + Wave 2 (integration) automatically
10:00  Review CI results. Triage failures.
10:30  Wave 3 execution (system tests — mix of automated and manual)
13:00  Wave 4 execution (security — manual penetration + automated)
15:00  Wave 5 + 6 (cross-platform, performance)
16:00  Defect triage meeting (15 min)
16:30  Update test execution report
```

### 5.2.3 Test Execution Rules

| Rule | Detail |
|------|--------|
| Fresh database per test run | Delete app data before each system-level test execution. Each test must start from a known state. |
| No test interdependence | Each test case must be self-contained. Setup prerequisites in preconditions, not in a previous test. |
| Security tests on real devices | SEC-* tests involving biometric, KeyStore, FLAG_SECURE must run on physical Android devices. |
| Timing tests with tolerance | Clipboard auto-clear tests (CLIP-002, CLIP-005) use ±2 second tolerance around 30-second boundary. |
| Failed tests re-run once | A failed test is re-executed once before recording as FAIL. If it fails both times: defect. |

## 5.3 Defect Reporting

### 5.3.1 Defect Severity Classification

| Severity | Definition for SecureVault | Examples |
|----------|---------------------------|----------|
| **S1 — Blocker** | Security vulnerability that exposes credentials. Data loss. Crash on critical path. | DataKey leaked to disk. Encryption returns plaintext. App crash on unlock. |
| **S2 — Critical** | Security control bypassed. Core flow fails. Data corruption. | Clipboard not cleared after 30s. Tampered ciphertext accepted. SecureMode password displayed. |
| **S3 — Major** | Non-security feature failure. Incorrect error message. State transition defect. | Wrong error message on bad password. Session not locked after timeout. |
| **S4 — Minor** | UI cosmetic issue. Non-critical edge case. | Snackbar text truncated. Category default value inconsistent. |

### 5.3.2 Defect Report Template

Every defect must include:

| Field | Required |
|-------|----------|
| Defect ID | DEF-{NNN} |
| Title | One-sentence description |
| Test Case ID | Links to SLTC test ID(s) |
| Severity | S1 / S2 / S3 / S4 |
| Platform | Android Physical / Android Emulator / Desktop Windows / Desktop macOS |
| State | System state when defect occurred |
| Preconditions | Exact setup steps |
| Steps to Reproduce | Numbered, deterministic steps |
| Expected Result | From SLTC |
| Actual Result | What actually happened (include screenshots, logs, heap dumps for security issues) |
| Attachments | DB dump, heap dump, logcat, clipboard log (as applicable) |

### 5.3.3 Security Defect Handling

| Rule | Detail |
|------|--------|
| S1 security defects halt execution | All testing stops. Dev team notified immediately. Fix verified within 24 hours. |
| Security defects are confidential | Not disclosed in public issue trackers until fixed. |
| S1/S2 security defects require fix + regression test | New test case added for any security defect to prevent regression. |
| Known issues (KI-*) are not new defects | Pre-existing known issues are tracked separately. Test results for KI-* items are recorded as "Known Issue — Expected Behavior." |

## 5.4 Regression Strategy

### 5.4.1 When Regression Runs

| Trigger | Regression Scope |
|---------|-----------------|
| Any defect fix merged | Wave 1 (full unit) + affected module's integration tests + the specific failed test |
| Crypto module change | Full Wave 1 + Wave 2 + Wave 4 (all security tests) |
| Session/KeyManager change | Wave 1 (SES-*, KDF-*) + Wave 2 (FLOW-SETUP-*, FLOW-UNLOCK-*) + STATE-* |
| UI change | Affected FLOW-* tests + FLOW-RET-005/007 (Secure Mode masking) |
| Pre-release | Full regression: all 165 test cases on all platforms |

### 5.4.2 Regression Test Set (Smoke)

A 25-test subset for quick regression (< 30 minutes):

| Priority | Test IDs | Coverage |
|----------|----------|----------|
| Crypto core | ENC-001, ENC-009, ENC-010 | Roundtrip, wrong key, tamper |
| Key lifecycle | FLOW-SETUP-001, FLOW-UNLOCK-001, FLOW-UNLOCK-002 | Setup, correct unlock, wrong unlock |
| CRUD | FLOW-ADD-001, FLOW-RET-001, FLOW-DEL-001 | Create, read, delete |
| Session | SES-001, SES-002, SES-006 | Unlock, lock, background immediate |
| Memory | MEM-001, MEM-009, SES-015 | Wipe, SensitiveData, DataKey wipe on lock |
| Security Mode | SMM-002, SMM-006, FLOW-RET-005 | Encrypt, usePassword, UI masking |
| Clipboard | CLIP-001, CLIP-002 | Copy, auto-clear |
| Password change | FLOW-PW-001, FLOW-PW-003 | Change, old password fails |
| State enforcement | STATE-003, STATE-004 | Lock forces login, operations blocked |
| Platform | XPLAT-001 | Cross-platform encryption compatibility |
| Attack | SEC-001, SEC-002 | Brute-force, wrong-key DB attack |

## 5.5 Test Execution Report Template

Generated after each execution cycle:

```
═══════════════════════════════════════════════
  SecureVault Test Execution Report
  Date: YYYY-MM-DD | Cycle: N
  Build: <commit hash> | Platform: <platform>
═══════════════════════════════════════════════

Summary:
  Total Executed:   ___ / 165
  Passed:           ___
  Failed:           ___
  Blocked:          ___
  Known Issue:      ___
  Not Executed:     ___

By Priority:
  CRITICAL tests:   ___ / ___ passed
  HIGH tests:       ___ / ___ passed
  MEDIUM tests:     ___ / ___ passed
  LOW tests:        ___ / ___ passed

By Module:
  Argon2id KDF:          ___/10
  XChaCha20-Poly1305:    ___/18
  Session Manager:       ___/15
  Platform Key Store:    ___/8
  Security Mode Manager: ___/11
  Secure Clipboard:      ___/9
  Memory Sanitizer:      ___/11
  Biometric Auth:        ___/9
  Password Repository:   ___/18
  Screenshot Protection: ___/4
  Password Generator:    ___/4
  Core Flows:            ___/54
  State Tests:           ___/11
  Security Scenarios:    ___/15
  Cross-Platform:        ___/5
  Edge Cases:            ___/12

New Defects Filed: ___
  S1 (Blocker):    ___
  S2 (Critical):   ___
  S3 (Major):      ___
  S4 (Minor):      ___

Open Defects (cumulative): ___
  S1: ___  S2: ___  S3: ___  S4: ___

Blocked Tests (with reason): [list]
Security Test Results: [pass/fail for SEC-001..SEC-015]
═══════════════════════════════════════════════
```

## 5.6 Outputs

| Output | Frequency |
|--------|-----------|
| Test execution report | After each cycle (§5.5 template) |
| Defect log | Continuous (§5.3.2 template) |
| Regression results | After each fix merge |

---

# 6. Test Closure Phase

## 6.1 Phase Objective

Determine if testing is complete, assess residual risk, report metrics, and document lessons learned.

## 6.2 Exit Criteria

### 6.2.1 Mandatory Exit Criteria (All Must Be Met)

| # | Criterion | Measurement |
|---|-----------|-------------|
| EC-1 | All 165 SLTC test cases executed on Android | Execution report shows 0 "Not Executed" |
| EC-2 | All 165 SLTC test cases executed on Desktop | Same as EC-1 for Desktop |
| EC-3 | 100% of CRITICAL security tests pass | SEC-002 through SEC-006, SEC-013, SEC-015, ENC-009..ENC-012, KDF-006, KDF-007, SES-015, SMM-004..SMM-007 = all PASS |
| EC-4 | Zero open S1 (Blocker) defects | Defect log confirms |
| EC-5 | Zero open S2 (Critical) defects in security modules | Crypto, KeyManager, SessionManager, SecurityModeManager, SecureClipboard |
| EC-6 | All known issues (KI-*) documented with acceptance status | Product Owner signs off on each KI |
| EC-7 | Regression suite (25 tests) passes on final build | Smoke regression green |
| EC-8 | R-01 (PRNG) either fixed or explicitly accepted by security reviewer | Risk acceptance form signed or code change verified |

### 6.2.2 Conditional Exit Criteria (Exceptions Require Sign-off)

| # | Criterion | Acceptable Exception |
|---|-----------|---------------------|
| EC-9 | All HIGH tests pass (>= 95%) | Up to 5% may fail with documented risk acceptance |
| EC-10 | All MEDIUM tests pass (>= 90%) | Up to 10% may fail with documented workarounds |
| EC-11 | Performance benchmarks within thresholds | Argon2 > 5s on low-end device is acceptable with documented UX mitigation |

## 6.3 Metrics

### 6.3.1 Test Effectiveness Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Test Case Pass Rate | (Passed / Executed) × 100 | >= 95% overall, 100% CRITICAL |
| Defect Detection Rate | (Defects found by QA / Total defects) × 100 | >= 80% |
| Security Test Coverage | (Security tests passed / Total security tests) × 100 | 100% |
| Requirement Coverage | (Requirements with >=1 test / Total testable requirements) × 100 | 100% |
| State Transition Coverage | (Transitions tested / Total transitions) × 100 | 100% (11/11) |

### 6.3.2 Defect Metrics

| Metric | Formula | Notes |
|--------|---------|-------|
| Defect Density | Defects / Module | Identify highest-defect modules |
| S1+S2 Density | Critical defects / Module | Security focus |
| Defect Resolution Rate | Fixed defects / Total defects | Target 100% for S1+S2 |
| Defect Leakage | Post-release defects / Total defects | Target < 5% |
| Mean Time to Fix (S1) | Avg hours from S1 report to verified fix | Target < 24 hours |

### 6.3.3 Platform Parity Metrics

| Metric | How Measured |
|--------|-------------|
| Feature parity score | Count of features behaving identically on Android vs Desktop / Total features | Target: documented divergences only |
| Platform-specific defects | Count of defects affecting only one platform | Track trend |

### 6.3.4 Security-Specific Metrics

| Metric | Target |
|--------|--------|
| Known security issues accepted without fix | Each must have Product Owner + Security Reviewer sign-off |
| Attack scenarios tested | 7/7 (AC-28 through AC-34) |
| Memory wipe verification pass rate | 100% of MEM-*, KDF-006/007, SES-015, SMM-004/005/007 |
| Clipboard clear timing accuracy | 100% of CLIP-002, CLIP-004, CLIP-005 within ±2s tolerance |

## 6.4 Test Summary Report Template

```
═══════════════════════════════════════════════════════
  SecureVault — Test Summary Report
  Version: <app version>
  Build: <commit hash>
  Date Range: <start> to <end>
  QA Lead: _______________
═══════════════════════════════════════════════════════

1. SCOPE
   Modules tested: 11/11
   Platforms tested: Android, Desktop
   Test types: Unit, Integration, UI, Security, Performance, Cross-platform

2. EXECUTION SUMMARY
   Total test cases: 165
   Executed: ___
   Passed: ___  (___%)
   Failed: ___  (___%)
   Blocked: ___
   Known Issue: ___

3. DEFECT SUMMARY
   Total defects found: ___
   S1 (Blocker): ___ (Open: ___, Fixed: ___)
   S2 (Critical): ___ (Open: ___, Fixed: ___)
   S3 (Major): ___ (Open: ___, Fixed: ___)
   S4 (Minor): ___ (Open: ___, Fixed: ___)
   Security defects: ___ (all S1/S2 fixed: Yes/No)

4. EXIT CRITERIA STATUS
   EC-1 (Android execution):       ☐ MET / ☐ NOT MET
   EC-2 (Desktop execution):       ☐ MET / ☐ NOT MET
   EC-3 (CRITICAL security):       ☐ MET / ☐ NOT MET
   EC-4 (Zero S1):                 ☐ MET / ☐ NOT MET
   EC-5 (Zero S2 in security):     ☐ MET / ☐ NOT MET
   EC-6 (KI documented):           ☐ MET / ☐ NOT MET
   EC-7 (Regression green):        ☐ MET / ☐ NOT MET
   EC-8 (R-01 PRNG resolved):      ☐ MET / ☐ NOT MET

5. RESIDUAL RISKS
   [List of accepted KI-* items with risk owner sign-off]

6. RECOMMENDATION
   ☐ RELEASE APPROVED (all exit criteria met)
   ☐ CONDITIONAL RELEASE (exceptions documented below)
   ☐ RELEASE NOT RECOMMENDED (open S1/S2 defects)

   Exceptions: ___
   Conditions: ___

═══════════════════════════════════════════════════════
  Sign-off:
  QA Lead: _______________ Date: ___
  Dev Lead: ______________ Date: ___
  Security Reviewer: _____ Date: ___
  Product Owner: _________ Date: ___
═══════════════════════════════════════════════════════
```

## 6.5 Lessons Learned

Captured at the end of each release cycle. Template:

### 6.5.1 What Worked

| # | Item | Impact |
|---|------|--------|
| 1 | Early identification of PRNG issue (KI-1) via security test design | Prevented shipping with critical vulnerability |
| 2 | Tamper injection test cases caught encryption edge cases early | Reduced S1 defect leakage |
| 3 | State transition test matrix caught navigation enforcement gaps | Prevented lock-screen bypass |

### 6.5.2 What Needs Improvement

| # | Item | Action |
|---|------|--------|
| 1 | Desktop has no lifecycle hooks — could not test auto-lock | File feature request for desktop lifecycle integration |
| 2 | Clipboard timing tests are flaky (±2s tolerance not always sufficient) | Investigate deterministic clipboard testing approach |
| 3 | Memory wipe verification is best-effort on JVM | Evaluate native memory inspection tools for more reliable verification |
| 4 | BiometricState rate-limiter untested in production path | Add integration test once wired in |

### 6.5.3 Security-Specific Lessons

| # | Lesson | Recommendation |
|---|--------|----------------|
| 1 | Class names misleading (`AesGcmCipher` uses XChaCha20-Poly1305) | Add security naming review to code review checklist |
| 2 | PRNG weakness was visible in code but not caught before MVP | Add static analysis rule: flag `kotlin.random.Random.Default` in crypto modules |
| 3 | Desktop key storage design was accepted too early | Require security architecture review for all platform `actual` implementations |
| 4 | Generated passwords stored in plaintext | Require encrypted-at-rest review for all SQL tables |

## 6.6 Outputs

| Output | Status |
|--------|--------|
| Test Summary Report | §6.4 template, filled after final execution cycle |
| Exit Criteria Assessment | §6.2, each criterion checked |
| Metrics Dashboard | §6.3, all metrics computed |
| Lessons Learned | §6.5, captured and actionable |
| Release Recommendation | In Test Summary Report, with sign-off block |
