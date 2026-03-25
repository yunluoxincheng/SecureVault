package com.securevault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.data.VaultExportMode
import com.securevault.data.VaultImportConflictStrategy
import com.securevault.ui.screens.SettingsScreen
import com.securevault.ui.screens.VaultSettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsVaultEntrySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<DebugTestActivity>()

    private companion object {
        const val EXPORT_BUTTON_LABEL = "\u5bfc\u51fa\u5bc6\u7801\u5e93"
        const val IMPORT_BUTTON_LABEL = "\u5bfc\u5165\u5bc6\u7801\u5e93"
    }

    @Test
    fun settings_to_vaultSettings_should_show_export_import_actions() {
        composeRule.setContent {
            var openVaultSettings by mutableStateOf(true)

            if (openVaultSettings) {
                VaultSettingsScreen(
                    exportMode = VaultExportMode.SecureMode,
                    importConflictStrategy = VaultImportConflictStrategy.Skip,
                    isExporting = false,
                    isImporting = false,
                    onExportModeChange = {},
                    onImportConflictStrategyChange = {},
                    onExportClick = {},
                    onImportClick = {},
                    onBack = { openVaultSettings = false },
                )
            } else {
                SettingsScreen(
                    onOpenAccountSecuritySettings = {},
                    onOpenAppearanceSettings = {},
                    onOpenAutofillSettings = {},
                    onOpenVaultSettings = { openVaultSettings = true },
                    onOpenAboutSettings = {},
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(EXPORT_BUTTON_LABEL).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(IMPORT_BUTTON_LABEL).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(EXPORT_BUTTON_LABEL).assertIsDisplayed()
        composeRule.onNodeWithText(IMPORT_BUTTON_LABEL).assertIsDisplayed()
    }
}
