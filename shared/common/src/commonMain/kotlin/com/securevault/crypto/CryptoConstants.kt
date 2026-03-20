package com.securevault.crypto

object CryptoConstants {
    const val AES_KEY_SIZE = 32
    const val XCHACHA_IV_SIZE = 24
    const val AEAD_TAG_SIZE = 16
    const val GCM_TAG_SIZE = AEAD_TAG_SIZE
    const val SALT_SIZE = 16
    const val BLOCK_SIZE = 256
    const val MIN_DATA_SIZE = 1
    const val MAX_DATA_SIZE = 1024 * 1024 * 10

    const val STORAGE_FORMAT_V2_PREFIX = "v2"
    const val STORAGE_FORMAT_V1_PREFIX = "v1"
    const val CURRENT_STORAGE_FORMAT = STORAGE_FORMAT_V2_PREFIX

    object Argon2 {
        const val DEFAULT_MEMORY_KB = 131072
        const val DEFAULT_ITERATIONS = 3
        const val DEFAULT_PARALLELISM = 4
        const val DEFAULT_OUTPUT_LENGTH = 32
        const val MIN_MEMORY_KB = 8192
        const val MIN_ITERATIONS = 1
        const val MIN_PARALLELISM = 1
    }

    object MemorySanitizer {
        const val DEFAULT_WIPE_PASSES = 3
    }

    object Clipboard {
        const val DEFAULT_CLEAR_TIMEOUT_MS = 30_000L
    }

    object Session {
        const val DEFAULT_LOCK_TIMEOUT_MS = 300_000L
        const val IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS = -1L
    }
}