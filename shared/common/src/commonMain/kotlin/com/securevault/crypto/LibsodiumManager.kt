package com.securevault.crypto

import kotlinx.coroutines.runBlocking

object LibsodiumManager {
    private var isInitialized = false

    suspend fun initialize() {
        if (!isInitialized) {
            com.ionspin.kotlin.crypto.LibsodiumInitializer.initialize()
            isInitialized = true
        }
    }

    fun ensureInitialized() {
        if (!isInitialized) {
            runBlocking {
                initialize()
            }
        }
    }

    fun isReady(): Boolean = isInitialized
}