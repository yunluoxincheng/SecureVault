package com.securevault.crypto

object AdaptiveArgon2Config {
    private const val TARGET_TIME_MS = 2000L
    private const val MIN_TIME_MS = 500L
    private const val MAX_TIME_MS = 4000L

    fun getStandardConfig(): Argon2Config {
        return Argon2Config(
            memoryKB = CryptoConstants.Argon2.DEFAULT_MEMORY_KB,
            iterations = CryptoConstants.Argon2.DEFAULT_ITERATIONS,
            parallelism = CryptoConstants.Argon2.DEFAULT_PARALLELISM
        )
    }

    fun getLowEndConfig(): Argon2Config {
        return Argon2Config(
            memoryKB = 65536,
            iterations = 2,
            parallelism = 2
        )
    }

    fun getHighEndConfig(): Argon2Config {
        return Argon2Config(
            memoryKB = 262144,
            iterations = 4,
            parallelism = 8
        )
    }

    fun getConfigForAvailableMemory(availableMemoryMB: Int): Argon2Config {
        return when {
            availableMemoryMB < 2048 -> getLowEndConfig()
            availableMemoryMB < 4096 -> getStandardConfig()
            else -> getHighEndConfig()
        }
    }

    fun adjustConfigForTime(
        baseConfig: Argon2Config,
        actualTimeMs: Long
    ): Argon2Config {
        return when {
            actualTimeMs < MIN_TIME_MS -> increaseSecurity(baseConfig)
            actualTimeMs > MAX_TIME_MS -> decreaseSecurity(baseConfig)
            else -> baseConfig
        }
    }

    private fun increaseSecurity(config: Argon2Config): Argon2Config {
        return config.copy(
            iterations = minOf(config.iterations + 1, 10),
            memoryKB = minOf(config.memoryKB * 2, 524288)
        )
    }

    private fun decreaseSecurity(config: Argon2Config): Argon2Config {
        return config.copy(
            iterations = maxOf(config.iterations - 1, 1),
            memoryKB = maxOf(config.memoryKB / 2, 8192)
        )
    }
}