package com.securevault.crypto

import com.ionspin.kotlin.crypto.aead.XChaCha20Poly1305

object AesGcmCipher {
    private const val KEY_SIZE = 32
    private const val IV_SIZE = 24
    private const val TAG_SIZE = 16

    suspend fun encrypt(plaintext: ByteArray, key: ByteArray): EncryptedData {
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }
        require(plaintext.isNotEmpty()) { "Plaintext cannot be empty" }

        val iv = CryptoUtils.generateSecureRandom(IV_SIZE)
        val nonce = XChaCha20Poly1305.Nonce(iv)
        val associatedData = byteArrayOf()

        val ciphertextWithTag = XChaCha20Poly1305.encrypt(
            plaintext,
            associatedData,
            nonce,
            key
        )

        val ciphertext = ciphertextWithTag.sliceArray(0 until ciphertextWithTag.size - TAG_SIZE)
        val tag = ciphertextWithTag.sliceArray(ciphertextWithTag.size - TAG_SIZE until ciphertextWithTag.size)

        return EncryptedData(
            version = CryptoConstants.CURRENT_STORAGE_FORMAT,
            iv = iv,
            ciphertext = ciphertext,
            tag = tag
        )
    }

    suspend fun decrypt(encryptedData: EncryptedData, key: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }
        require(encryptedData.version == CryptoConstants.CURRENT_STORAGE_FORMAT) {
            "Unsupported version: ${encryptedData.version}"
        }

        val nonce = XChaCha20Poly1305.Nonce(encryptedData.iv)
        val ciphertextWithTag = encryptedData.ciphertext + encryptedData.tag
        val associatedData = byteArrayOf()

        return XChaCha20Poly1305.decrypt(
            ciphertextWithTag,
            associatedData,
            nonce,
            key
        )
    }

    suspend fun encryptToStorageFormat(plaintext: ByteArray, key: ByteArray): String {
        val encryptedData = encrypt(plaintext, key)
        return encryptedData.toStorageFormat()
    }

    suspend fun decryptFromStorageFormat(storageData: String, key: ByteArray): ByteArray {
        val encryptedData = EncryptedData.fromStorageFormat(storageData)
        return decrypt(encryptedData, key)
    }
}