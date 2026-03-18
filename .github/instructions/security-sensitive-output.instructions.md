---
description: "Use when handling cryptography code, secrets, credentials, encrypted payload conversion, logging decisions, or security architecture explanations in SecureVault."
name: "SecureVault Security And Sensitive Output Rules"
---
# SecureVault 安全与敏感信息规则

- Treat security facts as implementation-first: align with current code, then docs.
- For algorithm references in this repository, describe current implementation as XChaCha20-Poly1305 via libsodium SecretBox, not AES-GCM unless code proves otherwise.
- When discussing `EncryptedData` and `SecretBox` conversion, preserve the required `tag + ciphertext` split/merge handling.
- Never print or suggest logging of secrets (keys, plaintext passwords, recovery phrases, raw master key material).
- Prefer error-safe flows with `Result` or sealed error types for security-critical operations.
- For sensitive buffers, keep cleanup guidance explicit (overwrite `ByteArray`/`CharArray` after use where applicable).
- If uncertain about a security detail, state uncertainty and require source verification instead of asserting.
