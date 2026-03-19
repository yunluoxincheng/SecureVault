package com.securevault

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.PasswordCard
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordCardSecuritySemanticsTest {

    @Test
    fun globalSecurityMode_shouldExposeGlobalSemantics() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    MaterialTheme {
                        PasswordCard(
                            entry = sampleEntry(securityMode = false),
                            securityModeEnabled = true,
                            onClick = {},
                            animateEntrance = false,
                        )
                    }
                }
            }

            onView(withContentDescription("全局安全模式条目")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun entrySecurityMode_shouldExposeEntrySemantics() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    MaterialTheme {
                        PasswordCard(
                            entry = sampleEntry(securityMode = true),
                            securityModeEnabled = false,
                            onClick = {},
                            animateEntrance = false,
                        )
                    }
                }
            }

            onView(withContentDescription("条目安全模式")).check(matches(isDisplayed()))
        }
    }

    private fun sampleEntry(securityMode: Boolean): PasswordEntry {
        val now = System.currentTimeMillis()
        return PasswordEntry(
            id = 1L,
            title = "测试条目",
            username = "tester",
            password = "Pass#123",
            securityMode = securityMode,
            createdAt = now,
            updatedAt = now,
        )
    }
}
