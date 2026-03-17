package com.securevault.security

import com.securevault.crypto.CryptoConstants
import com.securevault.crypto.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensitiveData<T : Any> private constructor(
    private var data: T?,
    private val cleaner: (T) -> Unit
) {
    private var isClosed = false

    val isAvailable: Boolean
        get() = !isClosed && data != null

    fun get(): T {
        check(!isClosed) { "SensitiveData has been closed" }
        return data ?: throw IllegalStateException("Data is null")
    }

    fun use(block: (T) -> Unit) {
        try {
            block(get())
        } finally {
            close()
        }
    }

    fun close() {
        if (isClosed) return
        isClosed = true
        data?.let { cleaner(it) }
        data = null
    }

    companion object {
        fun ofByteArray(data: ByteArray): SensitiveData<ByteArray> {
            return SensitiveData(data.copyOf()) { MemorySanitizer.wipe(it) }
        }

        fun ofCharArray(data: CharArray): SensitiveData<CharArray> {
            return SensitiveData(data.copyOf()) { MemorySanitizer.wipe(it) }
        }

        fun <T : Any> of(data: T, cleaner: (T) -> Unit): SensitiveData<T> {
            return SensitiveData(data, cleaner)
        }
    }
}

class SessionManager {
    private var dataKey: SensitiveData<ByteArray>? = null
    private var isUnlocked = false
    private var lastActivityTime = System.currentTimeMillis()
    private var lockTimeoutMs = CryptoConstants.Session.DEFAULT_LOCK_TIMEOUT_MS

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Locked)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun unlock(dataKey: ByteArray) {
        this.dataKey?.close()
        this.dataKey = SensitiveData.ofByteArray(dataKey)
        this.isUnlocked = true
        this.lastActivityTime = System.currentTimeMillis()
        _sessionState.value = SessionState.Unlocked
    }

    fun lock() {
        dataKey?.close()
        dataKey = null
        isUnlocked = false
        _sessionState.value = SessionState.Locked
    }

    fun getDataKey(): ByteArray {
        requireUnlocked()
        lastActivityTime = System.currentTimeMillis()
        return dataKey?.get()?.copyOf() ?: throw IllegalStateException("DataKey is null")
    }

    fun requireUnlocked() {
        check(isUnlocked) { "Session is locked" }
    }

    fun isUnlocked(): Boolean = isUnlocked

    fun extendSession() {
        requireUnlocked()
        lastActivityTime = System.currentTimeMillis()
    }

    fun setLockTimeout(timeoutMs: Long) {
        lockTimeoutMs = timeoutMs
    }

    fun checkAutoLock(): Boolean {
        if (!isUnlocked || lockTimeoutMs <= 0) return false

        val elapsed = System.currentTimeMillis() - lastActivityTime
        if (elapsed >= lockTimeoutMs) {
            lock()
            return true
        }
        return false
    }

    fun clear() {
        lock()
    }
}

sealed class SessionState {
    object Locked : SessionState()
    object Unlocked : SessionState()
    data class Error(val message: String) : SessionState()
}