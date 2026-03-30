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

/**
 * Owns unlocked vault key material and session timing. All public instance methods use the same
 * intrinsic lock (`synchronized(this)`), so cross-thread callers (UI lifecycle, [KeyManager] suspend
 * paths, Autofill) serialize mutations and reads of session fields without a global lock ordering
 * beyond this object.
 */
class SessionManager {
    private var dataKey: SensitiveData<ByteArray>? = null
    private var isUnlocked = false
    private var lastActivityTime = System.currentTimeMillis()
    private var backgroundEnteredAtMs: Long? = null
    private var lockTimeoutMs = CryptoConstants.Session.DEFAULT_LOCK_TIMEOUT_MS
    private var skipImmediateBackgroundLockUntilMs: Long = 0L

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Locked)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun unlock(dataKey: ByteArray) {
        synchronized(this) {
            this.dataKey?.close()
            this.dataKey = SensitiveData.ofByteArray(dataKey)
            this.isUnlocked = true
            this.lastActivityTime = System.currentTimeMillis()
            this.backgroundEnteredAtMs = null
            _sessionState.value = SessionState.Unlocked
        }
    }

    fun lock() {
        synchronized(this) {
            dataKey?.close()
            dataKey = null
            isUnlocked = false
            backgroundEnteredAtMs = null
            _sessionState.value = SessionState.Locked
        }
    }

    fun getDataKey(): ByteArray {
        synchronized(this) {
            checkUnlocked()
            lastActivityTime = System.currentTimeMillis()
            return dataKey?.get()?.copyOf() ?: throw IllegalStateException("DataKey is null")
        }
    }

    /**
     * Public guard for callers that do not already hold this instance's monitor.
     */
    fun requireUnlocked() {
        synchronized(this) {
            checkUnlocked()
        }
    }

    private fun checkUnlocked() {
        check(isUnlocked) { "Session is locked" }
    }

    fun isUnlocked(): Boolean = synchronized(this) { isUnlocked }

    fun extendSession() {
        synchronized(this) {
            checkUnlocked()
            lastActivityTime = System.currentTimeMillis()
        }
    }

    fun setLockTimeout(timeoutMs: Long) {
        synchronized(this) {
            lockTimeoutMs = timeoutMs
        }
    }

    fun allowImmediateBackgroundLockBypass(durationMs: Long) {
        synchronized(this) {
            if (durationMs <= 0L) return
            skipImmediateBackgroundLockUntilMs = System.currentTimeMillis() + durationMs
        }
    }

    fun onAppBackground(): Boolean {
        synchronized(this) {
            if (!isUnlocked) return false

            if (lockTimeoutMs == CryptoConstants.Session.IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS) {
                val now = System.currentTimeMillis()
                if (skipImmediateBackgroundLockUntilMs > now) {
                    skipImmediateBackgroundLockUntilMs = 0L
                    backgroundEnteredAtMs = now
                    return false
                }
                lock()
                return true
            }

            backgroundEnteredAtMs = System.currentTimeMillis()
            return false
        }
    }

    fun onAppForeground(): Boolean {
        synchronized(this) {
            if (!isUnlocked) {
                backgroundEnteredAtMs = null
                return false
            }

            val enteredAt = backgroundEnteredAtMs ?: return false
            backgroundEnteredAtMs = null

            if (lockTimeoutMs <= 0) {
                lastActivityTime = System.currentTimeMillis()
                return false
            }

            val elapsedInBackground = System.currentTimeMillis() - enteredAt
            if (elapsedInBackground >= lockTimeoutMs) {
                lock()
                return true
            }

            lastActivityTime = System.currentTimeMillis()
            return false
        }
    }

    fun checkAutoLock(): Boolean {
        synchronized(this) {
            if (!isUnlocked || lockTimeoutMs <= 0) return false

            val elapsed = System.currentTimeMillis() - lastActivityTime
            if (elapsed >= lockTimeoutMs) {
                lock()
                return true
            }
            return false
        }
    }

    fun clear() {
        synchronized(this) {
            lock()
            skipImmediateBackgroundLockUntilMs = 0L
        }
    }
}

sealed class SessionState {
    object Locked : SessionState()
    object Unlocked : SessionState()
    data class Error(val message: String) : SessionState()
}