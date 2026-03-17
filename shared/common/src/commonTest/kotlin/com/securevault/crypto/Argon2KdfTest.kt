package com.securevault.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Argon2KdfTest {

    private val config = Argon2Config(
        memoryKB = CryptoConstants.Argon2.DEFAULT_MEMORY_KB,
        iterations = CryptoConstants.Argon2.DEFAULT_ITERATIONS,
        parallelism = CryptoConstants.Argon2.DEFAULT_PARALLELISM,
        outputLength = CryptoConstants.Argon2.DEFAULT_OUTPUT_LENGTH
    )

    @Test
    fun deriveKey_sameInput_isDeterministic() {
        val kdf = Argon2Kdf()
        val password = "MasterP@ssw0rd".toCharArray()
        val salt = ByteArray(CryptoConstants.SALT_SIZE) { it.toByte() }

        val first = kdf.deriveKey(password.copyOf(), salt, config)
        val second = kdf.deriveKey(password.copyOf(), salt, config)

        assertTrue(first.contentEquals(second))
        assertEquals(config.outputLength, first.size)
    }

    @Test
    fun deriveKey_differentSalt_producesDifferentResult() {
        val kdf = Argon2Kdf()
        val password = "MasterP@ssw0rd".toCharArray()
        val salt1 = ByteArray(CryptoConstants.SALT_SIZE) { it.toByte() }
        val salt2 = ByteArray(CryptoConstants.SALT_SIZE) { (it + 7).toByte() }

        val first = kdf.deriveKey(password.copyOf(), salt1, config)
        val second = kdf.deriveKey(password.copyOf(), salt2, config)

        assertTrue(!first.contentEquals(second))
    }

    @Test
    fun deriveKey_respectsOutputLength() {
        val kdf = Argon2Kdf()
        val password = "MasterP@ssw0rd".toCharArray()
        val salt = ByteArray(CryptoConstants.SALT_SIZE) { (it * 2).toByte() }
        val customConfig = config.copy(outputLength = 64)

        val key = kdf.deriveKey(password, salt, customConfig)

        assertEquals(64, key.size)
    }
}