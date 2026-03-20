package com.securevault

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.security.SessionManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class SessionLifecycleLockTest {

    @Test
    fun app_background_then_foreground_should_lock_when_timeout_exceeded() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val koin = GlobalContext.get()
            val sessionManager = koin.get<SessionManager>()

            sessionManager.setLockTimeout(1L)
            sessionManager.unlock(ByteArray(32) { it.toByte() })
            assertTrue(sessionManager.isUnlocked())

            scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(20)
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertFalse(sessionManager.isUnlocked())
        }
    }
}
