package com.securevault.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AesGcmCipherTest {

    @Test
    fun encrypt_decrypt_roundtrip() = runTest {
        val key = ByteArray(CryptoConstants.AES_KEY_SIZE) { (it + 1).toByte() }
        val plaintext = "secure-vault-plaintext".encodeToByteArray()

        val encrypted = AesGcmCipher.encrypt(plaintext, key)
        val decrypted = AesGcmCipher.decrypt(encrypted, key)

        assertContentEquals(plaintext, decrypted)
        assertEquals(CryptoConstants.XCHACHA_IV_SIZE, encrypted.iv.size)
        assertEquals(CryptoConstants.AEAD_TAG_SIZE, encrypted.tag.size)
    }

    @Test
    fun encrypt_samePlaintext_producesDifferentCiphertext() = runTest {
        val key = ByteArray(CryptoConstants.AES_KEY_SIZE) { it.toByte() }
        val plaintext = "same-input".encodeToByteArray()

        val first = AesGcmCipher.encrypt(plaintext, key)
        val second = AesGcmCipher.encrypt(plaintext, key)

        assertTrue(!first.iv.contentEquals(second.iv))
        assertTrue(!first.ciphertext.contentEquals(second.ciphertext) || !first.tag.contentEquals(second.tag))
    }

    @Test
    fun decrypt_withWrongKey_fails() = runTest {
        val key = ByteArray(CryptoConstants.AES_KEY_SIZE) { it.toByte() }
        val wrongKey = ByteArray(CryptoConstants.AES_KEY_SIZE) { (it + 1).toByte() }
        val plaintext = "protected".encodeToByteArray()

        val encrypted = AesGcmCipher.encrypt(plaintext, key)

        assertFails {
            AesGcmCipher.decrypt(encrypted, wrongKey)
        }
    }

    @Test
    fun storageFormat_roundtrip() = runTest {
        val key = ByteArray(CryptoConstants.AES_KEY_SIZE) { (it * 3).toByte() }
        val plaintext = "storage-format".encodeToByteArray()

        val storage = AesGcmCipher.encryptToStorageFormat(plaintext, key)
        val decrypted = AesGcmCipher.decryptFromStorageFormat(storage, key)

        assertContentEquals(plaintext, decrypted)
    }
}