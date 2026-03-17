package com.securevault.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BiometricStateTest {

    @Test
    fun initially_notLockedOut() {
        val state = BiometricState()
        assertFalse(state.isLockedOut())
    }

    @Test
    fun canAuthenticate_initiallyTrue() {
        val state = BiometricState()
        assertTrue(state.canAuthenticate())
    }

    @Test
    fun recordFailure_incrementsAttempts() {
        val state = BiometricState(maxFailedAttempts = 3)
        state.recordFailure()
        assertEquals(2, state.getRemainingAttempts())
    }

    @Test
    fun lockout_afterMaxFailures() {
        val state = BiometricState(maxFailedAttempts = 3)
        state.recordFailure()
        assertFalse(state.isLockedOut())
        state.recordFailure()
        assertFalse(state.isLockedOut())
        state.recordFailure()
        assertTrue(state.isLockedOut())
    }

    @Test
    fun recordSuccess_resetsFailures() {
        val state = BiometricState(maxFailedAttempts = 3)
        state.recordFailure()
        state.recordFailure()
        state.recordSuccess()
        assertEquals(3, state.getRemainingAttempts())
        assertFalse(state.isLockedOut())
    }

    @Test
    fun reset_clearsAllState() {
        val state = BiometricState(maxFailedAttempts = 2)
        state.recordFailure()
        state.recordFailure()
        state.reset()
        assertFalse(state.isLockedOut())
        assertEquals(2, state.getRemainingAttempts())
    }

    @Test
    fun getRemainingAttempts_correctCount() {
        val state = BiometricState(maxFailedAttempts = 5)
        assertEquals(5, state.getRemainingAttempts())
        state.recordFailure()
        assertEquals(4, state.getRemainingAttempts())
        state.recordFailure()
        state.recordFailure()
        assertEquals(2, state.getRemainingAttempts())
    }
}