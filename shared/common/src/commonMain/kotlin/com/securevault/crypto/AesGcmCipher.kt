package com.securevault.crypto

import com.ionspin.kotlin.crypto.secretbox.SecretBox

object AesGcmCipher {
    private const val KEY_SIZE = 32
    private const val NONCE_SIZE = 24
    private const val TAG_SIZE = CryptoConstants.AEAD_TAG_SIZE

    suspend fun encrypt(plaintext: ByteArray, key: ByteArray): EncryptedData {
        LibsodiumManager.ensureInitialized()
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }
        require(plaintext.isNotEmpty()) { "Plaintext cannot be empty" }

        val nonce = CryptoUtils.generateSecureRandom(NONCE_SIZE)
        
        val cipherWithTag = SecretBox.easy(
            plaintext.toUByteArray(),
            nonce.toUByteArray(),
            key.toUByteArray()
        ).toByteArray()

        val tag = cipherWithTag.sliceArray(0 until TAG_SIZE)
        val ciphertext = cipherWithTag.sliceArray(TAG_SIZE until cipherWithTag.size)

        return EncryptedData(
            version = CryptoConstants.CURRENT_STORAGE_FORMAT,
            iv = nonce,
            ciphertext = ciphertext,
            tag = tag
        )
    }

    suspend fun decrypt(encryptedData: EncryptedData, key: ByteArray): ByteArray {
        LibsodiumManager.ensureInitialized()
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }
        require(encryptedData.version == CryptoConstants.CURRENT_STORAGE_FORMAT) {
            "Unsupported version: ${encryptedData.version}"
        }
        require(encryptedData.iv.size == NONCE_SIZE) { "Invalid nonce size: ${encryptedData.iv.size}" }
        require(encryptedData.tag.size == TAG_SIZE) { "Invalid tag size: ${encryptedData.tag.size}" }

        return SecretBox.openEasy(
            (encryptedData.tag + encryptedData.ciphertext).toUByteArray(),
            encryptedData.iv.toUByteArray(),
            key.toUByteArray()
        ).toByteArray()
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

private fun UByteArray.toByteArray(): ByteArray = ByteArray(size) { this[it].toByte() }
private fun ByteArray.toUByteArray(): UByteArray = UByteArray(size) { this[it].toUByte() }