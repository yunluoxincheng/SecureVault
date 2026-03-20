# SecureVault — System-Level Test Cases (SLTC)

> Based on SRS v1.0 | Date: 2026-03-21
> Author: System Test Lead (SLTC)

---

## Coverage Summary

| Dimension | Count |
|-----------|-------|
| Total test cases | 165 |
| Modules covered | 11 / 11 |
| System states covered | 4 / 4 (NOT_SETUP, LOCKED, UNLOCKED, UNLOCKED+SECURE_MODE) |
| Core flows covered | 5 / 5 (Setup, Add, Retrieve, Delete, Password Change) |
| Normal scenarios | 58 |
| Error scenarios | 47 |
| Edge cases | 28 |
| Security scenarios | 32 |

---

## 1. Module: Key Derivation (Argon2id KDF)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| KDF-001 | Argon2id KDF | UNLOCKED | Normal: derive key with default config | libsodium initialized; valid password CharArray; 16-byte salt; config = (128 MB, 3 iter, 4 par, 32 out) | 1. Call `deriveKey(password, salt, config)` | Returns 32-byte derived key. Password CharArray is wiped (all zeros). | CRITICAL |
| KDF-002 | Argon2id KDF | UNLOCKED | Normal: same inputs produce same output | Same password, salt, config as KDF-001 | 1. Call `deriveKey` with identical inputs twice | Both calls return identical 32-byte arrays | CRITICAL |
| KDF-003 | Argon2id KDF | UNLOCKED | Normal: different passwords produce different keys | Two distinct passwords; same salt and config | 1. Derive key with password A 2. Derive key with password B | Returned keys differ | CRITICAL |
| KDF-004 | Argon2id KDF | UNLOCKED | Normal: different salts produce different keys | Same password; two distinct 16-byte salts; same config | 1. Derive key with salt A 2. Derive key with salt B | Returned keys differ | HIGH |
| KDF-005 | Argon2id KDF | UNLOCKED | Error: libsodium not initialized | libsodium NOT initialized | 1. Call `deriveKey(password, salt, config)` | Exception thrown. Password CharArray is still wiped in `finally` block. | HIGH |
| KDF-006 | Argon2id KDF | UNLOCKED | Security: password CharArray wiped after successful derivation | Valid inputs | 1. Create CharArray password 2. Call `deriveKey` 3. Inspect password array after call | Every element of the password CharArray == `\u0000` | CRITICAL |
| KDF-007 | Argon2id KDF | UNLOCKED | Security: password CharArray wiped after failed derivation | Invalid config causing exception | 1. Call `deriveKey` with bad config 2. Inspect password array after exception | Every element of the password CharArray == `\u0000` | CRITICAL |
| KDF-008 | Argon2id KDF | UNLOCKED | Edge: single-character password | Password = CharArray(['a']); valid salt + config | 1. Call `deriveKey` | Returns 32-byte key without error | LOW |
| KDF-009 | Argon2id KDF | UNLOCKED | Edge: Unicode password (CJK characters) | Password = CharArray of Chinese characters; valid salt + config | 1. Call `deriveKey` | Returns 32-byte key without error | LOW |
| KDF-010 | Argon2id KDF | UNLOCKED | Edge: minimum Argon2 config | config = (8192 KB, 1 iter, 1 par, 32 out) | 1. Call `deriveKey` | Returns 32-byte key. Derivation completes faster than default config. | MEDIUM |

## 2. Module: Authenticated Encryption (XChaCha20-Poly1305)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| ENC-001 | XChaCha20-Poly1305 | UNLOCKED | Normal: encrypt then decrypt roundtrip | libsodium initialized; valid 32-byte key; plaintext = "hello" bytes | 1. Encrypt plaintext with key 2. Decrypt result with same key | Decrypted output == original plaintext | CRITICAL |
| ENC-002 | XChaCha20-Poly1305 | UNLOCKED | Normal: each encryption produces unique ciphertext | Same plaintext and key | 1. Encrypt same plaintext twice with same key | Two EncryptedData objects have different `iv` fields and different `ciphertext` fields (due to random nonce) | CRITICAL |
| ENC-003 | XChaCha20-Poly1305 | UNLOCKED | Normal: EncryptedData structure is valid | Valid plaintext and key | 1. Encrypt plaintext 2. Inspect returned EncryptedData | `version == "v2"`, `iv.size == 24`, `tag.size == 16`, `ciphertext.isNotEmpty()` | HIGH |
| ENC-004 | XChaCha20-Poly1305 | UNLOCKED | Normal: storage format serialization roundtrip | Valid EncryptedData object | 1. Call `toStorageFormat()` 2. Call `fromStorageFormat()` on result | Reconstructed EncryptedData has identical `version`, `iv`, `ciphertext`, `tag` | HIGH |
| ENC-005 | XChaCha20-Poly1305 | UNLOCKED | Normal: encryptToStorageFormat + decryptFromStorageFormat | Valid plaintext and key | 1. `encryptToStorageFormat(plaintext, key)` 2. `decryptFromStorageFormat(result, key)` | Decrypted bytes == original plaintext bytes | HIGH |
| ENC-006 | XChaCha20-Poly1305 | UNLOCKED | Error: key size < 32 bytes | Key = 16-byte array | 1. Call `encrypt(plaintext, shortKey)` | `IllegalArgumentException` thrown with message containing "Key must be 32 bytes" | CRITICAL |
| ENC-007 | XChaCha20-Poly1305 | UNLOCKED | Error: key size > 32 bytes | Key = 64-byte array | 1. Call `encrypt(plaintext, longKey)` | `IllegalArgumentException` thrown | HIGH |
| ENC-008 | XChaCha20-Poly1305 | UNLOCKED | Error: empty plaintext | Plaintext = empty ByteArray; valid key | 1. Call `encrypt(emptyArray, key)` | `IllegalArgumentException` with message "Plaintext cannot be empty" | HIGH |
| ENC-009 | XChaCha20-Poly1305 | UNLOCKED | Security: decrypt with wrong key | Encrypted with key A | 1. Encrypt with key A 2. Decrypt with key B (different 32-byte key) | libsodium throws authentication failure exception. No plaintext returned. | CRITICAL |
| ENC-010 | XChaCha20-Poly1305 | UNLOCKED | Security: tampered ciphertext detected | Valid EncryptedData | 1. Encrypt plaintext 2. Flip one bit in `ciphertext` array 3. Attempt decrypt | Authentication failure. Exception thrown. No plaintext returned. | CRITICAL |
| ENC-011 | XChaCha20-Poly1305 | UNLOCKED | Security: tampered tag detected | Valid EncryptedData | 1. Encrypt plaintext 2. Flip one bit in `tag` array 3. Attempt decrypt | Authentication failure. Exception thrown. | CRITICAL |
| ENC-012 | XChaCha20-Poly1305 | UNLOCKED | Security: tampered IV detected | Valid EncryptedData | 1. Encrypt plaintext 2. Modify one byte in `iv` 3. Attempt decrypt | Authentication failure. Exception thrown. | CRITICAL |
| ENC-013 | XChaCha20-Poly1305 | UNLOCKED | Error: unsupported version string | EncryptedData with version = "v1" | 1. Construct EncryptedData(version="v1", ...) 2. Call `decrypt` | `IllegalArgumentException` with message "Unsupported version: v1" | MEDIUM |
| ENC-014 | XChaCha20-Poly1305 | UNLOCKED | Error: invalid nonce size | EncryptedData with iv.size = 12 (not 24) | 1. Construct EncryptedData(iv=12 bytes) 2. Call `decrypt` | `IllegalArgumentException` with message about invalid nonce size | MEDIUM |
| ENC-015 | XChaCha20-Poly1305 | UNLOCKED | Error: invalid tag size | EncryptedData with tag.size = 8 (not 16) | 1. Construct EncryptedData(tag=8 bytes) 2. Call `decrypt` | `IllegalArgumentException` with message about invalid tag size | MEDIUM |
| ENC-016 | XChaCha20-Poly1305 | UNLOCKED | Edge: large plaintext (1 MB) | 1 MB byte array; valid key | 1. Encrypt 2. Decrypt | Roundtrip succeeds. Decrypted == original. | LOW |
| ENC-017 | XChaCha20-Poly1305 | UNLOCKED | Error: malformed storage format JSON | Invalid JSON string | 1. Call `decryptFromStorageFormat("{invalid", key)` | JSON parse exception thrown | MEDIUM |
| ENC-018 | XChaCha20-Poly1305 | UNLOCKED | Error: invalid base64 in storage format | JSON with invalid base64 for iv field | 1. Call `decryptFromStorageFormat` with bad base64 | Base64 decode exception thrown | MEDIUM |

## 3. Module: Session Manager

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| SES-001 | SessionManager | LOCKED | Normal: unlock loads DataKey | SessionManager in locked state; valid 32-byte DataKey | 1. Call `unlock(dataKey)` 2. Check `isUnlocked()` 3. Check `sessionState` | `isUnlocked() == true`. `sessionState == Unlocked`. `getDataKey()` returns data matching original. | CRITICAL |
| SES-002 | SessionManager | UNLOCKED | Normal: lock wipes DataKey | Session unlocked with a known DataKey | 1. Call `lock()` 2. Check `isUnlocked()` 3. Check `sessionState` | `isUnlocked() == false`. `sessionState == Locked`. | CRITICAL |
| SES-003 | SessionManager | UNLOCKED | Normal: getDataKey returns copy | Session unlocked | 1. Call `getDataKey()` twice | Two returned arrays have equal content but are different object references | HIGH |
| SES-004 | SessionManager | LOCKED | Error: getDataKey while locked | Session locked | 1. Call `getDataKey()` | `IllegalStateException("Session is locked")` thrown | CRITICAL |
| SES-005 | SessionManager | UNLOCKED | Normal: getDataKey refreshes lastActivityTime | Session unlocked; note current time | 1. Wait 100ms 2. Call `getDataKey()` 3. Call `checkAutoLock()` with timeout > 100ms | `checkAutoLock()` returns false (session extended by `getDataKey`) | HIGH |
| SES-006 | SessionManager | UNLOCKED | Normal: onAppBackground with immediate lock | Session unlocked; `lockTimeoutMs = -1` | 1. Call `onAppBackground()` | Returns true. `isUnlocked() == false`. `sessionState == Locked`. | HIGH |
| SES-007 | SessionManager | UNLOCKED | Normal: onAppBackground with non-immediate timeout | Session unlocked; `lockTimeoutMs = 300000` | 1. Call `onAppBackground()` | Returns false. `isUnlocked() == true`. `backgroundEnteredAtMs` is recorded. | HIGH |
| SES-008 | SessionManager | UNLOCKED | Normal: onAppForeground within timeout | Background recorded; elapsed < timeout | 1. Call `onAppBackground()` 2. Wait < timeout 3. Call `onAppForeground()` | Returns false. Session remains unlocked. `lastActivityTime` refreshed. | HIGH |
| SES-009 | SessionManager | UNLOCKED | Normal: onAppForeground exceeds timeout | Background recorded; `lockTimeoutMs = 1000` | 1. Call `onAppBackground()` 2. Wait >= 1000ms 3. Call `onAppForeground()` | Returns true. Session locked. DataKey wiped. | HIGH |
| SES-010 | SessionManager | UNLOCKED | Normal: checkAutoLock within timeout | Session unlocked; `lockTimeoutMs = 300000`; recent activity | 1. Call `checkAutoLock()` | Returns false. Session remains unlocked. | MEDIUM |
| SES-011 | SessionManager | UNLOCKED | Normal: checkAutoLock exceeds timeout | Session unlocked; `lockTimeoutMs = 100`; lastActivity > 100ms ago | 1. Wait > 100ms without calling getDataKey 2. Call `checkAutoLock()` | Returns true. Session locked. | MEDIUM |
| SES-012 | SessionManager | UNLOCKED | Edge: timeout = 0 (never auto-lock) | `lockTimeoutMs = 0` | 1. Call `onAppBackground()` 2. Wait any duration 3. Call `onAppForeground()` 4. Call `checkAutoLock()` | All return false. Session never auto-locks. | MEDIUM |
| SES-013 | SessionManager | UNLOCKED | Edge: re-unlock replaces previous key | Unlock with keyA; then unlock with keyB | 1. `unlock(keyA)` 2. `unlock(keyB)` 3. `getDataKey()` | Returned key matches keyB. keyA's SensitiveData wrapper was closed (wiped). | CRITICAL |
| SES-014 | SessionManager | LOCKED | Normal: onAppBackground while already locked | Session locked | 1. Call `onAppBackground()` | Returns false. No state change. | LOW |
| SES-015 | SessionManager | UNLOCKED | Security: DataKey wiped on lock | Capture DataKey reference before lock | 1. `unlock(key)` 2. Get internal reference 3. `lock()` 4. Inspect underlying byte array | Byte array all zeros (wiped by MemorySanitizer) | CRITICAL |

## 4. Module: Platform Key Store

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| PKS-001 | PlatformKeyStore | UNLOCKED | Normal: store and retrieve DataKey | Empty PlatformKeyStore | 1. `storeDeviceKey(key)` 2. `getDeviceKey()` | Retrieved key matches original key | CRITICAL |
| PKS-002 | PlatformKeyStore | ANY | Normal: hasDeviceKey after store | Key stored | 1. `storeDeviceKey(key)` 2. `hasDeviceKey()` | Returns true | HIGH |
| PKS-003 | PlatformKeyStore | ANY | Normal: hasDeviceKey before store | Empty store | 1. `hasDeviceKey()` | Returns false | HIGH |
| PKS-004 | PlatformKeyStore | ANY | Normal: deleteDeviceKey | Key stored | 1. `deleteDeviceKey()` 2. `hasDeviceKey()` 3. `getDeviceKey()` | `hasDeviceKey() == false`. `getDeviceKey() == null`. | HIGH |
| PKS-005 | PlatformKeyStore | ANY | Normal: overwrite existing key | Key A stored | 1. `storeDeviceKey(keyA)` 2. `storeDeviceKey(keyB)` 3. `getDeviceKey()` | Retrieved key matches keyB | HIGH |
| PKS-006 | PlatformKeyStore | ANY | Error: getDeviceKey when none stored | Empty store | 1. `getDeviceKey()` | Returns null | MEDIUM |
| PKS-007 | PlatformKeyStore | ANY | Edge (Android): isHardwareBacked | Android device with TEE | 1. `isHardwareBacked()` | Returns true on devices with TEE/StrongBox, false otherwise | MEDIUM |
| PKS-008 | PlatformKeyStore | ANY | Security (Desktop): XOR key recoverable | Desktop platform; key stored | 1. Read Java Preferences directly 2. Extract XOR master key and encrypted blob 3. XOR them | Recovered bytes match original DataKey (demonstrates weakness) | CRITICAL |

## 5. Module: Security Mode Manager

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| SMM-001 | SecurityModeManager | UNLOCKED | Normal: encrypt password with securityMode=false | Valid DataKey; `securityMode = false` | 1. Call `encryptPasswordForStorage("secret", dataKey, false)` | Returns encrypted JSON string. Can be decrypted with DataKey. | HIGH |
| SMM-002 | SecurityModeManager | UNLOCKED | Normal: encrypt password with securityMode=true | Valid DataKey; `securityMode = true`; no SecureModeKey exists yet | 1. Call `encryptPasswordForStorage("secret", dataKey, true)` | SecureModeKey is generated and stored encrypted in vault_config. Password encrypted with SecureModeKey. | CRITICAL |
| SMM-003 | SecurityModeManager | UNLOCKED | Normal: decrypt password with securityMode=true | Entry encrypted with securityMode=true | 1. Encrypt with securityMode=true 2. Call `decryptPasswordForRead(encrypted, dataKey, true)` | Returns original plaintext "secret" | CRITICAL |
| SMM-004 | SecurityModeManager | UNLOCKED | Security: SecureModeKey wiped after encrypt | Valid inputs; securityMode=true | 1. Call `encryptPasswordForStorage` with securityMode=true 2. Inspect secureModeKey after call | secureModeKey is wiped (all zeros) — verified by `finally` block execution | CRITICAL |
| SMM-005 | SecurityModeManager | UNLOCKED | Security: SecureModeKey wiped after decrypt | Valid inputs; securityMode=true | 1. Call `decryptPasswordForRead` with securityMode=true 2. Inspect secureModeKey | secureModeKey is wiped (all zeros) | CRITICAL |
| SMM-006 | SecurityModeManager | UNLOCKED | Normal: usePassword copies to clipboard and wipes | Valid encrypted entry; mock SecureClipboard | 1. Call `usePassword(encrypted, dataKey, true)` | `SecureClipboard.copy()` called with correct plaintext. `scheduleAutoClear()` called. Decrypted bytes wiped (all zeros). | CRITICAL |
| SMM-007 | SecurityModeManager | UNLOCKED | Security: usePassword wipes bytes on clipboard failure | SecureClipboard.copy() throws | 1. Call `usePassword` with mock clipboard that throws | Decrypted bytes are still wiped in `finally` block | CRITICAL |
| SMM-008 | SecurityModeManager | UNLOCKED | Normal: SecureModeKey reused across calls | SecureModeKey already exists in vault_config | 1. Encrypt with securityMode=true 2. Encrypt another password with securityMode=true | Both use the same SecureModeKey (retrieved from config, not regenerated). Both can be decrypted. | HIGH |
| SMM-009 | SecurityModeManager | UNLOCKED | Error: decrypt securityMode password with wrong DataKey | Password encrypted with dataKeyA | 1. Attempt `decryptPasswordForRead(encrypted, dataKeyB, true)` | Exception thrown (cannot decrypt SecureModeKey with wrong DataKey) | CRITICAL |
| SMM-010 | SecurityModeManager | UNLOCKED | Normal: isEnabled / setEnabled toggle | Config repo accessible | 1. `isEnabled()` → false 2. `setEnabled(true)` 3. `isEnabled()` → true 4. `setEnabled(false)` 5. `isEnabled()` → false | Each call returns the persisted value | MEDIUM |
| SMM-011 | SecurityModeManager | UNLOCKED | Edge: securityMode=true entry still uses SecureModeKey after global toggle off | Entry created with securityMode=true; then global toggle disabled | 1. Create entry with securityMode=true 2. Disable global SecurityMode 3. Decrypt the entry with securityMode=true flag | Decryption uses SecureModeKey (per-entry flag governs encryption key selection, not global toggle) | HIGH |

## 6. Module: Secure Clipboard

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| CLIP-001 | SecureClipboard | UNLOCKED | Normal: copy text to clipboard | Platform clipboard available | 1. Call `copy("myPassword", "Password")` | System clipboard contains "myPassword" | HIGH |
| CLIP-002 | SecureClipboard | UNLOCKED | Normal: auto-clear after 30 seconds | Text copied to clipboard | 1. Call `copy("secret", "Password")` 2. Call `scheduleAutoClear()` 3. Wait 30+ seconds | Clipboard content replaced with empty string | CRITICAL |
| CLIP-003 | SecureClipboard | UNLOCKED | Normal: clear immediately | Text on clipboard | 1. Call `copy("secret", "Password")` 2. Call `clear()` | Clipboard content replaced with empty string immediately | HIGH |
| CLIP-004 | SecureClipboard | UNLOCKED | Normal: re-copy resets timer | First copy done | 1. `copy("passwordA")` + `scheduleAutoClear()` 2. Wait 15 seconds 3. `copy("passwordB")` + `scheduleAutoClear()` 4. Wait 20 seconds | Clipboard contains "passwordB" (not cleared yet — only 20s of 30s elapsed since last copy) | CRITICAL |
| CLIP-005 | SecureClipboard | UNLOCKED | Normal: re-copy clears after full 30s from second copy | Continuing from CLIP-004 | 5. Wait additional 10 seconds (total 30s from second copy) | Clipboard is empty | CRITICAL |
| CLIP-006 | SecureClipboard | UNLOCKED | Security: clipboard not cleared before 30 seconds | Text copied | 1. `copy("secret")` + `scheduleAutoClear()` 2. Read clipboard at 29 seconds | Clipboard still contains "secret" | HIGH |
| CLIP-007 | SecureClipboard | UNLOCKED | Edge (iOS): copy is no-op | iOS platform | 1. Call `copy("test")` 2. Read iOS clipboard | iOS clipboard unchanged (method is a stub no-op) | MEDIUM |
| CLIP-008 | SecureClipboard | UNLOCKED | Edge (iOS): scheduleAutoClear is no-op | iOS platform | 1. Call `scheduleAutoClear()` | No crash. No timer scheduled. | MEDIUM |
| CLIP-009 | SecureClipboard | UNLOCKED | Edge: multiple rapid copies | — | 1. `copy("a")` + `scheduleAutoClear()` 2. Immediately `copy("b")` + `scheduleAutoClear()` 3. Immediately `copy("c")` + `scheduleAutoClear()` | Clipboard contains "c". Only one timer active. Timer fires 30s after "c" copy. | HIGH |

## 7. Module: Memory Sanitizer

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| MEM-001 | MemorySanitizer | ANY | Normal: wipe ByteArray | ByteArray of 32 bytes with non-zero values | 1. Call `wipe(data)` 2. Call `isWiped(data)` | `isWiped()` returns true. Every byte == 0. | CRITICAL |
| MEM-002 | MemorySanitizer | ANY | Normal: wipe CharArray | CharArray with characters | 1. Call `wipe(data)` 2. Call `isWiped(data)` | `isWiped()` returns true. Every char == `\u0000`. | CRITICAL |
| MEM-003 | MemorySanitizer | ANY | Normal: wipe IntArray | IntArray with non-zero values | 1. Call `wipe(data)` | Every int == 0 | HIGH |
| MEM-004 | MemorySanitizer | ANY | Normal: multi-pass overwrite | ByteArray; passes = 3 | 1. During wipe, verify intermediate state between passes | Pass 0: all bytes = 0x00. Pass 1: all bytes = 0x01. Pass 2: all bytes = 0x02. Final: all bytes = 0x00. | HIGH |
| MEM-005 | MemorySanitizer | ANY | Edge: wipe empty array | ByteArray of size 0 | 1. Call `wipe(emptyByteArray)` | No crash. `isWiped()` returns true. | LOW |
| MEM-006 | MemorySanitizer | ANY | Edge: wipe with passes = 1 | ByteArray; passes = 1 | 1. Call `wipe(data, passes = 1)` | All bytes overwritten once then zeroed. `isWiped()` returns true. | LOW |
| MEM-007 | MemorySanitizer | ANY | Error: wipe with passes = 0 | ByteArray; passes = 0 | 1. Call `wipe(data, passes = 0)` | `IllegalArgumentException("Passes must be positive")` | LOW |
| MEM-008 | MemorySanitizer | ANY | Normal: SensitiveData.ofByteArray stores copy | 32-byte array | 1. Create `SensitiveData.ofByteArray(original)` 2. Modify `original[0]` 3. Call `get()` | Returned data != modified original (copy is independent) | HIGH |
| MEM-009 | MemorySanitizer | ANY | Normal: SensitiveData.close wipes data | SensitiveData created | 1. `SensitiveData.ofByteArray(key)` 2. `close()` 3. `isAvailable` | `isAvailable == false`. Underlying array wiped. | CRITICAL |
| MEM-010 | MemorySanitizer | ANY | Error: SensitiveData.get after close | SensitiveData closed | 1. Create and close SensitiveData 2. Call `get()` | `IllegalStateException("SensitiveData has been closed")` | HIGH |
| MEM-011 | MemorySanitizer | ANY | Edge: SensitiveData.close called twice | SensitiveData created | 1. `close()` 2. `close()` | No crash. Second close is idempotent. | LOW |

## 8. Module: Biometric Authentication

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| BIO-001 | BiometricAuth | LOCKED | Normal: successful biometric unlock | Android; biometric enrolled; DeviceKey in PlatformKeyStore | 1. Call `authenticate("Title", "Subtitle")` 2. User scans fingerprint successfully | Returns `BiometricResult.Success` | HIGH |
| BIO-002 | BiometricAuth | LOCKED | Error: biometric authentication fails | Android; wrong fingerprint | 1. Call `authenticate` 2. User presents wrong fingerprint | Returns `BiometricResult.Failed` | HIGH |
| BIO-003 | BiometricAuth | LOCKED | Normal: user cancels biometric prompt | Android | 1. Call `authenticate` 2. User taps cancel | Returns `BiometricResult.Cancelled` | MEDIUM |
| BIO-004 | BiometricAuth | ANY | Normal: biometric not available on Desktop | Desktop platform | 1. Call `isAvailable()` | Returns false | MEDIUM |
| BIO-005 | BiometricAuth | ANY | Normal: authenticate returns NotAvailable on Desktop | Desktop platform | 1. Call `authenticate("Title", "Sub")` | Returns `BiometricResult.NotAvailable` | MEDIUM |
| BIO-006 | BiometricAuth | LOCKED | Edge: BiometricState rate-limiter (not wired) | BiometricState instance | 1. Call `recordFailure()` 5 times 2. Call `isLockedOut()` 3. Call `canAuthenticate()` | `isLockedOut() == true`. `canAuthenticate() == false`. `getRemainingAttempts() == 0`. Note: this class is not wired into actual auth flows. | HIGH |
| BIO-007 | BiometricAuth | LOCKED | Edge: BiometricState lockout expires | BiometricState locked out | 1. Lock out BiometricState 2. Wait 30 seconds 3. Call `isLockedOut()` | Returns false. Counter reset. | HIGH |
| BIO-008 | BiometricAuth | LOCKED | Normal: BiometricState debounce | BiometricState with debounce = 500ms | 1. Record success 2. Immediately call `canAuthenticate()` | Returns false (within debounce window) | MEDIUM |
| BIO-009 | BiometricAuth | LOCKED | Security: repeated biometric failures (no app-level lockout) | Android; 10+ wrong fingerprints | 1. Call `authenticate()` 10 times, each returning Failed | Each call returns `BiometricResult.Failed`. No application-level lockout. OS may impose its own lockout. | HIGH |

## 9. Module: Password Repository

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| REPO-001 | PasswordRepository | UNLOCKED | Normal: create and retrieve entry | Vault unlocked; valid DataKey | 1. Create entry (title="Test", username="user", password="pass") 2. Retrieve by returned ID | Retrieved entry has matching title, username, password after decryption | CRITICAL |
| REPO-002 | PasswordRepository | UNLOCKED | Normal: all fields encrypted in database | Entry created | 1. Create entry 2. Read raw database row directly (bypass decryption) | `encrypted_title`, `encrypted_username`, `encrypted_password` contain JSON with `version`, `iv`, `ciphertext`, `tag` keys. None contain plaintext. | CRITICAL |
| REPO-003 | PasswordRepository | UNLOCKED | Normal: metadata stored in plaintext | Entry created with category="social", isFavorite=true | 1. Create entry 2. Read raw database row | `category == "social"`, `is_favorite == 1`, `security_mode == 0` in plaintext. `created_at` and `updated_at` are plaintext integers. | MEDIUM |
| REPO-004 | PasswordRepository | UNLOCKED | Normal: update re-encrypts with fresh nonces | Entry exists | 1. Read raw row, note IV 2. Update entry 3. Read raw row again | IV has changed (each encryption uses fresh random nonce) | HIGH |
| REPO-005 | PasswordRepository | UNLOCKED | Normal: delete removes row | Entry exists with known ID | 1. `deleteById(id)` 2. `getById(id, dataKey)` | Returns null. Row no longer in database. | HIGH |
| REPO-006 | PasswordRepository | UNLOCKED | Normal: getAll returns all entries decrypted | 3 entries exist | 1. `getAll(dataKey)` | Returns list of 3 PasswordEntry objects with all plaintext fields | HIGH |
| REPO-007 | PasswordRepository | UNLOCKED | Normal: search by title substring | Entry with title="GitHub Account" exists | 1. `search("github", PasswordFilter(), dataKey)` | Returns list containing the entry (case-insensitive match) | HIGH |
| REPO-008 | PasswordRepository | UNLOCKED | Normal: search by username | Entry with username="john@example.com" | 1. `search("john", PasswordFilter(), dataKey)` | Returns list containing the entry | MEDIUM |
| REPO-009 | PasswordRepository | UNLOCKED | Normal: search with category filter | Entries in "social" and "work" categories | 1. `search("", PasswordFilter(category="social"), dataKey)` | Returns only entries with category == "social" | MEDIUM |
| REPO-010 | PasswordRepository | UNLOCKED | Normal: search with favorites filter | Mix of favorite and non-favorite entries | 1. `search("", PasswordFilter(onlyFavorites=true), dataKey)` | Returns only entries with `isFavorite == true` | MEDIUM |
| REPO-011 | PasswordRepository | UNLOCKED | Error: getById with non-existent ID | Empty database | 1. `getById(999, dataKey)` | Returns null | MEDIUM |
| REPO-012 | PasswordRepository | UNLOCKED | Error: update with null ID | PasswordEntry with id=null | 1. `update(entry, dataKey)` | Returns false. No database change. | MEDIUM |
| REPO-013 | PasswordRepository | UNLOCKED | Normal: clear deletes all entries | 5 entries exist | 1. `clear()` 2. `getAll(dataKey)` | Returns empty list | MEDIUM |
| REPO-014 | PasswordRepository | UNLOCKED | Security: securityMode entry uses SecureModeKey | DataKey and SecurityModeManager available | 1. Create entry with securityMode=true 2. Read raw encrypted_password column 3. Attempt decrypt with DataKey directly | DataKey decryption fails (password encrypted with SecureModeKey, not DataKey) | CRITICAL |
| REPO-015 | PasswordRepository | UNLOCKED | Normal: getPasswordCipherById returns raw cipher | Entry with securityMode=true exists | 1. `getPasswordCipherById(id)` | Returns PasswordCipherPayload with encrypted string and securityMode=true, without decrypting | HIGH |
| REPO-016 | PasswordRepository | UNLOCKED | Normal: nullable fields stored as NULL | Entry with url=null, notes=null | 1. Create entry 2. Read raw row | `encrypted_url IS NULL`, `encrypted_notes IS NULL` | LOW |
| REPO-017 | PasswordRepository | UNLOCKED | Normal: tags encrypted as JSON array | Entry with tags=["work", "important"] | 1. Create entry 2. Retrieve entry | `entry.tags == ["work", "important"]` | MEDIUM |
| REPO-018 | PasswordRepository | UNLOCKED | Edge: search with empty query and no filter | 10 entries exist | 1. `search("", PasswordFilter(), dataKey)` | Returns all 10 entries (no filtering applied) | LOW |

## 10. Module: Screenshot Protection

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| SS-001 | ScreenSecurity | ANY | Normal (Android): screenshot protection enabled by default | Android; app just started | 1. Inspect window flags after SettingsViewModel init | FLAG_SECURE is set on window | HIGH |
| SS-002 | ScreenSecurity | UNLOCKED | Normal (Android): disable screenshot protection | Settings: screenshotAllowed = false initially | 1. Call `updateScreenshotAllowed(true)` | FLAG_SECURE removed from window. Config persisted as `screenshot_allowed=true`. | HIGH |
| SS-003 | ScreenSecurity | UNLOCKED | Normal (Android): re-enable screenshot protection | screenshotAllowed = true | 1. Call `updateScreenshotAllowed(false)` | FLAG_SECURE set on window. Config persisted as `screenshot_allowed=false`. | HIGH |
| SS-004 | ScreenSecurity | ANY | Edge (Desktop): all calls are no-ops | Desktop platform | 1. Call `enableScreenshotProtection()` 2. Call `disableScreenshotProtection()` | No crash. No effect. | LOW |

## 11. Module: Password Generator

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| GEN-001 | PasswordGenerator | UNLOCKED | Normal: generate strong password | GeneratorViewModel available | 1. Call `generateWithPreset(PasswordPreset.Strong)` | Non-empty password string returned | MEDIUM |
| GEN-002 | PasswordGenerator | UNLOCKED | Normal: copy generated password to clipboard | Password generated | 1. Generate password 2. Call `copyGeneratedPassword()` | Password on clipboard. Auto-clear timer started. | HIGH |
| GEN-003 | PasswordGenerator | UNLOCKED | Security: generated passwords stored in plaintext | Generate a password | 1. Generate password 2. Read raw `generated_passwords` table | `password` column contains plaintext password string | HIGH |
| GEN-004 | PasswordGenerator | UNLOCKED | Normal: recent generated passwords limited to 20 | 25 passwords generated | 1. Generate 25 passwords 2. Query `selectRecentGeneratedPasswords` | Returns exactly 20 most recent passwords | LOW |

---

## 12. Core Flow: Vault Setup (5+ tests)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| FLOW-SETUP-001 | KeyManager | NOT_SETUP | Normal: first-time vault creation | No vault config in database; app on Register screen | 1. User enters master password "MyStr0ng!Pass" 2. Taps register | State transitions NOT_SETUP → UNLOCKED. vault_config contains salt, encrypted DataKey, Argon2 params. PlatformKeyStore has DeviceKey. User navigated to Vault screen. | CRITICAL |
| FLOW-SETUP-002 | KeyManager | NOT_SETUP | Normal: DataKey is encrypted in vault_config | Setup completed | 1. Read `encrypted_data_key_password` from vault_config 2. Parse JSON | Valid EncryptedData JSON with version "v2", base64 iv, ciphertext, tag. Not plaintext. | CRITICAL |
| FLOW-SETUP-003 | KeyManager | NOT_SETUP | Normal: PasswordKey and password wiped after setup | Intercept memory post-setup | 1. Complete setup 2. Inspect original password CharArray 3. Inspect PasswordKey ByteArray | Both are wiped (all zeros) | CRITICAL |
| FLOW-SETUP-004 | KeyManager | NOT_SETUP | Error: setup when vault already exists | Vault config already present | 1. Call `setupVault(password)` | Returns `KeyManagerResult.Error(VaultAlreadySetup)`. No new keys generated. No config modified. | HIGH |
| FLOW-SETUP-005 | KeyManager | NOT_SETUP | Normal: session is unlocked after setup | — | 1. Complete setup 2. Check `keyManager.state` 3. Check `sessionManager.isUnlocked()` | `state == KeyManagerState.Unlocked`. `isUnlocked() == true`. `getDataKey()` returns valid key. | HIGH |
| FLOW-SETUP-006 | KeyManager | NOT_SETUP | Normal: Argon2 params persisted correctly | — | 1. Complete setup 2. Read Argon2 params from vault_config | `argon2_memory_kb == "131072"`, `argon2_iterations == "3"`, `argon2_parallelism == "4"`, `argon2_output_length == "32"` | MEDIUM |
| FLOW-SETUP-007 | KeyManager | NOT_SETUP | Error: crypto failure during setup | libsodium not initialized (mock) | 1. Call `setupVault(password)` | Returns `KeyManagerResult.Error(CryptoError(...))`. Password still wiped. No partial config persisted. | HIGH |
| FLOW-SETUP-008 | KeyManager | NOT_SETUP | Normal: onboarding → register → vault screen | First launch; onboarding not completed | 1. Complete onboarding 2. Enter password on Register 3. Submit | Navigation: Onboarding → Register → Vault. Config: onboarding_completed=true, vault_setup_completed=true. | MEDIUM |

## 13. Core Flow: Vault Unlock (5+ tests)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| FLOW-UNLOCK-001 | KeyManager | LOCKED | Normal: unlock with correct password | Vault exists; state == LOCKED | 1. Enter correct master password 2. Submit | State transitions LOCKED → UNLOCKED. DataKey loaded into session. Navigate to Vault screen. PasswordKey and password wiped. | CRITICAL |
| FLOW-UNLOCK-002 | KeyManager | LOCKED | Error: unlock with incorrect password | Vault exists | 1. Enter wrong password 2. Submit | Returns `KeyManagerResult.Error(InvalidPassword)`. UI shows "主密码错误". State remains LOCKED. Password CharArray wiped. | CRITICAL |
| FLOW-UNLOCK-003 | KeyManager | LOCKED | Security: repeated incorrect password attempts | Vault exists | 1. Submit wrong password 10 times rapidly | Each attempt returns InvalidPassword. No lockout. No delay. Password wiped each time. Argon2id computation cost is the only throttle. | CRITICAL |
| FLOW-UNLOCK-004 | KeyManager | LOCKED | Normal: biometric unlock | Biometric enabled; DeviceKey in store | 1. Initiate biometric unlock 2. Successful biometric prompt | State transitions LOCKED → UNLOCKED. DataKey retrieved from PlatformKeyStore and loaded. | HIGH |
| FLOW-UNLOCK-005 | KeyManager | LOCKED | Error: biometric unlock without prior password login | No DeviceKey in PlatformKeyStore | 1. Initiate biometric unlock | Returns `Error(BiometricNotEnrolled)`. UI shows "尚未准备生物识别解锁，请先用主密码登录一次". | HIGH |
| FLOW-UNLOCK-006 | KeyManager | LOCKED | Error: unlock when vault not setup | No vault_config entries | 1. Call `unlockWithPassword(password)` | Returns `Error(VaultNotSetup)`. UI shows "未检测到已注册保险库，请先注册". | HIGH |
| FLOW-UNLOCK-007 | KeyManager | LOCKED | Normal: PlatformKeyStore updated on password unlock | Vault exists | 1. Unlock with password | `platformKeyStore.storeDeviceKey(dataKey)` called, refreshing the biometric path | MEDIUM |
| FLOW-UNLOCK-008 | KeyManager | LOCKED | Edge: biometric setting disabled, hardware available | Biometric hardware present; setting = false | 1. Call `unlockWithBiometric()` | UI shows "请先在设置中开启生物识别". Biometric prompt not shown. | MEDIUM |

## 14. Core Flow: Add Password (5+ tests)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| FLOW-ADD-001 | AddEditPassword | UNLOCKED | Normal: add entry with all required fields | Vault unlocked; on Vault screen | 1. Tap FAB 2. Enter title="GitHub", username="user@mail.com", password="s3cr3t!" 3. Tap Save | Entry created. All 3 fields encrypted in DB. Navigate to Vault screen. Entry appears in list. Snackbar "已保存". | CRITICAL |
| FLOW-ADD-002 | AddEditPassword | UNLOCKED | Normal: add entry with all optional fields | Vault unlocked | 1. Fill required fields 2. Fill url, notes, category, tags, favorite 3. Save | All fields encrypted. URL and notes encrypted. Category and favorite stored as plaintext metadata. | HIGH |
| FLOW-ADD-003 | AddEditPassword | UNLOCKED | Normal: add entry with securityMode=true | Vault unlocked; SecurityModeManager active | 1. Fill required fields 2. Toggle securityMode ON 3. Save | Password encrypted with SecureModeKey. Other fields encrypted with DataKey. `security_mode == 1` in DB. | CRITICAL |
| FLOW-ADD-004 | AddEditPassword | UNLOCKED | Error: save button disabled with blank title | On AddEdit screen | 1. Enter username + password 2. Leave title blank | Save button is disabled. Tap has no effect. | MEDIUM |
| FLOW-ADD-005 | AddEditPassword | UNLOCKED | Error: save button disabled with blank username | On AddEdit screen | 1. Enter title + password 2. Leave username blank | Save button is disabled | MEDIUM |
| FLOW-ADD-006 | AddEditPassword | UNLOCKED | Error: save button disabled with blank password | On AddEdit screen | 1. Enter title + username 2. Leave password blank | Save button is disabled | MEDIUM |
| FLOW-ADD-007 | AddEditPassword | LOCKED (race) | Error: session locks between form fill and save | User fills form; session locks before save | 1. Fill all fields 2. Session times out and locks 3. Tap Save | `getDataKey()` returns null. UI shows "保险库已锁定". Entry NOT saved. | HIGH |
| FLOW-ADD-008 | AddEditPassword | UNLOCKED | Normal: default category when blank | Category field left blank | 1. Leave category blank 2. Save | Entry stored with category = "默认" | LOW |
| FLOW-ADD-009 | AddEditPassword | UNLOCKED | Edge: very long password (10,000 chars) | — | 1. Enter 10,000 character password 2. Save | Entry saved successfully. Decryption returns exact 10,000 character string. | LOW |
| FLOW-ADD-010 | AddEditPassword | UNLOCKED | Normal: each field encrypted with independent nonce | Entry saved | 1. Save entry 2. Read raw DB row 3. Parse JSON of encrypted_title and encrypted_username | IV (nonce) values are different between the two fields | HIGH |

## 15. Core Flow: Retrieve Password (5+ tests)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| FLOW-RET-001 | PasswordDetail | UNLOCKED | Normal: view standard entry | Entry exists; securityMode=false | 1. Tap entry in list | Detail screen shows decrypted title, username, URL, notes. Password shows "••••••••••••". Eye icon visible. | HIGH |
| FLOW-RET-002 | PasswordDetail | UNLOCKED | Normal: toggle password visibility | On detail screen; standard entry | 1. Tap eye icon | Password plaintext displayed. 2. Tap eye icon again → password masked again. | HIGH |
| FLOW-RET-003 | PasswordDetail | UNLOCKED | Normal: copy password (standard entry) | On detail screen; standard entry | 1. Tap copy icon | Clipboard contains password. Message: "密码已复制，将在 30 秒后清除". Auto-clear timer started. | HIGH |
| FLOW-RET-004 | PasswordDetail | UNLOCKED | Normal: copy username | On detail screen | 1. Tap username copy | Clipboard contains username. Message: "用户名已复制，将在 30 秒后清除". | MEDIUM |
| FLOW-RET-005 | PasswordDetail | UNLOCKED+SM | Normal: view secure mode entry | Entry exists; securityMode=true; global SecureMode ON | 1. Tap entry in list | Password shows "••••••••••••". No eye icon. Only "使用" button visible. | CRITICAL |
| FLOW-RET-006 | PasswordDetail | UNLOCKED+SM | Normal: use password (secure mode) | Secure mode entry on detail | 1. Tap "使用" button | Password decrypted on-demand via usePassword. Copied to clipboard. Decrypted bytes wiped. Message: "密码已使用（已复制），将在 30 秒后清除". | CRITICAL |
| FLOW-RET-007 | PasswordDetail | UNLOCKED+SM | Security: password never visible in UI (secure mode) | Secure mode entry | 1. Open entry detail 2. Inspect all rendered text | No Text composable renders the actual password string. Only "••••••••••••" is rendered. | CRITICAL |
| FLOW-RET-008 | PasswordDetail | LOCKED (race) | Error: session locks while viewing | User on detail screen; session times out | 1. View detail 2. Session locks 3. Tap copy | `getDataKey()` returns null. UI shows "保险库已锁定". Clipboard not modified. | HIGH |
| FLOW-RET-009 | PasswordDetail | UNLOCKED | Error: entry deleted by another path | Entry ID exists; entry deleted externally | 1. Navigate to detail 2. Underlying row deleted 3. `getById` returns null | No entry displayed. Error or empty state shown. | MEDIUM |
| FLOW-RET-010 | PasswordDetail | UNLOCKED | Security: decrypted entry persists in ViewModel state | Load entry, then navigate away | 1. Open entry detail 2. Navigate back to Vault screen 3. Inspect ViewModel StateFlow | Entry remains in StateFlow until ViewModel is collected or reset. No explicit wipe of `PasswordEntry` strings. | MEDIUM |

## 16. Core Flow: Delete Password (5+ tests)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| FLOW-DEL-001 | PasswordDetail | UNLOCKED | Normal: delete standard entry | Entry exists; securityMode=false; global SecureMode OFF | 1. Tap "删除" 2. Confirm dialog | Entry removed from DB. Navigate to Vault. List refreshed without entry. | HIGH |
| FLOW-DEL-002 | PasswordDetail | UNLOCKED | Normal: cancel delete | Entry exists | 1. Tap "删除" 2. Tap "取消" in confirmation dialog | Entry NOT deleted. User remains on detail screen. | MEDIUM |
| FLOW-DEL-003 | PasswordDetail | UNLOCKED | Normal: delete securityMode entry with biometric re-auth | securityMode=true entry; biometric available | 1. Tap "删除" 2. Biometric prompt → success 3. Confirm dialog | Entry deleted. Navigate to Vault. | CRITICAL |
| FLOW-DEL-004 | PasswordDetail | UNLOCKED | Normal: delete securityMode entry with password re-auth | securityMode=true; biometric NOT available | 1. Tap "删除" 2. Password dialog → enter correct password 3. Confirm dialog | Entry deleted. Navigate to Vault. | CRITICAL |
| FLOW-DEL-005 | PasswordDetail | UNLOCKED | Error: delete securityMode entry — wrong password | securityMode=true; biometric NOT available | 1. Tap "删除" 2. Enter wrong password | Message "主密码错误". Dialog remains open. Entry NOT deleted. | HIGH |
| FLOW-DEL-006 | PasswordDetail | UNLOCKED | Error: delete securityMode entry — blank password | securityMode=true; biometric NOT available | 1. Tap "删除" 2. Submit blank password | Message "请输入主密码". Dialog remains open. | MEDIUM |
| FLOW-DEL-007 | PasswordDetail | UNLOCKED | Error: delete securityMode entry — biometric fails | securityMode=true; biometric returns Failed | 1. Tap "删除" 2. Biometric → failure | Message "生物识别验证失败". Entry NOT deleted. | HIGH |
| FLOW-DEL-008 | PasswordDetail | UNLOCKED | Error: delete securityMode entry — biometric cancelled | securityMode=true; biometric returns Cancelled | 1. Tap "删除" 2. User cancels biometric | Message "已取消生物识别验证". Entry NOT deleted. | MEDIUM |
| FLOW-DEL-009 | PasswordDetail | UNLOCKED+SM | Edge: edit/delete hidden in global secure mode | Global SecureMode ON; any entry | 1. Open entry detail 2. Inspect buttons | "编辑" and "删除" buttons are not rendered. No way to delete from UI. | HIGH |
| FLOW-DEL-010 | PasswordDetail | UNLOCKED | Edge: entry ID is null | PasswordEntry with id=null in state | 1. Call `delete()` on ViewModel | Method returns immediately (no-op). No DB operation. No crash. | LOW |

## 17. Core Flow: Master Password Change (5+ tests)

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| FLOW-PW-001 | KeyManager | UNLOCKED | Normal: change master password | Vault unlocked; current password known | 1. Enter current password 2. Enter new password 3. Submit | New salt generated. DataKey re-encrypted with new PasswordKey. vault_config updated. Session remains UNLOCKED. All entries still accessible. | CRITICAL |
| FLOW-PW-002 | KeyManager | UNLOCKED | Normal: DataKey unchanged after password change | Vault unlocked | 1. getDataKey() → keyBefore 2. Change password 3. getDataKey() → keyAfter | keyBefore == keyAfter (DataKey itself never changes) | CRITICAL |
| FLOW-PW-003 | KeyManager | UNLOCKED | Normal: old password no longer works | Password changed from A to B | 1. Change password to B 2. Lock vault 3. Unlock with password A | `Error(InvalidPassword)`. Password A fails. | CRITICAL |
| FLOW-PW-004 | KeyManager | UNLOCKED | Normal: new password works after change | Password changed from A to B | 1. Change password to B 2. Lock vault 3. Unlock with password B | Unlock succeeds. State → UNLOCKED. | CRITICAL |
| FLOW-PW-005 | KeyManager | UNLOCKED | Error: wrong current password | Vault unlocked | 1. Enter wrong current password + new password 2. Submit | `Error(InvalidPassword)`. UI shows "主密码错误". No config change. Session remains UNLOCKED. | HIGH |
| FLOW-PW-006 | KeyManager | UNLOCKED | Security: all ephemeral keys wiped | — | 1. Change password 2. Inspect currentPasswordKey, newPasswordKey, dataKey copy, both passwords | All wiped (all zeros) | CRITICAL |
| FLOW-PW-007 | KeyManager | UNLOCKED | Normal: entries remain accessible after password change | 5 entries exist | 1. Change password 2. Retrieve each entry | All 5 entries decrypt correctly with session DataKey | HIGH |
| FLOW-PW-008 | KeyManager | UNLOCKED | Security: PlatformKeyStore updated with DataKey | — | 1. Change password | `platformKeyStore.storeDeviceKey(dataKey)` called. Biometric path refreshed. | MEDIUM |

## 18. System State Tests

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| STATE-001 | Navigation | NOT_SETUP | State: only auth screens accessible | Fresh install | 1. Attempt to navigate to Vault screen | Navigation stays on Auth surface. Cannot reach Main surface. | HIGH |
| STATE-002 | Navigation | NOT_SETUP | State: onboarding → register flow | Fresh install; onboarding not completed | 1. App launch | Shows Onboarding screen. After completion, shows Register screen. | MEDIUM |
| STATE-003 | Navigation | LOCKED | State: forced to login when locked on Main | User on Vault screen; session locks | 1. Session times out 2. NavGraph LaunchedEffect fires | Automatically navigated to LoginRoute on Auth surface | CRITICAL |
| STATE-004 | Navigation | LOCKED | State: vault operations blocked | State == LOCKED | 1. Attempt `getDataKey()` from any ViewModel | Returns null. Operation aborted with "保险库已锁定" | CRITICAL |
| STATE-005 | Navigation | UNLOCKED | State: all vault operations permitted | State == UNLOCKED | 1. List entries 2. View detail 3. Add entry 4. Delete entry 5. Generate password | All operations succeed | HIGH |
| STATE-006 | Navigation | UNLOCKED+SM | State: secure mode restrictions enforced | SecureMode ON | 1. View entry → password masked 2. Try to find eye icon → absent 3. Check for edit/delete buttons → absent | All restrictions active simultaneously | CRITICAL |
| STATE-007 | KeyManager | UNLOCKED | State: transition UNLOCKED → LOCKED via manual lock | Vault unlocked | 1. Call `lock()` | State == LOCKED. DataKey wiped. Navigation forced to Auth. | HIGH |
| STATE-008 | KeyManager | UNLOCKED | State: transition UNLOCKED → NOT_SETUP via clear | Vault unlocked | 1. Call `clear()` | State == NOT_SETUP. DeviceKey deleted. vault_config nullified. | HIGH |
| STATE-009 | KeyManager | LOCKED | State: transition LOCKED → NOT_SETUP via clear | Vault locked | 1. Call `clear()` | State == NOT_SETUP. | MEDIUM |
| STATE-010 | SecurityMode | UNLOCKED | State: SM-1 transition — enable without auth | SecureMode OFF | 1. Toggle SecureMode ON | No auth prompt shown. Persisted as true. UI restrictions take effect. | HIGH |
| STATE-011 | SecurityMode | UNLOCKED+SM | State: SM-2 transition — disable requires auth | SecureMode ON | 1. Toggle SecureMode OFF 2. Biometric prompt (or password dialog) appears | Must authenticate before toggle completes. If cancelled, SecureMode stays ON. | CRITICAL |

## 19. Cross-Platform Consistency Tests

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| XPLAT-001 | Encryption | UNLOCKED | Cross-platform: encryption output is compatible | Same plaintext + key on Android and Desktop | 1. Encrypt on Android 2. Decrypt on Desktop (or vice versa) | Decryption succeeds. Same plaintext recovered. | HIGH |
| XPLAT-002 | BiometricAuth | ANY | Cross-platform: Desktop always returns NotAvailable | Desktop platform | 1. `isAvailable()` 2. `authenticate(...)` | `false`, `NotAvailable` | MEDIUM |
| XPLAT-003 | SecureClipboard | UNLOCKED | Cross-platform: iOS clipboard is no-op | iOS platform | 1. `copy("test")` 2. `scheduleAutoClear()` | No crash. No effect on system clipboard. | MEDIUM |
| XPLAT-004 | SessionManager | UNLOCKED | Cross-platform: Desktop never auto-locks | Desktop; no lifecycle hooks | 1. Run desktop app 2. Minimize window 3. Wait > timeout 4. Restore window | Session remains UNLOCKED (onAppBackground/onAppForeground never called) | HIGH |
| XPLAT-005 | PlatformKeyStore | UNLOCKED | Cross-platform: Android uses hardware-backed key | Android device with TEE | 1. `isHardwareBacked()` | Returns true | HIGH |

## 20. Security Scenario Tests

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| SEC-001 | KeyManager | LOCKED | Security: brute-force 100 password attempts | Vault locked; correct password is "P@ss123" | 1. Submit 100 wrong passwords 2. Submit correct password | All 100 fail with InvalidPassword. 101st succeeds. No lockout. Each attempt wipes password. | CRITICAL |
| SEC-002 | Encryption | UNLOCKED | Security: encrypted field cannot be decrypted without DataKey | Entry in DB | 1. Extract encrypted_title from raw DB 2. Attempt decrypt with random 32-byte key | Decrypt fails (authentication tag mismatch) | CRITICAL |
| SEC-003 | Encryption | UNLOCKED | Security: ciphertext swap between fields | Two entries exist | 1. Swap encrypted_password of entry A with entry B in raw DB 2. Decrypt entry A | Decryption succeeds but returns entry B's password (same DataKey used). Integrity of individual fields is maintained by AEAD tag, but cross-row swaps succeed if same key. | HIGH |
| SEC-004 | SecurityMode | UNLOCKED+SM | Security: usePassword never exposes plaintext to UI layer | Secure mode entry | 1. Call `usePassword()` 2. Trace data flow | Plaintext goes: decrypt → clipboard copy → wipe. Never assigned to any StateFlow or UI state. | CRITICAL |
| SEC-005 | SessionManager | UNLOCKED | Security: DataKey not accessible after lock | Session was unlocked | 1. `lock()` 2. Inspect SensitiveData wrapper | `isAvailable == false`. Underlying byte array is wiped (all zeros). `getDataKey()` throws. | CRITICAL |
| SEC-006 | KeyManager | UNLOCKED | Security: password change wipes all intermediates | — | 1. Change password 2. Inspect: currentPasswordKey, newPasswordKey, dataKey copy, currentPassword, newPassword | All 5 values are wiped (all zeros) | CRITICAL |
| SEC-007 | Clipboard | UNLOCKED | Security: clipboard NOT cleared if app is killed before 30s | Password copied; auto-clear scheduled | 1. Copy password 2. Force-kill app within 30s 3. Read clipboard | Clipboard still contains password (timer was in-process; kill terminates coroutine) | HIGH |
| SEC-008 | Database | UNLOCKED | Security: plaintext metadata leakage | Entries exist with various categories, timestamps | 1. Read raw DB without DataKey | Attacker learns: number of entries, categories, which are favorites, which use security mode, creation/update timestamps. Cannot read title, username, password, URL, notes, tags. | MEDIUM |
| SEC-009 | Database | UNLOCKED | Security: generated passwords in plaintext | Generated passwords exist | 1. Read raw `generated_passwords` table | All generated passwords readable in plaintext. Strength scores and timestamps visible. | HIGH |
| SEC-010 | PlatformKeyStore | LOCKED | Security (Desktop): DataKey recoverable from preferences | Desktop; key stored | 1. Read Java Preferences file 2. Extract XOR master key and encrypted blob 3. XOR them | DataKey recovered in plaintext | CRITICAL |
| SEC-011 | CryptoUtils | ANY | Security: PRNG used for key generation | — | 1. Inspect `CryptoUtils.generateSecureRandom()` 2. Note it uses `kotlin.random.Random.Default` | Random values are PRNG-generated, not CSPRNG. Affects all keys, nonces, and salts. | CRITICAL |
| SEC-012 | Argon2Kdf | ANY | Security: password string persists in memory | Derive key from password | 1. `deriveKey(password, salt, config)` 2. Trigger GC 3. Scan heap for password string | `password.concatToString()` creates immutable String in string pool. String may persist after GC. | HIGH |
| SEC-013 | SecurityMode | UNLOCKED+SM | Security: disable SecureMode rejected without auth | SecureMode ON | 1. Toggle OFF 2. Cancel biometric 3. Dismiss password dialog | SecureMode remains ON. No config change. | CRITICAL |
| SEC-014 | Navigation | UNLOCKED | Security: lock forces navigation regardless of screen depth | User deep in settings sub-screen | 1. Navigate: Vault → Detail → Edit (3 screens deep) 2. Session times out | Navigation forcibly resets to Auth/Login. All Main stack cleared. | HIGH |
| SEC-015 | KeyManager | NOT_SETUP | Security: vault wipe makes data irrecoverable | Vault with 10 entries | 1. Call `clear()` 2. Attempt to decrypt any entry | DataKey gone from memory and PlatformKeyStore. Cannot derive DataKey without master password + salt (which is in vault_config that is application-level nullified). Entries permanently inaccessible. | CRITICAL |

## 21. Edge Case Tests

| Test ID | Module | State | Scenario | Preconditions | Steps | Expected Result | Security Level |
|---------|--------|-------|----------|---------------|-------|-----------------|----------------|
| EDGE-001 | SessionManager | UNLOCKED | Edge: timeout exactly at boundary | `lockTimeoutMs = 1000`; elapsed = exactly 1000ms | 1. `onAppBackground()` 2. Wait exactly 1000ms 3. `onAppForeground()` | Condition `elapsed >= lockTimeoutMs` is true. Session locks. | MEDIUM |
| EDGE-002 | SessionManager | UNLOCKED | Edge: timeout = 0 then background for 24 hours | `lockTimeoutMs = 0` | 1. Background app 2. Wait 24 hours 3. Foreground | `onAppForeground` returns false (timeout <= 0 branch refreshes activity time). Session stays UNLOCKED. | MEDIUM |
| EDGE-003 | PasswordRepository | UNLOCKED | Edge: concurrent create and search | 2 coroutines running simultaneously | 1. Coroutine A: creates entry 2. Coroutine B: searches for entries | No crash. Search may or may not include the new entry depending on timing. | MEDIUM |
| EDGE-004 | SecurityMode | UNLOCKED | Edge: entry securityMode=true but global toggle OFF | Entry created with securityMode=true; global toggle later disabled | 1. View entry detail | Password still encrypted with SecureModeKey (per-entry flag). UI behavior depends on per-entry flag (password masked, "使用" button shown). | HIGH |
| EDGE-005 | KeyManager | UNLOCKED | Edge: changeMasterPassword during active entry load | Password change in progress; concurrent entry load | 1. Start password change 2. Simultaneously load entry | Entry load uses DataKey copy obtained before change. DataKey is the same value. Both operations succeed. | MEDIUM |
| EDGE-006 | Encryption | UNLOCKED | Edge: encrypt field with max 10 MB data | 10 MB byte array | 1. `encrypt(10MB_array, key)` | Succeeds. EncryptedData returned with 10MB+ ciphertext. | LOW |
| EDGE-007 | Encryption | UNLOCKED | Edge: encrypt field exceeding 10 MB | 11 MB byte array | 1. `encrypt(11MB_array, key)` | Behavior determined by libsodium. May succeed or fail depending on implementation. | LOW |
| EDGE-008 | PasswordRepository | UNLOCKED | Edge: search decrypts all 500 entries | 500 entries in database | 1. `search("nonexistent", PasswordFilter(), dataKey)` | All 500 entries decrypted into memory for client-side search. Returns empty list. Memory contains 500 plaintext entries briefly. | MEDIUM |
| EDGE-009 | SettingsViewModel | UNLOCKED | Edge: unsupported timeout value normalized | Timeout set to 999999 (not in supported set) | 1. `updateSessionTimeout(999999)` | Normalized to default 300,000 (5 minutes). Not stored as 999999. | LOW |
| EDGE-010 | SettingsViewModel | UNLOCKED | Edge: legacy immediate timeout migrated | Config contains `session_lock_timeout_ms = "1000"` (legacy) | 1. Load settings | Normalized from 1000 to -1 (IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS) | LOW |
| EDGE-011 | Encryption | UNLOCKED | Edge: storage format with extra JSON fields | JSON string with additional unknown fields | 1. `EncryptedData.fromStorageFormat` with extra fields in JSON | Parsed successfully (ignoreUnknownKeys = true). Extra fields ignored. | LOW |
| EDGE-012 | PasswordRepository | UNLOCKED | Edge: tags JSON parsing failure | Corrupted encrypted_tags decrypts to invalid JSON | 1. Decrypt tags 2. JSON.decode fails | Returns empty list (caught by `runCatching { ... }.getOrElse { emptyList() }`) | LOW |

---

## Traceability Matrix

| SRS Requirement | Test IDs |
|-----------------|----------|
| SO-1 (Individual field encryption) | REPO-002, REPO-014, FLOW-ADD-010 |
| SO-2 (DataKey not on disk) | FLOW-SETUP-002, SEC-005, SES-015 |
| SO-3 (Password never stored) | KDF-006, KDF-007, FLOW-SETUP-003, FLOW-PW-006 |
| SO-4 (Ephemeral material wiped) | SMM-004, SMM-005, SMM-007, SEC-006, MEM-001, MEM-009 |
| SO-5 (Clipboard 30s auto-clear) | CLIP-002, CLIP-004, CLIP-005, CLIP-006 |
| SO-6 (Screenshot protection default) | SS-001 |
| SO-7 (SecureMode passwords never displayed) | FLOW-RET-005, FLOW-RET-007, SEC-004 |
| SO-8 (Session auto-lock) | SES-006, SES-009, SES-011, FLOW-UNLOCK-001 |
| KM-1 (Password never stored) | KDF-006, KDF-007, FLOW-SETUP-003 |
| KM-4 (DataKey unchanged on pw change) | FLOW-PW-002 |
| KM-5 (SecureModeKey wiped per-op) | SMM-004, SMM-005, SMM-007 |
| SEC-CLIP-1 (30s fixed) | CLIP-002, CLIP-006, SEC-007 |
| SEC-ENC-3 (Tamper detection) | ENC-010, ENC-011, ENC-012 |
| AC-28 (Brute-force) | SEC-001, FLOW-UNLOCK-003 |
| AC-29 (DB theft) | SEC-002, SEC-008, SEC-009 |
| AC-31 (Desktop key extraction) | PKS-008, SEC-010 |
| KI-1 (PRNG issue) | SEC-011 |
| KI-3 (String in memory) | SEC-012 |
| KI-5 (checkAutoLock no caller) | SES-010, SES-011 |
| KI-6 (Desktop no lifecycle) | XPLAT-004 |
| KI-7 (BiometricState not wired) | BIO-006, BIO-007, BIO-009 |
