package com.securevault.security

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertContentEquals

class SensitiveDataTest {

    @Test
    fun get_returnsData() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val sensitive = SensitiveData.ofByteArray(data)
        val result = sensitive.get()
        assertTrue(result.contentEquals(data))
    }

    @Test
    fun close_wipesByteArray() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val sensitive = SensitiveData.ofByteArray(data)
        val leakedReference = sensitive.get()
        sensitive.close()

        assertContentEquals(ByteArray(leakedReference.size), leakedReference)
        assertFalse(sensitive.isAvailable)
    }

    @Test
    fun close_wipesCharArray() {
        val data = charArrayOf('a', 'b', 'c', 'd', 'e')
        val sensitive = SensitiveData.ofCharArray(data)
        sensitive.close()
        assertFalse(sensitive.isAvailable)
    }

    @Test
    fun get_afterClose_throws() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val sensitive = SensitiveData.ofByteArray(data)
        sensitive.close()
        assertFailsWith<IllegalStateException> {
            sensitive.get()
        }
    }

    @Test
    fun doubleClose_isSafe() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val sensitive = SensitiveData.ofByteArray(data)
        sensitive.close()
        sensitive.close()
    }

    @Test
    fun use_blockAutoCloses() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        var wasCalled = false
        SensitiveData.ofByteArray(data).use {
            wasCalled = true
        }
        assertTrue(wasCalled)
    }
}