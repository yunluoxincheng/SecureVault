package com.securevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.securevault.data.PasswordEntry
import com.securevault.ui.screens.AddEditPasswordScreen
import com.securevault.ui.screens.PasswordDetailScreen
import com.securevault.ui.screens.SettingsScreen
import com.securevault.ui.screens.SetupScreen
import com.securevault.ui.screens.UnlockScreen
import com.securevault.ui.screens.VaultScreen
import com.securevault.ui.theme.SecureVaultTheme
import com.securevault.ui.theme.ThemeMode

private enum class Route {
    Unlock,
    Setup,
    Vault,
    Detail,
    AddEdit,
    Settings
}

@Composable
fun SecureVaultApp() {
    var route by remember { mutableStateOf(Route.Unlock) }
    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }

    val entries = remember {
        mutableStateListOf(
            PasswordEntry(
                id = 1L,
                title = "GitHub",
                username = "dev@securevault.com",
                password = "P@ssw0rd!",
                url = "https://github.com",
                notes = "开发账号",
                tags = listOf("dev", "work"),
                category = "工作",
                isFavorite = true,
                securityMode = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            PasswordEntry(
                id = 2L,
                title = "Bank",
                username = "me@example.com",
                password = "********",
                url = "https://bank.example.com",
                notes = "安全模式示例",
                tags = listOf("finance"),
                category = "金融",
                isFavorite = false,
                securityMode = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    SecureVaultTheme(themeMode = themeMode) {
        when (route) {
            Route.Unlock -> UnlockScreen(
                onUnlock = { route = Route.Vault },
                onSetupClick = { route = Route.Setup }
            )

            Route.Setup -> SetupScreen(
                onSetupDone = { route = Route.Vault },
                onBack = { route = Route.Unlock }
            )

            Route.Vault -> VaultScreen(
                entries = entries,
                onEntryClick = { entry ->
                    selectedEntryId = entry.id
                    route = Route.Detail
                },
                onAddClick = {
                    selectedEntryId = null
                    route = Route.AddEdit
                },
                onSettingsClick = { route = Route.Settings }
            )

            Route.Detail -> {
                val entry = entries.firstOrNull { it.id == selectedEntryId }
                if (entry == null) {
                    route = Route.Vault
                } else {
                    PasswordDetailScreen(
                        entry = entry,
                        onBack = { route = Route.Vault },
                        onEdit = { route = Route.AddEdit },
                        onDelete = {
                            entries.removeAll { it.id == entry.id }
                            route = Route.Vault
                        }
                    )
                }
            }

            Route.AddEdit -> {
                val currentEntry = entries.firstOrNull { it.id == selectedEntryId }
                AddEditPasswordScreen(
                    entry = currentEntry,
                    onSave = { updated ->
                        val index = entries.indexOfFirst { it.id == updated.id }
                        if (index >= 0) {
                            entries[index] = updated
                        } else {
                            val nextId = (entries.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1L
                            entries.add(updated.copy(id = nextId))
                        }
                        route = Route.Vault
                    },
                    onCancel = { route = if (currentEntry == null) Route.Vault else Route.Detail }
                )
            }

            Route.Settings -> SettingsScreen(
                currentTheme = themeMode,
                onThemeChange = { themeMode = it },
                onBack = { route = Route.Vault },
                onLock = { route = Route.Unlock }
            )
        }
    }
}
