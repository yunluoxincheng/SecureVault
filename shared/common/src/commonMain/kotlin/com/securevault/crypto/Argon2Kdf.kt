package com.securevault.crypto

data class Argon2Config(
    val memoryKB: Int = CryptoConstants.Argon2.DEFAULT_MEMORY_KB,
    val iterations: Int = CryptoConstants.Argon2.DEFAULT_ITERATIONS,
    val parallelism: Int = CryptoConstants.Argon2.DEFAULT_PARALLELISM,
    val outputLength: Int = CryptoConstants.Argon2.DEFAULT_OUTPUT_LENGTH
) {
    init {
        require(memoryKB >= CryptoConstants.Argon2.MIN_MEMORY_KB) {
            "Memory must be at least ${CryptoConstants.Argon2.MIN_MEMORY_KB} KB"
        }
        require(iterations >= CryptoConstants.Argon2.MIN_ITERATIONS) {
            "Iterations must be at least ${CryptoConstants.Argon2.MIN_ITERATIONS}"
        }
        require(parallelism >= CryptoConstants.Argon2.MIN_PARALLELISM) {
            "Parallelism must be at least ${CryptoConstants.Argon2.MIN_PARALLELISM}"
        }
    }
}

expect class Argon2Kdf() {
    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        config: Argon2Config = Argon2Config()
    ): ByteArray

    fun generateSalt(size: Int = CryptoConstants.SALT_SIZE): ByteArray
}