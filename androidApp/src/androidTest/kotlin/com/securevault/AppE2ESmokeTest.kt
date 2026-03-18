package com.securevault

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppE2ESmokeTest {

    @Test
    fun app_launch_and_recreate_should_not_crash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }
}
