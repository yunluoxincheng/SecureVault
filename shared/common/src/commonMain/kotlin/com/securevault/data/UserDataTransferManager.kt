package com.securevault.data

import com.securevault.crypto.AdaptiveArgon2Config
import com.securevault.crypto.AesGcmCipher
import com.securevault.crypto.Argon2Config
import com.securevault.crypto.Argon2Kdf
import com.securevault.security.MemorySanitizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val USER_DATA_EXPORT_VERSION = 1
private const val USER_DATA_EXPORT_TYPE = "user_data_export"

@Serializable
data class UserDataArgon2Snapshot(
    val memoryKB: Int,
    val iterations: Int,
    val parallelism: Int,
    val outputLength: Int,
)

@Serializable
data class UserDataTransferPayload(
    val vaultSaltBase64: String,
    val encryptedDataKeyPassword: String,
    val vaultArgon2: UserDataArgon2Snapshot,
)

@Serializable
data class UserDataTransferEnvelope(
    val version: Int,
    val type: String,
    val createdAt: Long,
    val backupSaltBase64: String,
    val backupArgon2: UserDataArgon2Snapshot,
    val encryptedPayload: String,
)

class UserDataTransferManager(
    private val configRepository: ConfigRepository,
    private val argon2Kdf: Argon2Kdf,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun export(masterPassword: CharArray): Result<String> {
        return runCatching {
            val snapshot = readCurrentVaultSnapshot()
            val verifyPassword = masterPassword.copyOf()
            val verifyPasswordKey = argon2Kdf.deriveKey(
                password = verifyPassword,
                salt = snapshot.vaultSalt,
                config = snapshot.vaultArgon2,
            )

            try {
                val dataKey = AesGcmCipher.decryptFromStorageFormat(
                    snapshot.encryptedDataKeyPassword,
                    verifyPasswordKey,
                )
                MemorySanitizer.wipe(dataKey)
            } finally {
                MemorySanitizer.wipe(verifyPasswordKey)
            }

            val backupArgon2 = AdaptiveArgon2Config.getStandardConfig()
            val backupSalt = argon2Kdf.generateSalt()
            val backupPassword = masterPassword.copyOf()
            val backupKey = argon2Kdf.deriveKey(backupPassword, backupSalt, backupArgon2)

            try {
                val payload = UserDataTransferPayload(
                    vaultSaltBase64 = snapshot.vaultSaltBase64,
                    encryptedDataKeyPassword = snapshot.encryptedDataKeyPassword,
                    vaultArgon2 = snapshot.vaultArgon2.toSnapshot(),
                )
                val payloadJson = json.encodeToString(payload)
                val encryptedPayload = AesGcmCipher.encryptToStorageFormat(
                    payloadJson.encodeToByteArray(),
                    backupKey,
                )

                val envelope = UserDataTransferEnvelope(
                    version = USER_DATA_EXPORT_VERSION,
                    type = USER_DATA_EXPORT_TYPE,
                    createdAt = System.currentTimeMillis(),
                    backupSaltBase64 = snapshot.encodeBase64(backupSalt),
                    backupArgon2 = backupArgon2.toSnapshot(),
                    encryptedPayload = encryptedPayload,
                )

                json.encodeToString(envelope)
            } finally {
                MemorySanitizer.wipe(backupKey)
                MemorySanitizer.wipe(backupSalt)
                MemorySanitizer.wipe(masterPassword)
            }
        }
    }

    suspend fun import(envelopeJson: String, masterPassword: CharArray): Result<Unit> {
        return runCatching {
            val envelope = json.decodeFromString<UserDataTransferEnvelope>(envelopeJson)
            require(envelope.version == USER_DATA_EXPORT_VERSION) {
                "不支持的用户数据版本：${envelope.version}"
            }
            require(envelope.type == USER_DATA_EXPORT_TYPE) {
                "不支持的用户数据类型：${envelope.type}"
            }

            val backupSalt = decodeBase64(envelope.backupSaltBase64)
            val backupArgon2 = envelope.backupArgon2.toArgon2Config()
            val backupPassword = masterPassword.copyOf()
            val backupKey = argon2Kdf.deriveKey(backupPassword, backupSalt, backupArgon2)

            try {
                val payloadJson = runCatching {
                    AesGcmCipher.decryptFromStorageFormat(
                        envelope.encryptedPayload,
                        backupKey,
                    ).decodeToString()
                }.getOrElse { error ->
                    error("用户数据解密失败，请确认主密码正确：${error.message ?: "未知错误"}")
                }

                val payload = json.decodeFromString<UserDataTransferPayload>(payloadJson)
                val vaultSalt = decodeBase64(payload.vaultSaltBase64)
                val vaultArgon2 = payload.vaultArgon2.toArgon2Config()
                val vaultPassword = masterPassword.copyOf()
                val passwordKey = argon2Kdf.deriveKey(vaultPassword, vaultSalt, vaultArgon2)

                try {
                    val dataKey = runCatching {
                        AesGcmCipher.decryptFromStorageFormat(
                            payload.encryptedDataKeyPassword,
                            passwordKey,
                        )
                    }.getOrElse { error ->
                        error("主密码验证失败，无法恢复密钥：${error.message ?: "未知错误"}")
                    }
                    MemorySanitizer.wipe(dataKey)
                } finally {
                    MemorySanitizer.wipe(passwordKey)
                    MemorySanitizer.wipe(vaultSalt)
                }

                configRepository.set(VaultConfigKeys.Salt, payload.vaultSaltBase64)
                configRepository.set(
                    VaultConfigKeys.EncryptedDataKeyPassword,
                    payload.encryptedDataKeyPassword,
                )
                configRepository.set(
                    VaultConfigKeys.Argon2MemoryKb,
                    payload.vaultArgon2.memoryKB.toString(),
                )
                configRepository.set(
                    VaultConfigKeys.Argon2Iterations,
                    payload.vaultArgon2.iterations.toString(),
                )
                configRepository.set(
                    VaultConfigKeys.Argon2Parallelism,
                    payload.vaultArgon2.parallelism.toString(),
                )
                configRepository.set(
                    VaultConfigKeys.Argon2OutputLength,
                    payload.vaultArgon2.outputLength.toString(),
                )
                configRepository.set(VaultConfigKeys.VaultSetupCompleted, true.toString())
            } finally {
                MemorySanitizer.wipe(backupKey)
                MemorySanitizer.wipe(backupSalt)
                MemorySanitizer.wipe(masterPassword)
            }
        }
    }

    private suspend fun readCurrentVaultSnapshot(): VaultConfigSnapshot {
        val saltBase64 = requireNotNull(configRepository.get(VaultConfigKeys.Salt)) {
            "当前尚未配置保险库，无法导出用户数据"
        }
        val encryptedDataKeyPassword =
            requireNotNull(configRepository.get(VaultConfigKeys.EncryptedDataKeyPassword)) {
                "缺少保险库密钥数据，无法导出用户数据"
            }

        val vaultArgon2 = Argon2Config(
            memoryKB = requireNotNull(configRepository.get(VaultConfigKeys.Argon2MemoryKb))
                .toIntOrNull() ?: error("Argon2 参数损坏：memoryKB"),
            iterations = requireNotNull(configRepository.get(VaultConfigKeys.Argon2Iterations))
                .toIntOrNull() ?: error("Argon2 参数损坏：iterations"),
            parallelism = requireNotNull(configRepository.get(VaultConfigKeys.Argon2Parallelism))
                .toIntOrNull() ?: error("Argon2 参数损坏：parallelism"),
            outputLength = requireNotNull(configRepository.get(VaultConfigKeys.Argon2OutputLength))
                .toIntOrNull() ?: error("Argon2 参数损坏：outputLength"),
        )

        return VaultConfigSnapshot(
            vaultSaltBase64 = saltBase64,
            vaultSalt = decodeBase64(saltBase64),
            encryptedDataKeyPassword = encryptedDataKeyPassword,
            vaultArgon2 = vaultArgon2,
        )
    }

    private fun decodeBase64(data: String): ByteArray {
        return com.securevault.crypto.CryptoUtils.decodeBase64(data)
    }

    private fun VaultConfigSnapshot.encodeBase64(data: ByteArray): String {
        return com.securevault.crypto.CryptoUtils.encodeBase64(data)
    }
}

private data class VaultConfigSnapshot(
    val vaultSaltBase64: String,
    val vaultSalt: ByteArray,
    val encryptedDataKeyPassword: String,
    val vaultArgon2: Argon2Config,
)

private fun Argon2Config.toSnapshot(): UserDataArgon2Snapshot {
    return UserDataArgon2Snapshot(
        memoryKB = memoryKB,
        iterations = iterations,
        parallelism = parallelism,
        outputLength = outputLength,
    )
}

private fun UserDataArgon2Snapshot.toArgon2Config(): Argon2Config {
    return Argon2Config(
        memoryKB = memoryKB,
        iterations = iterations,
        parallelism = parallelism,
        outputLength = outputLength,
    )
}
