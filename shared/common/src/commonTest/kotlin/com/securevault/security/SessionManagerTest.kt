package com.securevault.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertContentEquals

class SessionManagerTest {

    @Test
    fun initially_locked() {
        val manager = SessionManager()
        assertFalse(manager.isUnlocked())
    }

    @Test
    fun unlock_setsUnlockedState() {
        val manager = SessionManager()
        val dataKey = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                                  17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)
        
        manager.unlock(dataKey)
        assertTrue(manager.isUnlocked())
    }

    @Test
    fun lock_clearsState() {
        val manager = SessionManager()
        val dataKey = ByteArray(32) { it.toByte() }
        
        manager.unlock(dataKey)
        manager.lock()
        
        assertFalse(manager.isUnlocked())
    }

    @Test
    fun getDataKey_whenLocked_throws() {
        val manager = SessionManager()
        assertFailsWith<IllegalStateException> {
            manager.getDataKey()
        }
    }

    @Test
    fun getDataKey_returnsCopy() {
        val manager = SessionManager()
        val dataKey = ByteArray(32) { it.toByte() }
        
        manager.unlock(dataKey)
        val copy1 = manager.getDataKey()
        val copy2 = manager.getDataKey()
        
        assertContentEquals(copy1, copy2)
    }

    @Test
    fun requireUnlocked_whenLocked_throws() {
        val manager = SessionManager()
        assertFailsWith<IllegalStateException> {
            manager.requireUnlocked()
        }
    }

    @Test
    fun requireUnlocked_whenUnlocked_succeeds() {
        val manager = SessionManager()
        manager.unlock(ByteArray(32))
        manager.requireUnlocked()
    }

    @Test
    fun checkAutoLock_whenTimeoutExceeded_locksSession() {
        val manager = SessionManager()
        manager.unlock(ByteArray(32))
        manager.setLockTimeout(1)

        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 5) {
            // busy wait for commonTest portability
        }

        assertTrue(manager.checkAutoLock())
        assertFalse(manager.isUnlocked())
    }

    @Test
    fun checkAutoLock_whenDisabled_doesNotLock() {
        val manager = SessionManager()
        manager.unlock(ByteArray(32))
        manager.setLockTimeout(0)

        assertFalse(manager.checkAutoLock())
        assertTrue(manager.isUnlocked())
    }

    @Test
    fun onAppForeground_whenBackgroundTimeoutExceeded_locksSession() {
        val manager = SessionManager()
        manager.unlock(ByteArray(32))
        manager.setLockTimeout(1)

        manager.onAppBackground()
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 5) {
            // busy wait for commonTest portability
        }

        assertTrue(manager.onAppForeground())
        assertFalse(manager.isUnlocked())
    }

    @Test
    fun onAppBackground_whenImmediateTimeoutEnabled_locksImmediately() {
        val manager = SessionManager()
        manager.unlock(ByteArray(32))
        manager.setLockTimeout(com.securevault.crypto.CryptoConstants.Session.IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS)

        assertTrue(manager.onAppBackground())
        assertFalse(manager.isUnlocked())
    }

    @Test
    fun concurrent_unlock_and_lock_leaves_consistent_state() = runBlocking {
        val manager = SessionManager()
        val key = ByteArray(32) { it.toByte() }
        coroutineScope {
            repeat(100) {
                launch(Dispatchers.Default) {
                    manager.unlock(key.copyOf())
                    delay(0)
                    manager.lock()
                }
            }
        }
        assertFalse(manager.isUnlocked())
    }
}