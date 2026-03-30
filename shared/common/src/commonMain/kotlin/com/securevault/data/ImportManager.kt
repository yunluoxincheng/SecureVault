package com.securevault.data

import com.securevault.crypto.AesGcmCipher
import com.securevault.security.KeyManager
import com.securevault.security.MemorySanitizer
import kotlinx.serialization.json.Json

class ImportManager(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager,
    private val configRepository: ConfigRepository? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import(
        envelopeJson: String,
        conflictStrategy: VaultImportConflictStrategy,
    ): Result<VaultImportResult> {
        return runCatching {
            val dataKey = requireDataKey()
            try {
                val envelope = parseEnvelope(envelopeJson)
                validateKeyBinding(envelope)
                val importedEntries = decodeEntriesFromEnvelope(envelope, dataKey)
                val existingEntries = passwordRepository.getAll(dataKey)
                val indexByFingerprint = existingEntries.associateBy { it.fingerprint() }.toMutableMap()

                var importedCount = 0
                var skippedCount = 0
                var overwrittenCount = 0

                passwordRepository.runInTransaction {
                    for (entry in importedEntries) {
                        val fingerprint = entry.fingerprint()
                        val existing = indexByFingerprint[fingerprint]

                        if (existing == null) {
                            create(entry.toPasswordEntry(), dataKey)
                            importedCount += 1
                            continue
                        }

                        when (conflictStrategy) {
                            VaultImportConflictStrategy.Skip -> {
                                skippedCount += 1
                            }

                            VaultImportConflictStrategy.Overwrite -> {
                                val now = System.currentTimeMillis()
                                val updated = entry.toPasswordEntry(
                                    id = existing.id,
                                    createdAt = existing.createdAt,
                                    updatedAt = now,
                                )
                                update(updated, dataKey)
                                overwrittenCount += 1
                            }
                        }
                    }
                }

                VaultImportResult(
                    totalInFile = importedEntries.size,
                    imported = importedCount,
                    skippedDuplicates = skippedCount,
                    overwritten = overwrittenCount,
                )
            } finally {
                passwordRepository.invalidateDecryptCache()
                MemorySanitizer.wipe(dataKey)
            }
        }
    }

    private fun parseEnvelope(rawJson: String): VaultExportEnvelope {
        val envelope = json.decodeFromString<VaultExportEnvelope>(rawJson)
        require(envelope.version == VAULT_EXPORT_FORMAT_VERSION) {
            "不支持的导出版本：${envelope.version}"
        }
        require(envelope.type.toVaultExportTypeOrNull() != null) {
            "不支持的导出类型：${envelope.type}"
        }
        return envelope
    }

    private suspend fun decodeEntriesFromEnvelope(
        envelope: VaultExportEnvelope,
        dataKey: ByteArray,
    ): List<VaultExportEntry> {
        val exportType = envelope.type.toVaultExportTypeOrNull()
            ?: error("不支持的导出类型：${envelope.type}")

        val payloadJson = when (exportType) {
            VaultExportType.Plain -> envelope.plainData
                ?: error("明文导出缺少 plainData")

            VaultExportType.Encrypted -> {
                val encryptedData = envelope.encryptedData ?: error("加密导出缺少 encryptedData")
                AesGcmCipher.decryptFromStorageFormat(encryptedData, dataKey).decodeToString()
            }

            VaultExportType.Secure -> {
                val encryptedData = envelope.encryptedData ?: error("安全导出缺少 encryptedData")
                val encryptedKey = envelope.encryptedKey ?: error("安全导出缺少 encryptedKey")
                val exportKey = AesGcmCipher.decryptFromStorageFormat(encryptedKey, dataKey)
                try {
                    AesGcmCipher.decryptFromStorageFormat(encryptedData, exportKey).decodeToString()
                } finally {
                    MemorySanitizer.wipe(exportKey)
                }
            }
        }

        val payload = json.decodeFromString<VaultExportPayload>(payloadJson)
        return payload.entries
    }

    private fun requireDataKey(): ByteArray {
        return keyManager.getDataKey() ?: error("会话已锁定，请先解锁后再导入")
    }

    private suspend fun validateKeyBinding(envelope: VaultExportEnvelope) {
        val expectedBinding = envelope.keyBinding ?: return
        val encryptedDataKeyPassword = configRepository
            ?.get(VaultConfigKeys.EncryptedDataKeyPassword)
            ?: error("当前设备缺少用户数据，请先导入用户数据后再导入加密密码库")
        val currentBinding = computeBinding(encryptedDataKeyPassword)
        require(expectedBinding == currentBinding) {
            "当前用户数据与该密码导出文件不匹配，请导入与该文件同源的用户数据"
        }
    }
}

private fun VaultExportEntry.fingerprint(): String {
    return listOf(
        title.trim().lowercase(),
        username.trim().lowercase(),
        url.orEmpty().trim().lowercase(),
    ).joinToString("|")
}

private fun PasswordEntry.fingerprint(): String {
    return listOf(
        title.trim().lowercase(),
        username.trim().lowercase(),
        url.orEmpty().trim().lowercase(),
    ).joinToString("|")
}
