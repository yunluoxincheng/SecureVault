package com.securevault.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals

class CryptoUtilsTest {

    @Test
    fun generateSecureRandom_producesCorrectSize() {
        val size = 32
        val result = CryptoUtils.generateSecureRandom(size)
        assertEquals(size, result.size)
    }

    @Test
    fun generateSecureRandom_producesDifferentResults() {
        val result1 = CryptoUtils.generateSecureRandom(32)
        val result2 = CryptoUtils.generateSecureRandom(32)
        assertTrue(!CryptoUtils.constantTimeEquals(result1, result2))
    }

    @Test
    fun constantTimeEquals_identicalArrays() {
        val a = byteArrayOf(1, 2, 3, 4, 5)
        val b = byteArrayOf(1, 2, 3, 4, 5)
        assertTrue(CryptoUtils.constantTimeEquals(a, b))
    }

    @Test
    fun constantTimeEquals_differentArrays() {
        val a = byteArrayOf(1, 2, 3, 4, 5)
        val b = byteArrayOf(1, 2, 3, 4, 6)
        assertTrue(!CryptoUtils.constantTimeEquals(a, b))
    }

    @Test
    fun constantTimeEquals_differentLengths() {
        val a = byteArrayOf(1, 2, 3)
        val b = byteArrayOf(1, 2, 3, 4)
        assertTrue(!CryptoUtils.constantTimeEquals(a, b))
    }

    @Test
    fun base64_encodeDecode_roundtrip() {
        val data = byteArrayOf(1, 2, 3, 4, 5, 127, -128, 0)
        val encoded = CryptoUtils.encodeBase64(data)
        val decoded = CryptoUtils.decodeBase64(encoded)
        assertContentEquals(data, decoded)
    }

    @Test
    fun charArrayToUtf16BE_roundtrip() {
        val chars = charArrayOf('H', 'e', 'l', 'l', 'o', '\u4e2d')
        val bytes = CryptoUtils.charArrayToUtf16BE(chars)
        val result = CryptoUtils.utf16BEToCharArray(bytes)
        assertContentEquals(chars, result)
    }

    @Test
    fun toHexString_producesCorrectFormat() {
        val data = byteArrayOf(0x00, 0x0f, 0xff, 0xab.toByte())
        val hex = data.toHexString()
        assertEquals("000fffab", hex)
    }

    @Test
    fun hexStringToByteArray_roundtrip() {
        val hex = "deadbeef"
        val bytes = CryptoUtils.hexStringToByteArray(hex)
        val result = bytes.toHexString()
        assertEquals(hex, result)
    }
}