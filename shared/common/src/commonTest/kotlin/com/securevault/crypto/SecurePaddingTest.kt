package com.securevault.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class SecurePaddingTest {

    @Test
    fun pad_emptyData_returnsBlockSizedData() {
        val data = ByteArray(0)
        val padded = SecurePadding.pad(data)
        assertEquals(CryptoConstants.BLOCK_SIZE, padded.size)
    }

    @Test
    fun pad_singleByte_returnsBlockSizedData() {
        val data = byteArrayOf(42)
        val padded = SecurePadding.pad(data)
        assertEquals(CryptoConstants.BLOCK_SIZE, padded.size)
        assertEquals(42, padded[0])
    }

    @Test
    fun pad_exactlyBlockSizedData_returnsNextBlockSize() {
        val data = ByteArray(CryptoConstants.BLOCK_SIZE) { it.toByte() }
        val padded = SecurePadding.pad(data)
        assertEquals(CryptoConstants.BLOCK_SIZE * 2, padded.size)
        assertContentEquals(data, padded.sliceArray(0 until CryptoConstants.BLOCK_SIZE))
    }

    @Test
    fun pad_multipleOfBlockSize_returnsNextBlockSize() {
        val data = ByteArray(CryptoConstants.BLOCK_SIZE * 3) { it.toByte() }
        val padded = SecurePadding.pad(data)
        assertEquals(CryptoConstants.BLOCK_SIZE * 4, padded.size)
    }

    @Test
    fun pad_dataLargerThanBlock_preservesData() {
        val data = ByteArray(300) { it.toByte() }
        val padded = SecurePadding.pad(data)
        assertContentEquals(data, padded.sliceArray(0 until data.size))
    }

    @Test
    fun pad_unpad_roundtrip() {
        val data = "padding-roundtrip".encodeToByteArray()
        val padded = SecurePadding.pad(data)
        val restored = SecurePadding.unpad(padded)

        assertContentEquals(data, restored)
    }

    @Test
    fun unpad_invalidLength_throws() {
        val invalid = ByteArray(257) { 1 }
        assertFailsWith<InvalidPaddingException> {
            SecurePadding.unpad(invalid)
        }
    }

    @Test
    fun pad_producesMultipleOfBlockSize() {
        for (size in 1..500) {
            val data = ByteArray(size) { it.toByte() }
            val padded = SecurePadding.pad(data)
            assertEquals(0, padded.size % CryptoConstants.BLOCK_SIZE)
        }
    }

    @Test
    fun pad_paddingIsRandom() {
        val data = ByteArray(100) { it.toByte() }
        val padded1 = SecurePadding.pad(data)
        val padded2 = SecurePadding.pad(data)

        assertTrue(!CryptoUtils.constantTimeEquals(padded1, padded2))
    }

    @Test
    fun isPadded_paddedData_returnsTrue() {
        val data = ByteArray(CryptoConstants.BLOCK_SIZE)
        assertTrue(SecurePadding.isPadded(data))
    }

    @Test
    fun isPadded_notPaddedData_returnsFalse() {
        val data = ByteArray(100)
        assertTrue(!SecurePadding.isPadded(data))
    }

    @Test
    fun paddedSize_calculatesCorrectSize() {
        assertEquals(CryptoConstants.BLOCK_SIZE, SecurePadding.paddedSize(0))
        assertEquals(CryptoConstants.BLOCK_SIZE, SecurePadding.paddedSize(1))
        assertEquals(CryptoConstants.BLOCK_SIZE * 2, SecurePadding.paddedSize(CryptoConstants.BLOCK_SIZE))
        assertEquals(CryptoConstants.BLOCK_SIZE * 2, SecurePadding.paddedSize(CryptoConstants.BLOCK_SIZE + 1))
    }
}