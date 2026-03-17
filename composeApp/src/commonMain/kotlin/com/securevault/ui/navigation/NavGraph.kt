package com.securevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.securevault.ui.screens.AddEditPasswordScreen
import com.securevault.ui.screens.GeneratorScreen
import com.securevault.ui.screens.PasswordDetailScreen
import com.securevault.ui.screens.SettingsScreen
import com.securevault.ui.screens.SetupScreen
import com.securevault.ui.screens.UnlockScreen
import com.securevault.ui.screens.VaultScreen
import com.securevault.ui.theme.SecureVaultTheme
import com.securevault.util.PasswordPreset
import com.securevault.viewmodel.AddEditPasswordViewModel
import com.securevault.viewmodel.GeneratorViewModel
import com.securevault.viewmodel.PasswordDetailViewModel
import com.securevault.viewmodel.SettingsViewModel
import com.securevault.viewmodel.UnlockViewModel
import com.securevault.viewmodel.VaultViewModel
import org.koin.compose.koinInject

private enum class Route {
    Unlock,
    Setup,
    Vault,
    Detail,
    AddEdit,
    Generator,
    Settings
}

@Composable
fun SecureVaultApp() {
    val unlockViewModel: UnlockViewModel = koinInject()
    val vaultViewModel: VaultViewModel = koinInject()
    val detailViewModel: PasswordDetailViewModel = koinInject()
    val addEditViewModel: AddEditPasswordViewModel = koinInject()
    val settingsViewModel: SettingsViewModel = koinInject()
    val generatorViewModel: GeneratorViewModel = koinInject()

    val unlockState by unlockViewModel.uiState.collectAsState()
    val vaultState by vaultViewModel.uiState.collectAsState()
    val detailState by detailViewModel.uiState.collectAsState()
    val addEditState by addEditViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val generatorState by generatorViewModel.uiState.collectAsState()

    var route by remember { mutableStateOf(Route.Unlock) }
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(route, selectedEntryId) {
        when (route) {
            Route.Unlock -> unlockViewModel.refreshState()
            Route.Vault -> vaultViewModel.loadEntries()
            Route.Detail -> selectedEntryId?.let { detailViewModel.load(it) }
            Route.AddEdit -> addEditViewModel.loadEntry(selectedEntryId)
            Route.Settings -> settingsViewModel.load()
            Route.Generator -> Unit
            Route.Setup -> Unit
        }
    }

    LaunchedEffect(unlockState.isUnlocked) {
        if (unlockState.isUnlocked) {
            route = Route.Vault
            unlockViewModel.consumeUnlockEvent()
            vaultViewModel.loadEntries()
        }
    }

    LaunchedEffect(addEditState.saveSuccess) {
        if (addEditState.saveSuccess) {
            route = Route.Vault
            addEditViewModel.consumeSaveResult()
            vaultViewModel.loadEntries()
        }
    }

    LaunchedEffect(detailState.deleted) {
        if (detailState.deleted) {
            route = Route.Vault
            vaultViewModel.loadEntries()
        }
    }

    SecureVaultTheme(themeMode = settingsState.themeMode) {
        when (route) {
            Route.Unlock -> UnlockScreen(
                biometricAvailable = unlockState.biometricAvailable,
                onUnlock = { password -> unlockViewModel.unlockWithPassword(password) },
                onBiometricUnlock = { unlockViewModel.unlockWithBiometric() },
                onSetupClick = { route = Route.Setup }
            )

            Route.Setup -> SetupScreen(
                onSetupDone = { password -> unlockViewModel.setupVault(password) },
                onBack = { route = Route.Unlock }
            )

            Route.Vault -> VaultScreen(
                entries = vaultState.entries,
                query = vaultState.query,
                onQueryChange = { vaultViewModel.updateQuery(it) },
                onEntryClick = { entry ->
                    selectedEntryId = entry.id
                    route = Route.Detail
                },
                onAddClick = {
                    selectedEntryId = null
                    route = Route.AddEdit
                },
                onGeneratorClick = { route = Route.Generator },
                onSettingsClick = { route = Route.Settings }
            )

            Route.Detail -> {
                val entry = detailState.entry
                if (entry == null) {
                    if (selectedEntryId != null) {
                        detailViewModel.load(selectedEntryId!!)
                    }
                } else {
                    PasswordDetailScreen(
                        entry = entry,
                        onBack = { route = Route.Vault },
                        onEdit = { route = Route.AddEdit },
                        onDelete = { detailViewModel.delete() },
                        onCopyUsername = { detailViewModel.copyUsername() },
                        onCopyPassword = { detailViewModel.copyPassword() }
                    )
                }
            }

            Route.AddEdit -> {
                AddEditPasswordScreen(
                    entry = addEditState.entry,
                    onSave = { updated -> addEditViewModel.save(updated) },
                    onCancel = { route = if (selectedEntryId == null) Route.Vault else Route.Detail },
                    onGeneratePassword = {
                        generatorViewModel.generateWithPreset(PasswordPreset.Strong)
                    }
                )
            }

            Route.Settings -> SettingsScreen(
                currentTheme = settingsState.themeMode,
                biometricEnabled = settingsState.biometricEnabled,
                onThemeChange = { settingsViewModel.updateTheme(it) },
                onBiometricChange = { settingsViewModel.updateBiometricEnabled(it) },
                onBack = { route = Route.Vault },
                onLock = {
                    settingsViewModel.lockNow()
                    route = Route.Unlock
                }
            )

            Route.Generator -> GeneratorScreen(
                uiState = generatorState,
                onBack = { route = Route.Vault },
                onGeneratePreset = { preset ->
                    generatorViewModel.generateWithPreset(preset)
                },
                onGenerateCustom = { config ->
                    generatorViewModel.generateWithConfig(config)
                },
                onCopyGenerated = { generatorViewModel.copyGeneratedPassword() }
            )
        }
    }
}
