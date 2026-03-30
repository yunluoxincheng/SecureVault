package com.securevault.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object LibsodiumManager {
    @Volatile
    private var isInitialized = false
    private val initMutex = Mutex()

    suspend fun initialize() {
        if (isInitialized) return
        initMutex.withLock {
            if (isInitialized) return@withLock
            com.ionspin.kotlin.crypto.LibsodiumInitializer.initialize()
            isInitialized = true
        }
    }

    /**
     * Blocks the calling thread until sodium is ready. Heavy work runs on [Dispatchers.Default]
     * inside the waiter; prefer [initialize] from [Application] / process start so first UI crypto
     * rarely contends here.
     */
    fun ensureInitialized() {
        if (isInitialized) return
        runBlocking(Dispatchers.Default) {
            initialize()
        }
    }

    fun isReady(): Boolean = isInitialized
}