package com.securevault.crypto

object SecurePadding {
    private const val BLOCK_SIZE = CryptoConstants.BLOCK_SIZE

    fun pad(data: ByteArray): ByteArray {
        if (data.isEmpty()) {
            return CryptoUtils.generateSecureRandom(BLOCK_SIZE)
        }

        val remainder = data.size % BLOCK_SIZE
        val paddingSize = if (remainder == 0) BLOCK_SIZE else BLOCK_SIZE - remainder
        val totalSize = data.size + paddingSize

        val result = ByteArray(totalSize)
        data.copyInto(result, 0)

        val padding = CryptoUtils.generateSecureRandom(paddingSize)
        padding.copyInto(result, data.size)

        return result
    }

    fun unpad(data: ByteArray): ByteArray {
        if (data.isEmpty()) {
            return ByteArray(0)
        }

        if (data.size % BLOCK_SIZE != 0) {
            throw InvalidPaddingException("Data size must be multiple of $BLOCK_SIZE")
        }

        return data.copyOf()
    }

    fun isPadded(data: ByteArray): Boolean {
        return data.size % BLOCK_SIZE == 0 && data.size >= BLOCK_SIZE
    }

    fun paddedSize(dataSize: Int): Int {
        if (dataSize == 0) return BLOCK_SIZE
        val remainder = dataSize % BLOCK_SIZE
        return if (remainder == 0) dataSize + BLOCK_SIZE else dataSize + (BLOCK_SIZE - remainder)
    }
}

class InvalidPaddingException(message: String) : Exception(message)