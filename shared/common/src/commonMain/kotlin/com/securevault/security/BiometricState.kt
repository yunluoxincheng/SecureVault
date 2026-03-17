package com.securevault.security

data class BiometricState(
    val maxFailedAttempts: Int = 5,
    val lockoutDurationMs: Long = 30_000L,
    val debounceMs: Long = 500L
) {
    private var failedAttempts = 0
    private var lastFailedTime = 0L
    private var lastAuthTime = 0L
    private var isLockedOut = false

    fun recordSuccess() {
        failedAttempts = 0
        isLockedOut = false
        lastAuthTime = System.currentTimeMillis()
    }

    fun recordFailure(): Boolean {
        failedAttempts++
        lastFailedTime = System.currentTimeMillis()

        if (failedAttempts >= maxFailedAttempts) {
            isLockedOut = true
        }

        return isLockedOut
    }

    fun isLockedOut(): Boolean {
        if (!isLockedOut) return false

        val elapsed = System.currentTimeMillis() - lastFailedTime
        if (elapsed >= lockoutDurationMs) {
            isLockedOut = false
            failedAttempts = 0
            return false
        }

        return true
    }

    fun canAuthenticate(): Boolean {
        if (isLockedOut()) return false

        val elapsed = System.currentTimeMillis() - lastAuthTime
        return elapsed >= debounceMs
    }

    fun reset() {
        failedAttempts = 0
        lastFailedTime = 0L
        lastAuthTime = 0L
        isLockedOut = false
    }

    fun getRemainingAttempts(): Int {
        return maxOf(0, maxFailedAttempts - failedAttempts)
    }

    fun getLockoutRemainingMs(): Long {
        if (!isLockedOut) return 0
        val elapsed = System.currentTimeMillis() - lastFailedTime
        return maxOf(0L, lockoutDurationMs - elapsed)
    }
}