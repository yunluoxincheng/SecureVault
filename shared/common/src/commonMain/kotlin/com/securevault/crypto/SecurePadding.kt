package com.securevault.crypto

object SecurePadding {
    private const val BLOCK_SIZE = CryptoConstants.BLOCK_SIZE

    fun pad(data: ByteArray): ByteArray {
        val remainder = data.size % BLOCK_SIZE
        val paddingSize = if (remainder == 0) BLOCK_SIZE else BLOCK_SIZE - remainder

        val result = ByteArray(data.size + paddingSize)
        data.copyInto(result)

        if (paddingSize > 1) {
            val randomPadding = CryptoUtils.generateSecureRandom(paddingSize - 1)
            randomPadding.copyInto(result, destinationOffset = data.size)
        }

        result[result.lastIndex] = (paddingSize % BLOCK_SIZE).toByte()
        return result
    }

    fun unpad(data: ByteArray): ByteArray {
        require(data.isNotEmpty()) { "Padded data cannot be empty" }

        if (data.size % BLOCK_SIZE != 0) {
            throw InvalidPaddingException("Data size must be multiple of $BLOCK_SIZE")
        }

        val lastByte = data.last().toInt() and 0xFF
        val paddingSize = if (lastByte == 0) BLOCK_SIZE else lastByte

        if (paddingSize !in 1..BLOCK_SIZE || paddingSize > data.size) {
            throw InvalidPaddingException("Invalid padding size: $paddingSize")
        }

        return data.copyOfRange(0, data.size - paddingSize)
    }

    fun isPadded(data: ByteArray): Boolean {
        return data.size % BLOCK_SIZE == 0 && data.size >= BLOCK_SIZE
    }

    fun paddedSize(dataSize: Int): Int {
        if (dataSize == 0) return BLOCK_SIZE
        val remainder = dataSize % BLOCK_SIZE
        return if (remainder == 0) dataSize + BLOCK_SIZE else dataSize + (BLOCK_SIZE - remainder)
    }

    fun padString(text: String): ByteArray = pad(text.encodeToByteArray())

    fun unpadToString(padded: ByteArray): String = unpad(padded).decodeToString()
}

class InvalidPaddingException(message: String) : Exception(message)