# SecureVault — Software Requirements Specification (SRS)

> Reverse-engineered from implemented MVP.
> Revision: 1.0 | Date: 2026-03-21
> Roles: Security Architect · Senior Product Manager · System Test Lead

---

## Table of Contents

- [1. System Overview](#1-system-overview)
- [2. System State Model](#2-system-state-model)
- [3. Core Business Flows](#3-core-business-flows)
- [4. Functional Requirements](#4-functional-requirements)
- [5. Security Requirements](#5-security-requirements)
- [6. Non-functional Requirements](#6-non-functional-requirements)
- [7. Acceptance Criteria](#7-acceptance-criteria)
- [8. Test Coverage Summary](#8-test-coverage-summary)

---

# 1. System Overview

## 1.1 System Goals

SecureVault is an offline-first, cross-platform password manager built with Kotlin Multiplatform. It targets Android, Desktop (JVM), and reserves an iOS stub for future activation. The system stores user credentials in an encrypted local database. No network communication occurs at any time.

**Primary goals:**

| # | Goal |
|---|------|
| G-1 | Allow users to store, retrieve, organize, and delete credential entries |
| G-2 | Encrypt all sensitive fields at rest using authenticated encryption |
| G-3 | Protect the encryption key hierarchy behind a user-chosen master password |
| G-4 | Provide biometric unlock as an alternative to the master password |
| G-5 | Offer a Secure Mode that prevents plaintext password display and requires re-authentication for destructive operations |
| G-6 | Automatically lock the vault after configurable inactivity or background duration |
| G-7 | Clear clipboard contents automatically after a fixed timeout |
| G-8 | Operate entirely offline with zero network dependencies |

## 1.2 Security Objectives

| ID | Objective | Priority |
|----|-----------|----------|
| SO-1 | All credential fields (title, username, password, URL, notes, tags) are encrypted individually before persistence | CRITICAL |
| SO-2 | The DataKey never exists in plaintext on disk; it exists in memory only while the session is unlocked | CRITICAL |
| SO-3 | The master password is never stored; only a derived key is used transiently | CRITICAL |
| SO-4 | Ephemeral cryptographic material (PasswordKey, SecureModeKey, password CharArrays) is wiped from memory immediately after use | CRITICAL |
| SO-5 | Clipboard contents containing credentials are auto-cleared after 30 seconds | HIGH |
| SO-6 | Screenshot protection is enabled by default on Android | HIGH |
| SO-7 | Secure Mode passwords are never displayed in the UI under any circumstance | HIGH |
| SO-8 | Sessions auto-lock after a configurable inactivity/background timeout | HIGH |

## 1.3 Target Platforms

| Platform | Status | Biometric | Clipboard | Screenshot Protection | DeviceKey Storage |
|----------|--------|-----------|-----------|----------------------|-------------------|
| Android | Active | BiometricPrompt (BIOMETRIC_STRONG) | ClipboardManager | FLAG_SECURE | Android KeyStore (AES-256-GCM, hardware-backed) |
| Desktop (JVM) | Active | Not available (stub returns NotAvailable) | java.awt.Toolkit clipboard | Not available (no-op) | Java Preferences with XOR obfuscation |
| iOS | Stub only (not included in build) | Not available (stub) | Not available (no-op stub) | Not available (no-op) | Not implemented |

---

# 2. System State Model

## 2.1 States

The system operates in a finite set of states. Two state layers exist: a **session layer** and a **UI overlay layer**.

### 2.1.1 Session States

| State | Definition | DataKey in Memory |
|-------|-----------|-------------------|
| **NOT_SETUP** | No vault has been created. No encrypted DataKey exists in `vault_config`. Initial state on first launch. | No |
| **LOCKED** | A vault exists (config persisted) but the DataKey is not in memory. All cryptographic operations are unavailable. | No |
| **UNLOCKED** | The DataKey is live in a `SensitiveData<ByteArray>` wrapper in `SessionManager`. All vault operations are available. | Yes |

### 2.1.2 UI Overlay States

These are orthogonal to the session state and only meaningful when the session is UNLOCKED.

| Overlay | Definition |
|---------|-----------|
| **SECURE_MODE_OFF** | Standard operation. Passwords can be toggled visible. Edit/delete are freely available. |
| **SECURE_MODE_ON** | Passwords are permanently masked in the UI. Edit and delete require re-authentication (biometric or master password). The global enable/disable toggle is persisted across sessions. |

### 2.1.3 Navigation Surfaces

| Surface | When Active |
|---------|-------------|
| **Auth** | Session is NOT_SETUP or LOCKED. Shows Onboarding, Register, or Login screens. |
| **Main** | Session is UNLOCKED. Shows Vault, Generator, and Settings tabs. |

The system enforces: whenever `KeyManagerState == Locked` and the current surface is `Main`, navigation is forced to `Auth/Login`.

## 2.2 State Transitions

```
┌─────────────┐
│  NOT_SETUP  │
└──────┬──────┘
       │ setupVault(password) succeeds
       ▼
┌─────────────┐   lock() / background timeout /   ┌──────────┐
│  UNLOCKED   │──────────────────────────────────► │  LOCKED  │
│             │◄──────────────────────────────────  │          │
└─────────────┘   unlockWithPassword(password)     └──────────┘
       │           or unlockWithBiometric()               │
       │                                                  │
       │ clear() (vault wipe)                             │
       ▼                                                  │
┌─────────────┐                                           │
│  NOT_SETUP  │◄──────────────────────────────────────────┘
└─────────────┘   clear() (vault wipe)
```

### 2.2.1 Transition Table

| # | From | To | Trigger | Preconditions | Actions |
|---|------|----|---------|---------------|---------|
| T-1 | NOT_SETUP | UNLOCKED | `setupVault(password)` succeeds | No vault config exists | Generate salt; derive PasswordKey via Argon2id; generate random DataKey; encrypt DataKey with PasswordKey; store encrypted DataKey + Argon2 params in `vault_config`; store DataKey in PlatformKeyStore; load DataKey into SessionManager; wipe PasswordKey and local DataKey copy; wipe password CharArray |
| T-2 | LOCKED | UNLOCKED | `unlockWithPassword(password)` succeeds | Vault config exists and `isSetup == true` | Read salt + Argon2 config from `vault_config`; derive PasswordKey; decrypt DataKey; update PlatformKeyStore; load DataKey into SessionManager; wipe PasswordKey and local DataKey copy; wipe password CharArray |
| T-3 | LOCKED | UNLOCKED | `unlockWithBiometric()` succeeds | Vault config exists; DeviceKey stored in PlatformKeyStore | Retrieve DataKey from PlatformKeyStore; load DataKey into SessionManager; wipe local DataKey copy |
| T-4 | UNLOCKED | LOCKED | `lock()` (manual) | Session is unlocked | Wipe DataKey via `SensitiveData.close()`; set session state to Locked; force navigation to Auth/Login |
| T-5 | UNLOCKED | LOCKED | `onAppBackground()` when timeout == -1 (immediate) | Session is unlocked; lock timeout configured to immediate | Same as T-4 |
| T-6 | UNLOCKED | LOCKED | `onAppForeground()` when background duration >= timeout | Session is unlocked; timeout > 0 | Same as T-4 |
| T-7 | UNLOCKED | LOCKED | `checkAutoLock()` when inactivity >= timeout | Session is unlocked; timeout > 0 | Same as T-4 |
| T-8 | UNLOCKED | NOT_SETUP | `clear()` (vault wipe) | — | Lock session; delete DeviceKey from PlatformKeyStore; null vault config |
| T-9 | LOCKED | NOT_SETUP | `clear()` (vault wipe) | — | Same as T-8 |

### 2.2.2 Secure Mode Transitions

| # | From | To | Trigger | Auth Required |
|---|------|----|---------|---------------|
| SM-1 | SECURE_MODE_OFF | SECURE_MODE_ON | User toggles ON in settings | No |
| SM-2 | SECURE_MODE_ON | SECURE_MODE_OFF | User toggles OFF in settings | Yes: biometric, fallback to master password |

## 2.3 Allowed Operations per State

| Operation | NOT_SETUP | LOCKED | UNLOCKED | UNLOCKED + SECURE_MODE |
|-----------|:---------:|:------:|:--------:|:----------------------:|
| Complete onboarding | Yes | — | — | — |
| Create vault (register) | Yes | — | — | — |
| Enter master password | — | Yes | — | — |
| Biometric unlock | — | Yes (if enrolled) | — | — |
| List entries | — | — | Yes | Yes |
| View entry detail | — | — | Yes | Yes (password always masked) |
| Toggle password visibility | — | — | Yes | **Forbidden** |
| Copy password to clipboard | — | — | Yes | Yes (via secure `usePassword` path) |
| Copy username to clipboard | — | — | Yes | Yes |
| Add entry | — | — | Yes | Yes |
| Edit entry | — | — | Yes | Requires re-auth |
| Delete entry | — | — | Yes (with confirmation) | Requires re-auth + confirmation |
| Search entries | — | — | Yes | Yes |
| Generate password | — | — | Yes | Yes |
| Change master password | — | — | Yes | Yes |
| Change settings | — | — | Yes | Yes |
| Lock vault | — | — | Yes | Yes |
| Enable Secure Mode | — | — | Yes | — |
| Disable Secure Mode | — | — | — | Requires re-auth |

---

# 3. Core Business Flows

## 3.1 Add Password

### Normal Flow

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1 | User | Taps "+" FAB on Vault screen | System navigates to AddEdit screen with empty form |
| 2 | User | Fills required fields: title, username, password. Optionally fills: URL, notes, category, tags, favorite toggle, security mode toggle | System enables Save button when all three required fields are non-blank |
| 3 | User | Taps "Save" | System constructs `PasswordEntry` with `id=null`, `createdAt=now`, `updatedAt=now` |
| 4 | System | Retrieves DataKey from SessionManager | Returns a copy of the DataKey; refreshes `lastActivityTime` |
| 5 | System | Encrypts each field individually | title, username, url, notes, tags: encrypted with DataKey via XChaCha20-Poly1305. Password: encrypted with DataKey (standard) or SecureModeKey (if `securityMode=true`) |
| 6 | System | Serializes each `EncryptedData` to JSON storage format | Format: `{"version":"v2","iv":"<base64>","ciphertext":"<base64>","tag":"<base64>"}` |
| 7 | System | INSERTs row into `password_entries` table | Plaintext columns: `category`, `is_favorite`, `security_mode`, `created_at`, `updated_at`. All content columns: encrypted JSON strings |
| 8 | System | Returns success | Navigates to Vault screen; refreshes entry list; displays "已保存" snackbar |

### Error Flows

| Condition | System Behavior |
|-----------|----------------|
| E-ADD-1: Session is locked when Save is tapped | `getDataKey()` returns null. Error message: "保险库已锁定". Save aborted. |
| E-ADD-2: Encryption fails (e.g., libsodium not initialized) | Exception caught by `runCatching`. Error message displayed from exception. Entry not persisted. |
| E-ADD-3: Database INSERT fails | Exception caught. Error message: "保存失败" or exception message. Entry not persisted. |
| E-ADD-4: SecurityModeManager unavailable when `securityMode=true` | Falls back to encrypting password with DataKey directly (standard encryption path). No error surfaced. |

### Validation Rules

| Field | Rule |
|-------|------|
| title | Required. Must be non-blank. No maximum length enforced. |
| username | Required. Must be non-blank. No maximum length enforced. |
| password | Required. Must be non-blank. No minimum complexity enforced. |
| url | Optional. No format validation. |
| notes | Optional. No length limit. |
| category | Defaults to "默认" if blank. |
| tags | Inherited from existing entry on edit; empty list on create. |

## 3.2 Retrieve Password

### Normal Flow (Standard Entry)

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1 | User | Taps a PasswordCard on Vault screen | System navigates to PasswordDetail screen |
| 2 | System | Retrieves DataKey from SessionManager | Returns a copy of the DataKey |
| 3 | System | SELECTs row from `password_entries` by ID | Retrieves encrypted row |
| 4 | System | Decrypts all fields | Each encrypted JSON string → parse → XChaCha20-Poly1305 decrypt. Password: decrypted with DataKey (standard) or SecureModeKey (if `security_mode=1`) |
| 5 | System | Stores decrypted `PasswordEntry` in ViewModel StateFlow | All plaintext fields held in memory |
| 6 | System | Renders detail screen | Password displayed as "••••••••••••" by default |
| 7 | User | Taps eye icon | Password toggles to plaintext display. No auto-hide timer exists. Remains visible until user toggles off or navigates away. |
| 8 | User | Taps copy icon | Password string copied to clipboard. Auto-clear timer starts (30 seconds). Message: "密码已复制，将在 30 秒后清除" |

### Normal Flow (Secure Mode Entry)

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1–5 | — | Same as standard flow | Same — password IS decrypted into memory |
| 6 | System | Renders detail screen | Password displayed as "••••••••••••". No eye icon. Only "使用" (Use) button visible. |
| 7 | User | Taps "使用" button | System re-reads encrypted password cipher from database; decrypts on-demand via `SecurityModeManager.usePassword()`; copies plaintext to clipboard; wipes decrypted bytes from memory; starts 30-second auto-clear timer. Message: "密码已使用（已复制），将在 30 秒后清除" |

### Error Flows

| Condition | System Behavior |
|-----------|----------------|
| E-RET-1: Session locked | `getDataKey()` returns null. Message: "保险库已锁定". Detail screen shows error. |
| E-RET-2: Entry not found in database | `getById()` returns null. No entry displayed. |
| E-RET-3: Decryption fails (wrong key, corrupted data) | Exception caught. Message: "加载失败" or exception message. |
| E-RET-4: Secure Mode `usePassword` fails | Exception caught. Message: "密码使用失败" or exception message. Clipboard not modified. |

## 3.3 Autofill

### Status: NOT IMPLEMENTED

The autofill settings screen exists as a UI placeholder. It contains two non-functional toggles with local-only state (not persisted). The screen explicitly states: "本页仅实现界面，功能将在后续版本接入。"

**What does not exist:**
- No `AutofillService` subclass
- No Android manifest service declaration
- No autofill dataset provider
- No fill/save request handling
- No cross-platform autofill abstraction

**Cross-platform behavioral differences regarding autofill:**
All platforms behave identically — autofill is unavailable.

## 3.4 Delete Password

### Normal Flow (Standard Entry, Secure Mode OFF)

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1 | User | On PasswordDetail screen, taps "删除" | Confirmation dialog: "删除「{title}」后将无法恢复，是否继续？" |
| 2 | User | Confirms deletion | System calls `passwordRepository.deleteById(id)` |
| 3 | System | Executes `DELETE FROM password_entries WHERE id = ?` | Row removed from database |
| 4 | System | Returns success | Navigates to Vault screen; refreshes entry list |

### Normal Flow (Security Mode Entry, Per-Entry `securityMode=true`)

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1 | User | Taps "删除" | Re-authentication triggered (not a confirmation dialog) |
| 2 | System | Checks if biometric is enabled and available | — |
| 2a | System | Biometric available → shows biometric prompt | — |
| 2b | System | Biometric unavailable → shows master password dialog | — |
| 3 | User | Authenticates successfully | `verifiedAction = SensitiveAction.Delete` |
| 4 | System | Shows confirmation dialog (same as standard flow step 1) | — |
| 5 | User | Confirms | Same as standard flow steps 2–4 |

### Normal Flow (Global Secure Mode ON)

| Step | System Behavior |
|------|----------------|
| 1 | Edit and Delete buttons are entirely hidden from the UI. Deletion is not possible. |

### Error Flows

| Condition | System Behavior |
|-----------|----------------|
| E-DEL-1: Biometric authentication fails | Message: "生物识别验证失败". User remains on detail screen. |
| E-DEL-2: Biometric cancelled | Message: "已取消生物识别验证". User remains on detail screen. |
| E-DEL-3: Master password incorrect | Message: "主密码错误". Password dialog remains open. |
| E-DEL-4: Master password blank | Message: "请输入主密码". Password dialog remains open. |
| E-DEL-5: Database DELETE fails | Exception caught. Message: "删除失败" or exception message. Entry not deleted. |
| E-DEL-6: Entry ID is null | `delete()` returns immediately (no-op). |

### Post-Delete Cleanup

The system performs a SQL `DELETE` only. The following are **not** performed:
- No overwrite of encrypted data before deletion (SQLite does not guarantee physical erasure)
- No SecureModeKey rotation
- No in-memory cleanup of the decrypted `PasswordEntry` in ViewModel state (persists until navigation)
- No deletion audit log

---

# 4. Functional Requirements

## 4.1 Module: Key Derivation (Argon2id KDF)

### Inputs
| Name | Type | Constraints |
|------|------|-------------|
| password | CharArray | Non-empty |
| salt | ByteArray | 16 bytes |
| config | Argon2Config | memoryKB >= 8192, iterations >= 1, parallelism >= 1, outputLength = 32 |

### Outputs
| Name | Type | Description |
|------|------|-------------|
| derivedKey | ByteArray | 32-byte key derived via Argon2id |

### Processing Logic
1. The system converts the password CharArray to a String and passes it to libsodium's `PasswordHash.pwhash` with `algorithm = 2` (Argon2id).
2. Default configuration: 128 MB memory (`131072` KB), 3 iterations, parallelism 4, 32-byte output.
3. The password CharArray is wiped via `MemorySanitizer.wipe()` in a `finally` block after derivation completes or fails.

### Security Constraints
- SEC-KDF-1: The password CharArray is wiped after derivation regardless of success or failure.
- SEC-KDF-2: The derived key is the caller's responsibility to wipe after use.

### Error Handling
- If libsodium is not initialized, `pwhash` throws. The caller catches this as a `CryptoError`.
- If any Argon2 parameter is below the minimum, the behavior is determined by libsodium (typically throws).

### Edge Cases
- EC-KDF-1: The password CharArray is converted to an immutable `String` internally. This String cannot be wiped from memory and persists in the JVM string pool until garbage collection. This is a known limitation on JVM platforms.

## 4.2 Module: Authenticated Encryption (XChaCha20-Poly1305)

### Inputs (Encrypt)
| Name | Type | Constraints |
|------|------|-------------|
| plaintext | ByteArray | Non-empty, max 10 MB |
| key | ByteArray | Exactly 32 bytes |

### Outputs (Encrypt)
| Name | Type | Description |
|------|------|-------------|
| EncryptedData | Object | Contains: version ("v2"), iv (24 bytes), ciphertext, tag (16 bytes) |

### Inputs (Decrypt)
| Name | Type | Constraints |
|------|------|-------------|
| encryptedData | EncryptedData | version == "v2", iv == 24 bytes, tag == 16 bytes |
| key | ByteArray | Exactly 32 bytes |

### Outputs (Decrypt)
| Name | Type | Description |
|------|------|-------------|
| plaintext | ByteArray | Original plaintext |

### Processing Logic

**Encryption:**
1. Generate 24-byte random nonce.
2. Call libsodium `SecretBox.easy(plaintext, nonce, key)` → returns `tag(16 bytes) || ciphertext`.
3. Split result into tag and ciphertext.
4. Return `EncryptedData(version="v2", iv=nonce, ciphertext=ciphertext, tag=tag)`.

**Decryption:**
1. Validate version == "v2", nonce size == 24, tag size == 16.
2. Reassemble: `tag || ciphertext`.
3. Call libsodium `SecretBox.openEasy(tag||ciphertext, iv, key)` → returns plaintext.
4. If authentication fails, libsodium throws.

**Storage Serialization:**
The `EncryptedData` object serializes to JSON: `{"version":"v2","iv":"<base64>","ciphertext":"<base64>","tag":"<base64>"}`. This JSON string is what gets stored in each database column.

### Security Constraints
- SEC-ENC-1: Each encryption operation generates a fresh random nonce. Nonces are never reused for the same key.
- SEC-ENC-2: The key size is validated at the start of every encrypt/decrypt call. If != 32 bytes, an `IllegalArgumentException` is thrown before any crypto operation.
- SEC-ENC-3: Decryption verifies authenticity via Poly1305 tag. Tampered ciphertext causes decryption to fail.

### Error Handling
| Condition | Behavior |
|-----------|----------|
| Key size != 32 | `IllegalArgumentException` thrown |
| Plaintext empty | `IllegalArgumentException` thrown |
| Version != "v2" | `IllegalArgumentException` thrown |
| Nonce size != 24 | `IllegalArgumentException` thrown |
| Tag size != 16 | `IllegalArgumentException` thrown |
| Authentication failure (tampered data) | libsodium throws exception (decryption returns garbage or exception depending on binding) |

### Edge Cases
- EC-ENC-1: The class is named `AesGcmCipher` but implements XChaCha20-Poly1305 via libsodium SecretBox. The naming is misleading; the actual algorithm is XChaCha20-Poly1305.

## 4.3 Module: Session Manager

### Inputs
| Name | Type | Description |
|------|------|-------------|
| dataKey | ByteArray | 32-byte DataKey to load into session |
| lockTimeoutMs | Long | Configurable timeout in milliseconds |

### Outputs
| Name | Type | Description |
|------|------|-------------|
| sessionState | StateFlow\<SessionState\> | Observable state: Locked or Unlocked |
| dataKeyCopy | ByteArray | Copy of DataKey (returned by `getDataKey()`) |

### State Transitions

| Method | Precondition | Effect |
|--------|-------------|--------|
| `unlock(dataKey)` | — | Close previous SensitiveData wrapper (wipe old key); create new SensitiveData wrapper with a copy of dataKey; set `isUnlocked=true`; reset `lastActivityTime`; clear `backgroundEnteredAtMs`; emit `SessionState.Unlocked` |
| `lock()` | — | Close SensitiveData wrapper (wipe DataKey); set `isUnlocked=false`; emit `SessionState.Locked` |
| `getDataKey()` | Session must be unlocked | Update `lastActivityTime` to now; return `dataKey.get().copyOf()` |
| `onAppBackground()` | — | If unlocked and timeout == -1: lock immediately. Otherwise: record `backgroundEnteredAtMs`. |
| `onAppForeground()` | — | If unlocked and background duration >= timeout: lock. Otherwise: reset `lastActivityTime`. |
| `checkAutoLock()` | — | If unlocked and elapsed since last activity >= timeout: lock. |

### Processing Logic: Lock Timeout Values

| Value | Meaning |
|-------|---------|
| -1 | Lock immediately when app goes to background |
| 0 | Never auto-lock |
| 60,000 | 1 minute |
| 300,000 | 5 minutes (default) |
| 900,000 | 15 minutes |
| 1,800,000 | 30 minutes |

### Security Constraints
- SEC-SES-1: The DataKey is stored inside a `SensitiveData<ByteArray>` wrapper. On `close()`, the wrapper calls `MemorySanitizer.wipe()` on the key bytes and nullifies the reference.
- SEC-SES-2: `getDataKey()` returns a **copy** of the key, not a reference. The original stays protected.
- SEC-SES-3: `getDataKey()` implicitly refreshes `lastActivityTime`, extending the session.

### Error Handling
| Condition | Behavior |
|-----------|----------|
| `getDataKey()` called while locked | `IllegalStateException("Session is locked")` |
| `requireUnlocked()` called while locked | `IllegalStateException("Session is locked")` |

### Edge Cases
- EC-SES-1: `checkAutoLock()` is defined and tested but has **no periodic caller** in the current implementation. Foreground inactivity alone does not trigger auto-lock unless the app is backgrounded and returned.
- EC-SES-2: `extendSession()` exists but has **no callers**. User interactions (navigation, scrolling, tapping) do not reset the inactivity timer. Only `getDataKey()` implicitly refreshes the timer.
- EC-SES-3: On desktop, `onAppBackground()` and `onAppForeground()` are never called. Desktop sessions do not auto-lock based on window focus or minimize events.
- EC-SES-4: When timeout is set to `0` (never), no maximum session lifetime exists. The session remains open indefinitely.

## 4.4 Module: Platform Key Store (DeviceKey Management)

### Inputs
| Name | Type | Description |
|------|------|-------------|
| key | ByteArray | 32-byte DataKey to store |

### Outputs
| Name | Type | Description |
|------|------|-------------|
| key | ByteArray? | Retrieved DataKey, or null if not stored |
| hasDeviceKey | Boolean | Whether a DeviceKey is currently stored |
| isHardwareBacked | Boolean | Whether the storage is hardware-backed |

### Processing Logic

**Android:**
1. A 256-bit AES-GCM wrapping key is created in Android KeyStore (hardware-backed when available) under alias `securevault_device_key`.
2. `storeDeviceKey`: encrypts the DataKey with AES-256-GCM using the wrapping key. Stores the resulting IV + encrypted blob in SharedPreferences (`securevault.keystore`).
3. `getDeviceKey`: reads IV + blob from SharedPreferences; decrypts with the KeyStore wrapping key; returns the DataKey.
4. `deleteDeviceKey`: removes the wrapping key from Android KeyStore and clears SharedPreferences.

**Desktop:**
1. A random "master key" is generated and stored in Java Preferences.
2. `storeDeviceKey`: XORs the DataKey with the master key; stores the result as base64 in Java Preferences.
3. `getDeviceKey`: reads base64 from Preferences; decodes; XORs with master key; returns the DataKey.

### Security Constraints
- SEC-PKS-1 (Android): The wrapping key resides in hardware-backed KeyStore when the device supports it. The DataKey cannot be extracted without the KeyStore key.
- SEC-PKS-2 (Desktop): **KNOWN WEAKNESS.** The desktop implementation uses XOR with a key stored in plaintext in Java Preferences. An attacker with read access to the Preferences file can recover the DataKey trivially.

### Edge Cases
- EC-PKS-1: If the Android KeyStore key is invalidated (e.g., user changes device lock), `getDeviceKey()` returns null. The user must unlock with the master password to re-store the DeviceKey.

## 4.5 Module: Security Mode Manager

### Inputs
| Name | Type | Description |
|------|------|-------------|
| plaintextPassword | String | Password to encrypt |
| encryptedPassword | String | Encrypted password (storage format JSON) |
| dataKey | ByteArray | 32-byte DataKey |
| securityMode | Boolean | Whether this entry uses security mode encryption |

### Outputs
| Name | Type | Description |
|------|------|-------------|
| encryptedResult | String | Encrypted password in storage format |
| decryptedResult | String | Decrypted plaintext password |

### Processing Logic

**Key Hierarchy for Secure Mode:**
1. A 32-byte SecureModeKey is generated randomly and encrypted with the DataKey.
2. The encrypted SecureModeKey is stored in `vault_config` under key `encrypted_secure_mode_key`.
3. On first use, if no SecureModeKey exists, one is created and persisted.

**Encryption (`encryptPasswordForStorage`):**
- If `securityMode == false`: encrypt with DataKey directly.
- If `securityMode == true`: decrypt SecureModeKey from config using DataKey; encrypt password with SecureModeKey; wipe SecureModeKey.

**Decryption (`decryptPasswordForRead`):**
- If `securityMode == false`: decrypt with DataKey directly.
- If `securityMode == true`: decrypt SecureModeKey from config using DataKey; decrypt password with SecureModeKey; wipe SecureModeKey.

**Secure Copy (`usePassword`):**
1. Decrypt password to byte array (using appropriate key based on securityMode flag).
2. Convert bytes to string.
3. Copy to clipboard via `SecureClipboard.copy()`.
4. Schedule auto-clear (30 seconds).
5. Wipe decrypted byte array in `finally` block.
6. If SecureModeKey was used, wipe it in `finally` block.

### Security Constraints
- SEC-SM-1: The SecureModeKey is wiped from memory after every single encrypt/decrypt/use operation.
- SEC-SM-2: In `usePassword()`, the decrypted password bytes are wiped in a `finally` block regardless of success or failure.
- SEC-SM-3: The SecureModeKey is never exposed to the UI layer. It exists only within `SecurityModeManager` during a single operation.

### Error Handling
| Condition | Behavior |
|-----------|----------|
| SecureModeKey decryption fails | Exception propagates to caller. Password not returned. |
| Config repository unavailable | `getOrCreateSecureModeKey` throws. Caller receives exception. |

### Edge Cases
- EC-SM-1: The `securityMode` flag is per-entry. An entry created with `securityMode=true` remains encrypted with the SecureModeKey even if the global Secure Mode toggle is later turned off. The global toggle only controls UI restrictions, not the encryption layer.
- EC-SM-2: The SecureModeKey is never rotated. Once created, it persists for the lifetime of the vault. Deleting the last security-mode entry does not trigger SecureModeKey cleanup.

## 4.6 Module: Secure Clipboard

### Inputs
| Name | Type | Constraints |
|------|------|-------------|
| text | String | Credential to copy |
| label | String | Clipboard label (default: "Password") |
| delayMs | Long | Auto-clear delay (default: 30,000 ms) |

### Processing Logic
1. Copy text to system clipboard using platform API.
2. Start a coroutine-based timer for `delayMs` milliseconds.
3. If a new copy operation occurs before the timer fires, cancel the previous timer.
4. When the timer fires, replace clipboard contents with an empty string.

### Security Constraints
- SEC-CLIP-1: Auto-clear timeout is fixed at 30 seconds. It is not user-configurable.
- SEC-CLIP-2: Each new copy cancels any pending auto-clear timer and starts a new one.
- SEC-CLIP-3: Clearing replaces clipboard contents with empty text rather than just removing the entry.

### Error Handling
- Clipboard operations are fire-and-forget. No exceptions are propagated to the caller.

### Edge Cases
- EC-CLIP-1 (iOS): The entire `SecureClipboard` implementation is a no-op stub. Copy and clear do nothing.
- EC-CLIP-2: `CryptoConstants.Clipboard.DEFAULT_CLEAR_TIMEOUT_MS` (30,000) is defined as a named constant but is never referenced by code. The 30-second value is instead hardcoded as the `expect` function's default parameter.

## 4.7 Module: Memory Sanitizer

### Inputs
| Name | Type | Constraints |
|------|------|-------------|
| data | ByteArray, CharArray, or IntArray | Target to wipe |
| passes | Int | Number of overwrite passes (default: 3) |

### Processing Logic
1. For each pass (0 to `passes-1`): overwrite every element with `pass % 256` (ByteArray), `(pass % 256) + 1` as char (CharArray), or `pass` (IntArray).
2. Final pass: zero-fill all elements.
3. Verification: `isWiped()` returns true if all elements are zero.

### Security Constraints
- SEC-MEM-1: Wiping is best-effort on JVM. The garbage collector may have relocated the array, leaving original bytes in freed heap regions.
- SEC-MEM-2: `SensitiveData<T>` provides RAII-style wiping — the cleaner function (which calls `MemorySanitizer.wipe()`) is invoked on `close()`.
- SEC-MEM-3: `SensitiveData.ofByteArray()` stores a **copy** of the input. The caller retains responsibility for wiping the original.

### Edge Cases
- EC-MEM-1: Passes must be > 0 or an `IllegalArgumentException` is thrown.

## 4.8 Module: Biometric Authentication

### Inputs
| Name | Type | Description |
|------|------|-------------|
| title | String | Prompt title |
| subtitle | String | Prompt subtitle |

### Outputs
| Name | Type | Description |
|------|------|-------------|
| BiometricResult | Sealed class | Success, Failed, Cancelled, NotAvailable, Error(message) |

### Processing Logic
- **Android**: Uses `BiometricPrompt` with `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`. The prompt is shown on the main thread. Result maps to `BiometricResult`.
- **Desktop**: Always returns `BiometricResult.NotAvailable`.
- **iOS**: Always returns `BiometricResult.NotAvailable`.

### Security Constraints
- SEC-BIO-1: Biometric unlock requires a DeviceKey to be stored in PlatformKeyStore (meaning the user must have unlocked with the master password at least once).
- SEC-BIO-2: Enabling biometric in settings requires a successful biometric prompt first.

### Error Handling
| BiometricResult | UI Response |
|-----------------|-------------|
| Success | Proceed with operation |
| Failed | Error message: "生物识别验证失败" |
| Cancelled | Error message: "已取消生物识别验证" |
| NotAvailable | Fall back to master password dialog |
| Error(msg) | Display error message |

### Edge Cases
- EC-BIO-1: A `BiometricState` rate-limiter class exists (5 max attempts, 30-second lockout, 500ms debounce) but is **not wired into any authentication flow**. The application-layer brute-force protection is inactive. Protection relies entirely on the OS-level biometric prompt.

## 4.9 Module: Password Repository

### Inputs (Create)
| Name | Type | Description |
|------|------|-------------|
| entry | PasswordEntry | Domain object with plaintext fields |
| dataKey | ByteArray | 32-byte DataKey |

### Outputs (Create)
| Name | Type | Description |
|------|------|-------------|
| id | Long | Auto-generated row ID |

### Processing Logic

**Create:**
1. Encrypt each content field individually (see §4.2 for encryption details).
2. For the password field: route through `SecurityModeManager` if available (uses SecureModeKey for security-mode entries).
3. Extract IV from the title's encrypted data (used as a row-level IV reference).
4. INSERT into `password_entries` table.
5. Return the auto-incremented ID via `last_insert_rowid()`.

**Update:**
1. Verify entry ID exists in database.
2. Re-encrypt all fields with fresh nonces (each encryption generates a new random nonce).
3. UPDATE row in `password_entries` table.

**Delete:**
1. Execute `DELETE FROM password_entries WHERE id = ?`.
2. No pre-deletion overwrite. No key rotation.

**Search:**
1. Apply database-level filter (category, favorites, security-mode).
2. Decrypt **all** matching rows into `PasswordEntry` objects.
3. Perform client-side text search across decrypted fields: title, username, url, notes, tags.
4. Return matching entries.

### Security Constraints
- SEC-REPO-1: Each field is encrypted with an independent random nonce. No two fields share a nonce.
- SEC-REPO-2: The DataKey is required for all read and write operations (except `deleteById`, which only needs the row ID).
- SEC-REPO-3: Search decrypts all candidate rows into memory. The number of plaintext entries in memory at any time equals the search result count.

### Error Handling
- All database operations are wrapped in `runCatching` by the calling ViewModel. Exceptions propagate as error messages.

### Edge Cases
- EC-REPO-1: `searchByTitle` query in SQL exists but is never used (search is always client-side after full decryption).
- EC-REPO-2: The `generated_passwords` table stores generated passwords in **plaintext**. These are not encrypted.

## 4.10 Module: Screenshot Protection

### Processing Logic
- **Android**: Sets `WindowManager.LayoutParams.FLAG_SECURE` on the activity window.
- **Desktop**: No-op.
- **iOS**: No-op.

### Security Constraints
- SEC-SS-1: Screenshot protection is enabled by default on app initialization.
- SEC-SS-2: The user can toggle it off in settings. The preference is persisted as `screenshot_allowed` in `vault_config`.

## 4.11 Module: Password Generator

### Processing Logic
1. User selects a preset (Strong, etc.) or configures custom parameters.
2. System generates a random password based on character set and length.
3. Generated password is displayed to the user.
4. User can copy to clipboard (30-second auto-clear applies).
5. Generated password is stored in `generated_passwords` table in **plaintext** with a strength score and timestamp.
6. Recent generated passwords (last 20) can be viewed.

### Security Constraints
- SEC-GEN-1: **KNOWN WEAKNESS.** Generated passwords are stored in plaintext in the `generated_passwords` SQL table. An attacker with database access can read all recently generated passwords.

---

# 5. Security Requirements

## 5.1 Key Management Rules

| ID | Rule |
|----|------|
| KM-1 | The master password is never stored in any form. It is converted to a PasswordKey via Argon2id and then immediately wiped. |
| KM-2 | The PasswordKey exists in memory only for the duration of a single encrypt or decrypt operation on the DataKey. It is wiped in the same function that creates it. |
| KM-3 | The DataKey is the only long-lived key. It resides in `SessionManager` inside a `SensitiveData` wrapper while the vault is unlocked, and is wiped on lock. |
| KM-4 | The DataKey never changes during a password change operation. Only its encryption wrapper (PasswordKey-encrypted blob) is regenerated. |
| KM-5 | The SecureModeKey is derived on-demand and wiped after every single encrypt/decrypt/use operation. It never persists in memory across operations. |
| KM-6 | On vault wipe (`clear()`), the session is locked, the DeviceKey is deleted from PlatformKeyStore, and all in-memory config is nullified. Database entries become permanently inaccessible. |

## 5.2 Memory Wiping Behavior

### What IS Wiped (with code-level guarantee)

| Material | When Wiped | Mechanism |
|----------|-----------|-----------|
| PasswordKey (ByteArray) | After encrypting/decrypting DataKey | `MemorySanitizer.wipe()` in function body |
| DataKey local copy (ByteArray) | After loading into SessionManager | `MemorySanitizer.wipe()` in function body |
| Master password (CharArray) | After Argon2 derivation | `MemorySanitizer.wipe()` in `finally` block |
| SecureModeKey (ByteArray) | After every encrypt/decrypt/use | `MemorySanitizer.wipe()` in `finally` block |
| Decrypted password bytes in `usePassword` | After clipboard copy | `MemorySanitizer.wipe()` in `finally` block |
| Previous DataKey on re-unlock | Before new key is loaded | `SensitiveData.close()` |
| DataKey on session lock | On `lock()` call | `SensitiveData.close()` → `MemorySanitizer.wipe()` |

### What is NOT Wiped (known gaps)

| Material | Reason |
|----------|--------|
| Kotlin `String` objects created from decrypted fields | Strings are immutable on JVM; cannot be zeroed. Every `decryptField()` returns a `String`. |
| Decrypted `PasswordEntry` in ViewModel `StateFlow` | Persists until the user navigates away or the ViewModel is garbage-collected. |
| DataKey copies returned by `getDataKey()` | Callers (ViewModels) do not wipe returned copies. |
| `String` created from password CharArray in Argon2Kdf | `password.concatToString()` creates an immutable copy before passing to libsodium. |
| Generated passwords in `generated_passwords` table | Stored as plaintext. Never encrypted. |

## 5.3 Clipboard Policy

| Rule | Value |
|------|-------|
| Auto-clear timeout | 30 seconds (fixed) |
| Clear mechanism | Replace clipboard with empty string |
| Timer behavior on re-copy | Previous timer is cancelled; new 30-second timer starts |
| Applies to | Password copy, username copy, generated password copy |
| Android behavior | Functional |
| Desktop behavior | Functional |
| iOS behavior | No-op (stub) |
| User configurable | No |

## 5.4 Encryption Constraints

| Constraint | Value |
|------------|-------|
| Algorithm | XChaCha20-Poly1305 (libsodium SecretBox) |
| Key size | 256-bit (32 bytes) |
| Nonce size | 192-bit (24 bytes) |
| Auth tag size | 128-bit (16 bytes) |
| Nonce generation | Random per encryption (fresh for each field) |
| Storage format | JSON with base64-encoded iv, ciphertext, tag |
| Storage format version | "v2" |
| Per-field encryption | Each database column is encrypted independently with its own nonce |
| Plaintext metadata columns | `category`, `is_favorite`, `security_mode`, `created_at`, `updated_at` |

## 5.5 Authentication Constraints

| Constraint | Detail |
|------------|--------|
| Vault unlock | Master password OR biometric (if enrolled) |
| Biometric enrollment | Requires at least one prior master-password unlock (to populate PlatformKeyStore) |
| Enabling biometric setting | Requires successful biometric prompt |
| Disabling Secure Mode | Requires biometric or master password |
| Edit in Secure Mode | Requires biometric or master password |
| Delete in Secure Mode | Requires biometric or master password |
| Failed password attempts | No application-layer rate limiting. No lockout. |
| Failed biometric attempts | BiometricState rate-limiter exists but is not wired in. OS-level limits apply. |

---

# 6. Non-functional Requirements

## 6.1 Performance

| Metric | Requirement | Basis |
|--------|-------------|-------|
| NFR-PERF-1: KDF latency | Argon2id derivation with default config (128 MB, 3 iter) completes within the device's capability. Expected: 0.5–3 seconds on modern devices. | Configured via `AdaptiveArgon2Config` but defaults to standard config. |
| NFR-PERF-2: Encryption/decryption per field | Single-field XChaCha20-Poly1305 operation completes in < 1 ms for typical credential sizes (< 1 KB). | libsodium native performance. |
| NFR-PERF-3: Vault list loading | Decrypting all entries for display. Linear in entry count. No pagination. All entries decrypted on each load. | Current implementation decrypts every row on `getAll()`. |
| NFR-PERF-4: Search latency | Linear in total entry count (all entries decrypted, then filtered client-side). | No server-side search index. |

## 6.2 Security Level

| Metric | Value |
|--------|-------|
| Encryption algorithm strength | 256-bit key with XChaCha20-Poly1305 (considered equivalent to AES-256-GCM in security margin) |
| KDF strength | Argon2id with 128 MB memory, 3 iterations — resistant to GPU-based brute force at current default parameters |
| DeviceKey protection (Android) | Hardware-backed AES-256-GCM in Android KeyStore |
| DeviceKey protection (Desktop) | **Weak.** XOR with plaintext key in Java Preferences. |
| Random number generation | **KNOWN ISSUE.** Uses `kotlin.random.Random.Default` (PRNG, not CSPRNG) for all random values including keys, nonces, and salts. |

## 6.3 Cross-Platform Consistency

| Behavior | Android | Desktop | iOS (stub) |
|----------|---------|---------|------------|
| Encryption/decryption | Identical (common module) | Identical | Identical |
| Biometric | Full (BiometricPrompt) | Unavailable | Unavailable |
| Clipboard auto-clear | Functional | Functional | No-op |
| Screenshot protection | FLAG_SECURE | No-op | No-op |
| DeviceKey storage | Hardware-backed KeyStore | Java Preferences + XOR | Not implemented |
| Auto-lock on background | Functional (lifecycle hooks in MainActivity) | **Not functional** (no lifecycle hooks) | Not implemented |
| Auto-lock on inactivity | **Not functional** (checkAutoLock never called periodically) | **Not functional** | Not implemented |

## 6.4 Data Consistency (Offline-First)

| Requirement | Detail |
|-------------|--------|
| NFR-DATA-1 | All data is stored locally in SQLite. No network synchronization. |
| NFR-DATA-2 | SQLDelight provides compile-time SQL validation and generated type-safe queries. |
| NFR-DATA-3 | Config values (vault settings, Argon2 parameters) are stored in `vault_config` key-value table. |
| NFR-DATA-4 | No data migration path exists between storage format versions (v1 → v2). |
| NFR-DATA-5 | Database operations are suspend functions, executed on `Dispatchers.Default`. |

---

# 7. Acceptance Criteria

## 7.1 Vault Setup & Unlock

### AC-1: First-time vault creation
```
GIVEN the app is launched for the first time
  AND no vault config exists in the database
WHEN the user completes onboarding and enters a master password on the Register screen
THEN the system derives a PasswordKey via Argon2id
  AND generates a random 32-byte DataKey
  AND encrypts the DataKey with the PasswordKey
  AND stores the encrypted DataKey + Argon2 parameters in vault_config
  AND stores the DataKey in PlatformKeyStore
  AND loads the DataKey into SessionManager
  AND wipes the PasswordKey and password from memory
  AND transitions to UNLOCKED state
  AND navigates to the Vault screen
```

### AC-2: Password unlock (success)
```
GIVEN a vault exists (state == LOCKED)
WHEN the user enters the correct master password and submits
THEN the system derives the PasswordKey
  AND decrypts the DataKey successfully
  AND transitions to UNLOCKED state
  AND navigates to the Vault screen
```

### AC-3: Password unlock (failure)
```
GIVEN a vault exists (state == LOCKED)
WHEN the user enters an incorrect master password and submits
THEN decryption fails (authentication tag mismatch)
  AND the system displays "主密码错误"
  AND the state remains LOCKED
  AND the password CharArray is wiped from memory
```

### AC-4: Biometric unlock (success)
```
GIVEN a vault exists (state == LOCKED)
  AND biometric is enabled in settings
  AND a DeviceKey exists in PlatformKeyStore
WHEN the user initiates biometric unlock
  AND the OS biometric prompt returns success
THEN the system retrieves the DataKey from PlatformKeyStore
  AND loads it into SessionManager
  AND transitions to UNLOCKED state
```

### AC-5: Biometric unlock (not enrolled)
```
GIVEN a vault exists (state == LOCKED)
  AND no DeviceKey exists in PlatformKeyStore (user never unlocked with password)
WHEN the user initiates biometric unlock
THEN the system displays "尚未准备生物识别解锁，请先用主密码登录一次"
  AND the state remains LOCKED
```

### AC-6: Duplicate vault setup prevention
```
GIVEN a vault already exists
WHEN setupVault() is called
THEN the system returns VaultAlreadySetup error
  AND no keys are generated or stored
```

## 7.2 Password CRUD

### AC-7: Add password entry
```
GIVEN the vault is UNLOCKED
WHEN the user fills title, username, and password (all non-blank) and taps Save
THEN each field is encrypted individually with a fresh random nonce
  AND the encrypted entry is inserted into password_entries
  AND the user is navigated to the Vault screen
  AND the Vault list refreshes to include the new entry
```

### AC-8: Add password with security mode
```
GIVEN the vault is UNLOCKED
  AND the user enables the security mode toggle for the entry
WHEN the user saves the entry
THEN the password field is encrypted with the SecureModeKey (not the DataKey)
  AND all other fields are encrypted with the DataKey
  AND security_mode is set to 1 in the database row
```

### AC-9: View password (standard entry)
```
GIVEN the vault is UNLOCKED
WHEN the user taps an entry in the list
THEN all fields are decrypted and displayed
  AND the password is shown as "••••••••••••"
  AND an eye icon is available to toggle visibility
```

### AC-10: View password (secure mode entry)
```
GIVEN the vault is UNLOCKED
  AND the entry has securityMode=true
WHEN the user views the entry detail
THEN the password is shown as "••••••••••••"
  AND no eye icon is displayed
  AND only a "使用" (Use) button is displayed
```

### AC-11: Copy password (standard entry)
```
GIVEN the user is on a standard entry's detail screen
WHEN the user taps the copy icon
THEN the password is copied to the system clipboard
  AND a 30-second auto-clear timer starts
  AND the message "密码已复制，将在 30 秒后清除" is displayed
```

### AC-12: Use password (secure mode entry)
```
GIVEN the user is on a secure mode entry's detail screen
WHEN the user taps the "使用" button
THEN the system re-reads the encrypted password from the database
  AND decrypts it using the SecureModeKey
  AND copies the plaintext to the clipboard
  AND wipes the decrypted bytes from memory
  AND starts a 30-second auto-clear timer
  AND displays "密码已使用（已复制），将在 30 秒后清除"
```

### AC-13: Delete password (standard, no secure mode)
```
GIVEN the user is on a standard entry's detail screen
  AND global Secure Mode is OFF
WHEN the user taps "删除" and confirms the dialog
THEN the entry is deleted from password_entries
  AND the user is navigated to the Vault screen
  AND the list refreshes without the deleted entry
```

### AC-14: Delete password (secure mode entry — biometric re-auth)
```
GIVEN the user is on a secure mode entry's detail screen
  AND biometric is enabled and available
WHEN the user taps "删除"
THEN a biometric prompt is shown
  AND upon biometric success, a confirmation dialog appears
  AND upon confirmation, the entry is deleted
```

### AC-15: Delete password (secure mode entry — password re-auth)
```
GIVEN the user is on a secure mode entry's detail screen
  AND biometric is NOT available
WHEN the user taps "删除"
THEN a master password dialog appears
  AND upon entering the correct password, a confirmation dialog appears
  AND upon confirmation, the entry is deleted
```

### AC-16: Edit/delete hidden in global secure mode
```
GIVEN global Secure Mode is ON
WHEN the user views any entry's detail screen
THEN the Edit and Delete buttons are not rendered
  AND no edit or delete action is available
```

## 7.3 Auto-Lock & Session

### AC-17: Auto-lock on background (immediate mode)
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to -1 (immediate)
WHEN the app goes to background (Activity.onStop on Android)
THEN the system locks the session immediately
  AND wipes the DataKey from memory
  AND the state transitions to LOCKED
```

### AC-18: Auto-lock on background (timed mode)
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to 300,000 ms (5 minutes)
WHEN the app goes to background
  AND the app returns to foreground after 6 minutes
THEN the system detects elapsed time >= timeout
  AND locks the session
  AND forces navigation to the Login screen
```

### AC-19: Auto-lock NOT triggered within timeout
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to 300,000 ms (5 minutes)
WHEN the app goes to background
  AND returns to foreground after 2 minutes
THEN the session remains UNLOCKED
  AND lastActivityTime is refreshed
```

### AC-20: Never auto-lock mode
```
GIVEN the vault is UNLOCKED
  AND session timeout is set to 0 (never)
WHEN the app goes to background and returns after any duration
THEN the session remains UNLOCKED
```

### AC-21: Manual lock
```
GIVEN the vault is UNLOCKED
WHEN the user taps "Lock Now" in settings
THEN the system locks the session immediately
  AND navigates to the Login screen
```

## 7.4 Clipboard Security

### AC-22: Clipboard auto-clear
```
GIVEN the user copies a password to clipboard
WHEN 30 seconds elapse without another copy
THEN the clipboard is replaced with empty content
```

### AC-23: Clipboard timer reset on re-copy
```
GIVEN the user copies password A
  AND 15 seconds later copies password B
WHEN 30 seconds elapse after copying password B
THEN the clipboard is cleared
  AND password A's 15-second-old timer was cancelled
```

## 7.5 Master Password Change

### AC-24: Successful password change
```
GIVEN the vault is UNLOCKED
WHEN the user provides the correct current password and a new password
THEN the system derives a new PasswordKey from the new password
  AND re-encrypts the SAME DataKey with the new PasswordKey
  AND updates vault_config with new salt and encrypted DataKey
  AND wipes all ephemeral keys from memory
  AND the session remains UNLOCKED
  AND all existing entries remain accessible without re-encryption
```

### AC-25: Failed password change (wrong current password)
```
GIVEN the vault is UNLOCKED
WHEN the user provides an incorrect current password
THEN decryption of the DataKey fails
  AND the system displays "主密码错误"
  AND no config changes are persisted
  AND the session remains UNLOCKED (unchanged)
```

## 7.6 Security Mode Toggle

### AC-26: Enable secure mode (no auth required)
```
GIVEN Secure Mode is currently OFF
WHEN the user toggles Secure Mode ON in settings
THEN the system persists security_mode_enabled=true in vault_config
  AND all password visibility toggles are hidden
  AND edit/delete buttons are hidden on entry detail screens
```

### AC-27: Disable secure mode (auth required)
```
GIVEN Secure Mode is currently ON
WHEN the user toggles Secure Mode OFF
THEN the system requests biometric authentication (if available)
  AND upon biometric success (or correct master password fallback):
    - persists security_mode_enabled=false in vault_config
    - restores normal UI behavior
```

## 7.7 Attack Scenarios

### AC-28: Brute-force master password (no rate limiting)
```
GIVEN the vault is LOCKED
WHEN an attacker submits N incorrect passwords in rapid succession
THEN each attempt is processed independently
  AND each attempt wipes the password CharArray after derivation
  AND no application-layer lockout or delay is imposed
  AND Argon2id's computational cost (128 MB, 3 iterations) is the only brute-force resistance
```

### AC-29: Database theft (offline attack on encrypted entries)
```
GIVEN an attacker obtains a copy of the SQLite database
THEN the attacker can read: category, is_favorite, security_mode, timestamps, entry count
  AND all credential fields are encrypted with XChaCha20-Poly1305
  AND the DataKey is encrypted with Argon2id-derived key
  AND brute-forcing the master password requires Argon2id computation per attempt
  AND generated_passwords table contents are readable in plaintext
```

### AC-30: Memory dump attack (unlocked session)
```
GIVEN the vault is UNLOCKED
  AND an attacker obtains a memory dump of the process
THEN the DataKey is present in memory (inside SensitiveData wrapper)
  AND decrypted String fields from recently viewed entries are present (immutable strings cannot be wiped)
  AND DataKey copies returned to ViewModels may be present (not explicitly wiped)
```

### AC-31: Desktop device key extraction
```
GIVEN the attacker has read access to the desktop user's Java Preferences
THEN the attacker can read the XOR master key in plaintext
  AND the XOR-obfuscated DataKey in plaintext
  AND can trivially recover the DataKey by XORing the two values
  AND can decrypt all vault entries
```

### AC-32: Clipboard sniffing
```
GIVEN the user copies a password to clipboard
  AND a malicious app reads the clipboard within 30 seconds
THEN the malicious app obtains the password in plaintext
  AND after 30 seconds, the clipboard is replaced with empty content
```

### AC-33: Screenshot attack (Android)
```
GIVEN screenshot protection is enabled (default)
WHEN a screenshot or screen recording is attempted
THEN the system prevents capture via FLAG_SECURE
  AND the captured content shows a blank/black screen
```

### AC-34: Screenshot attack (Desktop)
```
GIVEN the app is running on desktop
WHEN a screenshot is taken
THEN the system does NOT prevent capture (no-op implementation)
  AND the screenshot contains the visible vault content
```

---

# 8. Test Coverage Summary

## 8.1 Critical Path Coverage Matrix

| Test Area | Priority | Covered | Gaps |
|-----------|----------|---------|------|
| Vault setup (T-1) | CRITICAL | Unit tests for KeyManager.setupVault | — |
| Password unlock (T-2) | CRITICAL | Unit tests for KeyManager.unlockWithPassword | No rate-limiting test (none exists) |
| Biometric unlock (T-3) | CRITICAL | Unit tests for KeyManager.unlockWithBiometric | Platform-specific prompt not testable in unit tests |
| Session lock (T-4, T-5, T-6) | CRITICAL | Unit tests for SessionManager lock/unlock/background/foreground | checkAutoLock() tested but never called in production |
| Field encryption | CRITICAL | Unit tests for AesGcmCipher encrypt/decrypt | — |
| Field decryption with wrong key | CRITICAL | Test expected | Verifies authentication tag rejection |
| Per-entry security mode encryption | HIGH | Unit tests for SecurityModeManager | — |
| Password change (key re-wrapping) | HIGH | Unit tests for KeyManager.changeMasterPassword | — |
| Memory wiping (MemorySanitizer) | HIGH | Unit tests for wipe/isWiped | JVM GC behavior not testable |
| Clipboard auto-clear | HIGH | Platform-dependent | Timing-based; difficult to unit test |
| Secure Mode UI restrictions | HIGH | UI test needed | Compose UI tests |
| Delete with re-authentication | HIGH | Integration test needed | Biometric mocking required |
| Search (client-side decryption) | MEDIUM | Unit tests for PasswordRepositoryImpl.search | — |
| Vault wipe (clear) | MEDIUM | Unit tests for KeyManager.clear | — |
| Generated password plaintext storage | LOW | Known design decision | — |

## 8.2 Platform-Specific Test Matrix

| Test | Android | Desktop | iOS |
|------|---------|---------|-----|
| PlatformKeyStore (hardware-backed) | Integration test with Android KeyStore | Unit test (XOR logic) | N/A |
| SecureClipboard auto-clear | Integration test | Integration test | N/A (stub) |
| BiometricAuth prompt | Manual test / Instrumentation | N/A (always NotAvailable) | N/A (stub) |
| ScreenSecurity FLAG_SECURE | Manual test | N/A (no-op) | N/A (stub) |
| Lifecycle auto-lock | Integration test with Activity lifecycle | **Untestable** (no lifecycle hooks) | N/A |

## 8.3 Required Test Scenarios Not Covered by Unit Tests

| # | Scenario | Type Needed | Reason |
|---|----------|-------------|--------|
| TS-1 | End-to-end: register → add entry → lock → unlock → verify entry readable | Integration | Validates full key lifecycle |
| TS-2 | Background lock timing accuracy | Integration | Requires real clock and lifecycle events |
| TS-3 | Clipboard cleared after 30 seconds | Integration | Requires real clipboard and timer |
| TS-4 | Secure Mode: password never visible in UI | UI Test | Requires Compose test assertions |
| TS-5 | Secure Mode: edit/delete require re-auth | UI Test | Requires biometric mock |
| TS-6 | Master password change followed by re-lock and re-unlock | Integration | Validates new password works |
| TS-7 | 100+ entries search performance | Performance | Measures linear decryption cost |
| TS-8 | Concurrent access from multiple coroutines | Stress | SessionManager thread safety |
| TS-9 | Database corruption recovery | Fault injection | SQLite file corruption handling |
| TS-10 | PlatformKeyStore invalidation after device lock change | Integration (Android) | KeyStore key may be invalidated |

## 8.4 Known Issues Requiring Resolution

| # | Issue | Severity | Category |
|---|-------|----------|----------|
| KI-1 | `CryptoUtils.generateSecureRandom()` uses `kotlin.random.Random.Default` (PRNG, not CSPRNG) | CRITICAL | Security |
| KI-2 | Desktop PlatformKeyStore uses XOR with plaintext key in Java Preferences | HIGH | Security |
| KI-3 | Argon2Kdf converts password CharArray to immutable String | HIGH | Security |
| KI-4 | `generated_passwords` table stores passwords in plaintext | HIGH | Security |
| KI-5 | `checkAutoLock()` has no periodic caller (foreground inactivity never triggers lock) | MEDIUM | Behavior |
| KI-6 | Desktop app has no lifecycle hooks (auto-lock never fires) | MEDIUM | Platform |
| KI-7 | `BiometricState` rate-limiter not wired into authentication flows | MEDIUM | Security |
| KI-8 | ViewModel callers of `getDataKey()` never wipe returned DataKey copies | MEDIUM | Security |
| KI-9 | Argon2 config stored in plaintext (downgrade attack surface) | LOW | Security |
| KI-10 | `SecurePadding` exists but is not integrated (ciphertext length leaks plaintext length) | LOW | Security |
| KI-11 | `SessionState.Error` and `KeyManagerState.Ready` are defined but never emitted | LOW | Code hygiene |
| KI-12 | No absolute session TTL when timeout is set to 0 (never) | LOW | Behavior |
| KI-13 | Autofill feature is a UI placeholder only — not implemented | LOW | Feature gap |
| KI-14 | iOS platform is entirely stubs — clipboard, biometric, keystore are no-ops | LOW | Platform |
