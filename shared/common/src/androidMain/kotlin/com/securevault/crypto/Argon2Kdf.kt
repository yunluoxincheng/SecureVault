package com.securevault.crypto

import com.ionspin.kotlin.crypto.pwhash.PasswordHash
import com.securevault.security.MemorySanitizer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

actual class Argon2Kdf {
    actual fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        config: Argon2Config
    ): ByteArray {
        LibsodiumManager.ensureInitialized()
        val passwordUtf8 = encodeUtf8(password)
        return try {
            val passwordString = String(passwordUtf8, StandardCharsets.UTF_8)
            PasswordHash.pwhash(
                outputLength = config.outputLength,
                password = passwordString,
                salt = salt.toUByteArray(),
                opsLimit = config.iterations.toULong(),
                memLimit = config.memoryKB * 1024,
                algorithm = 2
            ).toByteArray()
        } finally {
            MemorySanitizer.wipe(passwordUtf8)
            MemorySanitizer.wipe(password)
        }
    }

    actual fun generateSalt(size: Int): ByteArray {
        return CryptoUtils.generateSecureRandom(size)
    }
}

/** UTF-8 bytes for [chars], without allocating an intermediate [String]. */
private fun encodeUtf8(chars: CharArray): ByteArray {
    val bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars))
    val n = bb.remaining()
    val out = ByteArray(n)
    bb.get(out)
    return out
}

private fun UByteArray.toByteArray(): ByteArray = ByteArray(size) { this[it].toByte() }
