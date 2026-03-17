package com.securevault.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertContentEquals

class MemorySanitizerTest {

    @Test
    fun wipe_byteArray_allZeros() {
        val data = byteArrayOf(1, 2, 3, 4, 5, 127, -128)
        MemorySanitizer.wipe(data)
        assertTrue(MemorySanitizer.isWiped(data))
    }

    @Test
    fun wipe_charArray_allZeros() {
        val data = charArrayOf('a', 'b', 'c', '中')
        MemorySanitizer.wipe(data)
        assertTrue(MemorySanitizer.isWiped(data))
    }

    @Test
    fun wipe_intArray_allZeros() {
        val data = intArrayOf(1, 2, 3, 4, 5, Int.MAX_VALUE, Int.MIN_VALUE)
        MemorySanitizer.wipe(data)
        assertTrue(data.all { it == 0 })
    }

    @Test
    fun wipe_withCustomPasses() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        MemorySanitizer.wipe(data, passes = 5)
        assertTrue(MemorySanitizer.isWiped(data))
    }

    @Test
    fun isWiped_emptyArray_returnsTrue() {
        val data = ByteArray(0)
        assertTrue(MemorySanitizer.isWiped(data))
    }

    @Test
    fun isWiped_nonZeroArray_returnsFalse() {
        val data = byteArrayOf(0, 0, 0, 1)
        assertFalse(MemorySanitizer.isWiped(data))
    }
}