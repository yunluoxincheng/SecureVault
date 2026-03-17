package com.securevault.security

import com.securevault.crypto.CryptoConstants

class MemorySanitizer {
    companion object {
        fun wipe(data: ByteArray, passes: Int = CryptoConstants.MemorySanitizer.DEFAULT_WIPE_PASSES) {
            require(passes > 0) { "Passes must be positive" }
            for (pass in 0 until passes) {
                for (i in data.indices) {
                    data[i] = (pass % 256).toByte()
                }
            }
            for (i in data.indices) {
                data[i] = 0
            }
        }

        fun wipe(data: CharArray, passes: Int = CryptoConstants.MemorySanitizer.DEFAULT_WIPE_PASSES) {
            require(passes > 0) { "Passes must be positive" }
            for (pass in 0 until passes) {
                for (i in data.indices) {
                    data[i] = ((pass % 256) + 1).toChar()
                }
            }
            for (i in data.indices) {
                data[i] = '\u0000'
            }
        }

        fun wipe(data: IntArray, passes: Int = CryptoConstants.MemorySanitizer.DEFAULT_WIPE_PASSES) {
            require(passes > 0) { "Passes must be positive" }
            for (pass in 0 until passes) {
                for (i in data.indices) {
                    data[i] = pass
                }
            }
            for (i in data.indices) {
                data[i] = 0
            }
        }

        fun isWiped(data: ByteArray): Boolean {
            return data.all { it == 0.toByte() }
        }

        fun isWiped(data: CharArray): Boolean {
            return data.all { it == '\u0000' }
        }
    }
}