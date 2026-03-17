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
            iv = ByteArray(24) { (it + 1).toByte() },
            ciphertext = byteArrayOf(100, 101, 102, 103, 104, 105, 106, 107),
            tag = byteArrayOf(200.toByte(), 201.toByte(), 202.toByte(), 203.toByte(), 204.toByte(), 205.toByte(), 206.toByte(), 207.toByte(), 208.toByte(), 209.toByte(), 210.toByte(), 211.toByte(), 212.toByte(), 213.toByte(), 214.toByte(), 215.toByte())
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
        val iv = ByteArray(24) { (it + 1).toByte() }
        val ciphertext = byteArrayOf(100, 101, 102)
        val tag = byteArrayOf(200.toByte(), 201.toByte(), 202.toByte(), 203.toByte(), 204.toByte(), 205.toByte(), 206.toByte(), 207.toByte(), 208.toByte(), 209.toByte(), 210.toByte(), 211.toByte(), 212.toByte(), 213.toByte(), 214.toByte(), 215.toByte())

        val data = EncryptedData("v2", iv, ciphertext, tag)
        val combined = data.combined()

        assertContentEquals(iv, combined.sliceArray(0 until 24))
        assertContentEquals(ciphertext, combined.sliceArray(24 until 27))
        assertContentEquals(tag, combined.sliceArray(27 until 43))
    }

    @Test
    fun fromCombined_restoresCorrectData() {
        val iv = ByteArray(24) { (it + 1).toByte() }
        val ciphertext = byteArrayOf(100, 101, 102, 103, 104)
        val tag = byteArrayOf(200.toByte(), 201.toByte(), 202.toByte(), 203.toByte(), 204.toByte(), 205.toByte(), 206.toByte(), 207.toByte(), 208.toByte(), 209.toByte(), 210.toByte(), 211.toByte(), 212.toByte(), 213.toByte(), 214.toByte(), 215.toByte())

        val combined = iv + ciphertext + tag
        val restored = EncryptedData.fromCombined(combined)

        assertContentEquals(iv, restored.iv)
        assertContentEquals(ciphertext, restored.ciphertext)
        assertContentEquals(tag, restored.tag)
    }
}