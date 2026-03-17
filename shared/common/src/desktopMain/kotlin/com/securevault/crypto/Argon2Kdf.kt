package com.securevault.crypto

import com.ionspin.kotlin.crypto.pwhash.PasswordHash
import com.securevault.security.MemorySanitizer

actual class Argon2Kdf {
    actual fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        config: Argon2Config
    ): ByteArray {
        LibsodiumManager.ensureInitialized()
        val passwordString = password.concatToString()
        return try {
            PasswordHash.pwhash(
                outputLength = config.outputLength,
                password = passwordString,
                salt = salt.toUByteArray(),
                opsLimit = config.iterations.toULong(),
                memLimit = config.memoryKB * 1024,
                algorithm = 2
            ).toByteArray()
        } finally {
            MemorySanitizer.wipe(password)
        }
    }

    actual fun generateSalt(size: Int): ByteArray {
        return CryptoUtils.generateSecureRandom(size)
    }
}

private fun UByteArray.toByteArray(): ByteArray = ByteArray(size) { this[it].toByte() }