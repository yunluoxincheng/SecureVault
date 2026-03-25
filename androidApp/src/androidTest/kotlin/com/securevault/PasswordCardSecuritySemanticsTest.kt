package com.securevault

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.PasswordCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordCardSecuritySemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<DebugTestActivity>()

    private companion object {
        const val GLOBAL_SECURITY_DESC = "\u5168\u5c40\u5b89\u5168\u6a21\u5f0f\u6761\u76ee"
        const val ENTRY_SECURITY_DESC = "\u6761\u76ee\u5b89\u5168\u6a21\u5f0f"
    }

    @Test
    fun globalSecurityMode_shouldExposeGlobalSemantics() {
        composeRule.setContent {
            MaterialTheme {
                PasswordCard(
                    entry = sampleEntry(securityMode = false),
                    securityModeEnabled = true,
                    onClick = {},
                    animateEntrance = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription(GLOBAL_SECURITY_DESC).assertIsDisplayed()
    }

    @Test
    fun entrySecurityMode_shouldExposeEntrySemantics() {
        composeRule.setContent {
            MaterialTheme {
                PasswordCard(
                    entry = sampleEntry(securityMode = true),
                    securityModeEnabled = false,
                    onClick = {},
                    animateEntrance = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription(ENTRY_SECURITY_DESC).assertIsDisplayed()
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
