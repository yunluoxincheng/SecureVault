package com.securevault.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EncryptedData(
    val version: String,
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray
) {
    fun toStorageFormat(): String {
        val json = Json { ignoreUnknownKeys = true }
        val data = StorageData(
            version = version,
            iv = CryptoUtils.encodeBase64(iv),
            ciphertext = CryptoUtils.encodeBase64(ciphertext),
            tag = CryptoUtils.encodeBase64(tag)
        )
        return json.encodeToString(data)
    }

    fun combined(): ByteArray {
        return iv + ciphertext + tag
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedData) return false
        return version == other.version &&
               iv.contentEquals(other.iv) &&
               ciphertext.contentEquals(other.ciphertext) &&
               tag.contentEquals(other.tag)
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + tag.contentHashCode()
        return result
    }

    @Serializable
    private data class StorageData(
        val version: String,
        val iv: String,
        val ciphertext: String,
        val tag: String
    )

    companion object {
        fun fromStorageFormat(storageData: String): EncryptedData {
            val json = Json { ignoreUnknownKeys = true }
            val data = json.decodeFromString<StorageData>(storageData)
            return EncryptedData(
                version = data.version,
                iv = CryptoUtils.decodeBase64(data.iv),
                ciphertext = CryptoUtils.decodeBase64(data.ciphertext),
                tag = CryptoUtils.decodeBase64(data.tag)
            )
        }

        fun fromCombined(data: ByteArray, version: String = CryptoConstants.CURRENT_STORAGE_FORMAT): EncryptedData {
            require(data.size > IV_SIZE + TAG_SIZE) { "Data too short" }
            val iv = data.sliceArray(0 until IV_SIZE)
            val ciphertext = data.sliceArray(IV_SIZE until data.size - TAG_SIZE)
            val tag = data.sliceArray(data.size - TAG_SIZE until data.size)
            return EncryptedData(version, iv, ciphertext, tag)
        }

        private const val IV_SIZE = 24
        private const val TAG_SIZE = 16
    }
}