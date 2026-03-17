package com.securevault.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals

class EncryptedDataTest {

    @Test
    fun toStorageFormat_and_fromStorageFormat_roundtrip() {
        val data = EncryptedData(
            version = "v2",
            iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
            ciphertext = byteArrayOf(100, 101, 102, 103, 104, 105, 106, 107),
            tag = byteArrayOf(200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215)
        )

        val storage = data.toStorageFormat()
        val restored = EncryptedData.fromStorageFormat(storage)

        assertEquals(data.version, restored.version)
        assertContentEquals(data.iv, restored.iv)
        assertContentEquals(data.ciphertext, restored.ciphertext)
        assertContentEquals(data.tag, restored.tag)
    }

    @Test
    fun combined_returnsCorrectByteArray() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val ciphertext = byteArrayOf(100, 101, 102)
        val tag = byteArrayOf(200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215)

        val data = EncryptedData("v2", iv, ciphertext, tag)
        val combined = data.combined()

        assertContentEquals(iv, combined.sliceArray(0 until 12))
        assertContentEquals(ciphertext, combined.sliceArray(12 until 15))
        assertContentEquals(tag, combined.sliceArray(15 until 31))
    }

    @Test
    fun fromCombined_restoresCorrectData() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val ciphertext = byteArrayOf(100, 101, 102, 103, 104)
        val tag = byteArrayOf(200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215)

        val combined = iv + ciphertext + tag
        val restored = EncryptedData.fromCombined(combined)

        assertContentEquals(iv, restored.iv)
        assertContentEquals(ciphertext, restored.ciphertext)
        assertContentEquals(tag, restored.tag)
    }
}