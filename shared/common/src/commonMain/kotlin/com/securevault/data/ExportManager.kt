package com.securevault.data

import com.securevault.crypto.AesGcmCipher
import com.securevault.crypto.CryptoConstants
import com.securevault.crypto.CryptoUtils
import com.securevault.security.KeyManager
import com.securevault.security.MemorySanitizer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExportManager(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun export(mode: VaultExportMode): Result<String> {
        return runCatching {
            val dataKey = requireDataKey()
            try {
                val entries = passwordRepository.getAll(dataKey).map { it.toExportEntry() }
                val payloadJson = json.encodeToString(VaultExportPayload(entries))
                val createdAt = System.currentTimeMillis()

                val envelope = when (mode) {
                    VaultExportMode.Plaintext -> {
                        VaultExportEnvelope(
                            version = VAULT_EXPORT_FORMAT_VERSION,
                            type = VaultExportType.Plain.toRawValue(),
                            createdAt = createdAt,
                            entryCount = entries.size,
                            plainData = payloadJson,
                        )
                    }

                    VaultExportMode.Encrypted -> {
                        val encryptedData = AesGcmCipher.encryptToStorageFormat(
                            payloadJson.encodeToByteArray(),
                            dataKey,
                        )
                        VaultExportEnvelope(
                            version = VAULT_EXPORT_FORMAT_VERSION,
                            type = VaultExportType.Encrypted.toRawValue(),
                            createdAt = createdAt,
                            entryCount = entries.size,
                            encryptedData = encryptedData,
                        )
                    }

                    VaultExportMode.SecureMode -> {
                        val exportKey = CryptoUtils.generateSecureRandom(CryptoConstants.AES_KEY_SIZE)
                        try {
                            val encryptedData = AesGcmCipher.encryptToStorageFormat(
                                payloadJson.encodeToByteArray(),
                                exportKey,
                            )
                            val encryptedKey = AesGcmCipher.encryptToStorageFormat(exportKey, dataKey)
                            VaultExportEnvelope(
                                version = VAULT_EXPORT_FORMAT_VERSION,
                                type = VaultExportType.Secure.toRawValue(),
                                createdAt = createdAt,
                                entryCount = entries.size,
                                encryptedKey = encryptedKey,
                                encryptedData = encryptedData,
                            )
                        } finally {
                            MemorySanitizer.wipe(exportKey)
                        }
                    }
                }

                json.encodeToString(envelope)
            } finally {
                MemorySanitizer.wipe(dataKey)
            }
        }
    }

    private fun requireDataKey(): ByteArray {
        return keyManager.getDataKey() ?: error("会话已锁定，请先解锁后再导出")
    }
}
