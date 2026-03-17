package com.securevault.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.securevault.crypto.Argon2Config
import com.securevault.crypto.CryptoConstants
import com.securevault.crypto.CryptoUtils

actual class Argon2Kdf {
    private val argon2 = Argon2Kt()

    actual fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        config: Argon2Config
    ): ByteArray {
        val passwordBytes = CryptoUtils.charArrayToUtf16BE(password)

        val result = argon2.hash(
            mode = Argon2Mode.Argon2id,
            password = passwordBytes,
            salt = salt,
            tCostInIterations = config.iterations,
            mCostInKiB = config.memoryKB,
            parallelism = config.parallelism,
            hashLength = config.outputLength
        )

        passwordBytes.fill(0)

        return result.rawHash()
    }

    actual fun generateSalt(size: Int): ByteArray {
        return CryptoUtils.generateSecureRandom(size)
    }
}