# Project Context

## Purpose

SecureVault is an **offline-first** Kotlin Multiplatform password manager. Users store and retrieve secrets locally with strong cryptography; network sync is not assumed. **Current product scope: Android + Windows Desktop** (see `docs/PLATFORM-SCOPE.md`). iOS and other desktop targets exist in-repo but are not in the active build matrix where noted in workspace docs.

## Tech Stack

- **Language & build:** Kotlin 2.3.20, Gradle 9.3.x, Android Gradle Plugin 9.0.x
- **UI:** Compose Multiplatform 1.10.x
- **Data:** SQLDelight 2.2.1 (`generateAsync = true`)
- **DI & async:** Koin, Kotlin Coroutines (structured concurrency; avoid `GlobalScope`)
- **Crypto:** libsodium (XChaCha20-Poly1305 / SecretBox), argon2kt
- **Quality:** detekt; tests via Gradle targets listed below

## Project Conventions

### Code Style

- **Indentation:** 4 spaces
- **Style:** Match surrounding code; avoid unrelated refactors in change sets
- **KMP:** `expect` / `actual` pairs live in the **same** KMP module in the correct source sets (e.g. `commonMain` + `androidMain`), not split across sibling modules in a way that breaks expect/actual linkage

### Architecture Patterns

- **Layers:** `UI (Compose) → ViewModel → UseCase/Repository → DataSource`
- **Errors:** Prefer `Result` and sealed error types over ad-hoc exceptions at boundaries
- **Module roles:**
  - `shared/common` — cross-platform domain, crypto, session, repositories, models
  - `shared/android`, `shared/ios`, `shared/desktop` — platform implementations and bridges
  - `composeApp` — shared UI, ViewModels, navigation, DI
  - `androidApp`, `desktopApp` — app entry and shell
- **Note:** `iosApp` may exist but is not included in `settings.gradle.kts`; do not rely on `iosApp:*` Gradle tasks in this workspace unless that changes

### Testing Strategy

- Prefer tests that lock behavior at the right layer (use cases, repositories, crypto helpers)
- **Useful Gradle tasks (verify names in repo before citing):** `./gradlew shared:common:allTests`, `shared:common:desktopTest`, `desktopApp:jvmTest`, `androidApp:testDebugUnitTest`, `./gradlew check`, `./gradlew detekt`
- See `docs/design/TESTING-AND-PERFORMANCE.md` for project testing guidance

### Git Workflow

- Use focused branches and commits; keep OpenSpec **change proposals** (`openspec/changes/<change-id>/`) in sync with implementation status
- After a change is deployed and reviewed, archive per `openspec/AGENTS.md` (move to `changes/archive/…`, update specs as needed)

## Domain Context

- **Threat model baseline:** Local vault confidentiality and integrity; secrets must not appear in logs (passwords, keys, recovery phrases)
- **Crypto (as implemented):** XChaCha20-Poly1305 via libsodium SecretBox — **not** AES-GCM
- **Implementation detail:** `EncryptedData` ↔ SecretBox requires correct handling of `tag + ciphertext` layout
- **Sensitive buffers:** Clear `ByteArray` / `CharArray` after use where feasible
- **Android release:** R8 issues with JNA / kotlinx.serialization may require ProGuard rules — see `docs/reference/SECURITY-ARCHITECTURE.md` (e.g. section on JNA / serialization)

## Important Constraints

- Do not log or persist secrets in plaintext outside the vault’s cryptographic model
- Platform scope and module boundaries in workspace `AGENTS.md` are authoritative for day-to-day work
- Prefer verifying commands, module names, and crypto details against **source** and `docs/reference/` rather than stale assumptions

## External Dependencies

- **Build & tooling:** Gradle, AGP, Kotlin, detekt
- **Crypto:** libsodium (via project integration), argon2kt
- **No required cloud service** for core vault operation (offline-first); document any new external service explicitly in proposals and security docs
