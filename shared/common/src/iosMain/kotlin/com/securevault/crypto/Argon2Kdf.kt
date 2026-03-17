package com.securevault.crypto

import com.ionspin.kotlin.crypto.pwhash.ArgonMode
import com.ionspin.kotlin.crypto.pwhash.PasswordHash
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

actual class Argon2Kdf {
    actual fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        config: Argon2Config
    ): ByteArray {
        val passwordBytes = CryptoUtils.charArrayToUtf16BE(password)
        return try {
            val result = PasswordHash.pwhash(
                outputLength = config.outputLength.toUInt(),
                password = passwordBytes,
                salt = salt,
                opsLimit = PasswordHash.OpLimit.Interactive,
                memLimit = PasswordHash.MemLimit.Interactive,
                algorithm = ArgonMode.Argon2id
            )
            result.asBytes()
        } finally {
            com.securevault.security.MemorySanitizer.wipe(passwordBytes)
        }
    }

    actual fun generateSalt(size: Int): ByteArray {
        return LibsodiumRandom.buf(size.toUInt())
    }
}
