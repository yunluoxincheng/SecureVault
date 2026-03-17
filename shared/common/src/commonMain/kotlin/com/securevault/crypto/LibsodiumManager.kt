package com.securevault.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer

object LibsodiumManager {
    private var isInitialized = false

    suspend fun initialize() {
        if (!isInitialized) {
            LibsodiumInitializer.load()
            isInitialized = true
        }
    }

    fun isReady(): Boolean = isInitialized
}