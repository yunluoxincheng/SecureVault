package com.securevault.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.pwhash.PasswordHash
import com.ionspin.kotlin.crypto.pwhash.ArgonMode
import com.ionspin.kotlin.crypto.util.LibsodiumRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class Argon2Kdf {
    actual fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        config: Argon2Config
    ): ByteArray {
        val passwordBytes = CryptoUtils.charArrayToUtf16BE(password)

        val opsLimit = PasswordHash.OpLimit.Interactive
        val memLimit = PasswordHash.MemLimit.Interactive

        val result = PasswordHash.pwhash(
            outputLength = config.outputLength.toUInt(),
            password = passwordBytes,
            salt = salt,
            opsLimit = opsLimit,
            memLimit = memLimit,
            algorithm = ArgonMode.Argon2id
        )

        MemorySanitizer.wipe(passwordBytes)

        return result.asBytes()
    }

    actual fun generateSalt(size: Int): ByteArray {
        return LibsodiumRandom.buf(size.toUInt())
    }
}