package com.securevault.crypto

import kotlin.experimental.and

object CryptoUtils {

    fun generateSecureRandom(size: Int): ByteArray {
        require(size > 0) { "Size must be positive" }
        val bytes = ByteArray(size)
        kotlin.random.Random.Default.nextBytes(bytes)
        return bytes
    }

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    fun constantTimeEquals(a: CharArray, b: CharArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    fun encodeBase64(data: ByteArray): String {
        return kotlin.io.encoding.Base64.encode(data)
    }

    fun decodeBase64(data: String): ByteArray {
        return kotlin.io.encoding.Base64.decode(data)
    }

    fun encodeBase64Url(data: ByteArray): String {
        return kotlin.io.encoding.Base64.UrlSafe.encode(data)
    }

    fun decodeBase64Url(data: String): ByteArray {
        return kotlin.io.encoding.Base64.UrlSafe.decode(data)
    }

    fun charArrayToUtf16BE(chars: CharArray): ByteArray {
        val bytes = ByteArray(chars.size * 2)
        for (i in chars.indices) {
            val code = chars[i].code
            bytes[i * 2] = ((code shr 8) and 0xFF).toByte()
            bytes[i * 2 + 1] = (code and 0xFF).toByte()
        }
        return bytes
    }

    fun utf16BEToCharArray(bytes: ByteArray): CharArray {
        require(bytes.size % 2 == 0) { "Byte array length must be even" }
        val chars = CharArray(bytes.size / 2)
        for (i in chars.indices) {
            val high = (bytes[i * 2].toInt() and 0xFF) shl 8
            val low = bytes[i * 2 + 1].toInt() and 0xFF
            chars[i] = Char(high or low)
        }
        return chars
    }

    fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    fun hexStringToByteArray(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string length must be even" }
        return ByteArray(hex.length / 2) {
            hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    fun ByteArray.xor(other: ByteArray): ByteArray {
        require(size == other.size) { "Arrays must have same length" }
        return ByteArray(size) { i -> (this[i] xor other[i]) }
    }

    fun ByteArray.copy(): ByteArray {
        return copyOf()
    }

    fun CharArray.copy(): CharArray {
        return copyOf()
    }
}