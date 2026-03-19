package com.securevault.security

import com.securevault.crypto.AesGcmCipher
import com.securevault.crypto.CryptoConstants
import com.securevault.crypto.CryptoUtils
import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys

class SecurityModeManager(
    private val configRepository: ConfigRepository,
    private val secureClipboard: SecureClipboard,
) {
    suspend fun isEnabled(): Boolean {
        return configRepository.get(VaultConfigKeys.SecurityModeEnabled)
            ?.toBooleanStrictOrNull()
            ?: false
    }

    suspend fun setEnabled(enabled: Boolean) {
        configRepository.set(VaultConfigKeys.SecurityModeEnabled, enabled.toString())
    }

    suspend fun encryptPasswordForStorage(
        plaintextPassword: String,
        dataKey: ByteArray,
        securityMode: Boolean,
    ): String {
        if (!securityMode) {
            return AesGcmCipher.encryptToStorageFormat(plaintextPassword.encodeToByteArray(), dataKey)
        }

        val secureModeKey = getOrCreateSecureModeKey(dataKey)
        return try {
            AesGcmCipher.encryptToStorageFormat(plaintextPassword.encodeToByteArray(), secureModeKey)
        } finally {
            MemorySanitizer.wipe(secureModeKey)
        }
    }

    suspend fun decryptPasswordForRead(
        encryptedPassword: String,
        dataKey: ByteArray,
        securityMode: Boolean,
    ): String {
        if (!securityMode) {
            return AesGcmCipher.decryptFromStorageFormat(encryptedPassword, dataKey).decodeToString()
        }

        val secureModeKey = getOrCreateSecureModeKey(dataKey)
        return try {
            AesGcmCipher.decryptFromStorageFormat(encryptedPassword, secureModeKey).decodeToString()
        } finally {
            MemorySanitizer.wipe(secureModeKey)
        }
    }

    suspend fun usePassword(
        encryptedPassword: String,
        dataKey: ByteArray,
        securityMode: Boolean,
        label: String = "Password",
    ) {
        val decryptedBytes = if (!securityMode) {
            AesGcmCipher.decryptFromStorageFormat(encryptedPassword, dataKey)
        } else {
            val secureModeKey = getOrCreateSecureModeKey(dataKey)
            try {
                AesGcmCipher.decryptFromStorageFormat(encryptedPassword, secureModeKey)
            } finally {
                MemorySanitizer.wipe(secureModeKey)
            }
        }

        try {
            secureClipboard.copy(decryptedBytes.decodeToString(), label)
            secureClipboard.scheduleAutoClear()
        } finally {
            MemorySanitizer.wipe(decryptedBytes)
        }
    }

    private suspend fun getOrCreateSecureModeKey(dataKey: ByteArray): ByteArray {
        val encryptedSecureModeKey = configRepository.get(VaultConfigKeys.EncryptedSecureModeKey)
        if (encryptedSecureModeKey != null) {
            return AesGcmCipher.decryptFromStorageFormat(encryptedSecureModeKey, dataKey)
        }

        val secureModeKey = CryptoUtils.generateSecureRandom(CryptoConstants.AES_KEY_SIZE)
        return try {
            val encrypted = AesGcmCipher.encryptToStorageFormat(secureModeKey, dataKey)
            configRepository.set(VaultConfigKeys.EncryptedSecureModeKey, encrypted)
            secureModeKey.copyOf()
        } finally {
            MemorySanitizer.wipe(secureModeKey)
        }
    }
}
