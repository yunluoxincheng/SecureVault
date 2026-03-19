package com.securevault.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.securevault.ui.components.MyAppBottomBar
import com.securevault.ui.components.MyAppBottomBarItem
import com.securevault.ui.components.SkeletonList
import com.securevault.ui.components.SvToastHost
import com.securevault.ui.screens.AddEditPasswordScreen
import com.securevault.ui.screens.GeneratorScreen
import com.securevault.ui.screens.LoginScreen
import com.securevault.ui.screens.OnboardingScreen
import com.securevault.ui.screens.PasswordDetailScreen
import com.securevault.ui.screens.RegisterScreen
import com.securevault.ui.screens.SecurityModeScreen
import com.securevault.ui.screens.SettingsScreen
import com.securevault.ui.screens.VaultScreen
import com.securevault.ui.theme.AppTheme
import com.securevault.ui.theme.spacing
import com.securevault.util.PasswordPreset
import com.securevault.viewmodel.AddEditPasswordViewModel
import com.securevault.viewmodel.AuthFlowViewModel
import com.securevault.viewmodel.AuthStartDestination
import com.securevault.viewmodel.GeneratorViewModel
import com.securevault.viewmodel.PasswordDetailViewModel
import com.securevault.viewmodel.SettingsViewModel
import com.securevault.viewmodel.SecurityModeViewModel
import com.securevault.viewmodel.UnlockViewModel
import com.securevault.viewmodel.VaultViewModel
import org.koin.compose.koinInject

private val mainTabRoutes = setOf(VaultRoute, GeneratorRoute, SettingsRoute)

private data class BottomTab(
    val route: MainTabRoute,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun SecureVaultApp() {
    val authFlowViewModel: AuthFlowViewModel = koinInject()
    val unlockViewModel: UnlockViewModel = koinInject()
    val vaultViewModel: VaultViewModel = koinInject()
    val detailViewModel: PasswordDetailViewModel = koinInject()
    val addEditViewModel: AddEditPasswordViewModel = koinInject()
    val settingsViewModel: SettingsViewModel = koinInject()
    val securityModeViewModel: SecurityModeViewModel = koinInject()
    val generatorViewModel: GeneratorViewModel = koinInject()

    val authState by authFlowViewModel.uiState.collectAsState()
    val unlockState by unlockViewModel.uiState.collectAsState()
    val vaultState by vaultViewModel.uiState.collectAsState()
    val detailState by detailViewModel.uiState.collectAsState()
    val addEditState by addEditViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val securityModeState by securityModeViewModel.uiState.collectAsState()
    val generatorState by generatorViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    AppTheme(themeMode = settingsState.themeMode) {
        if (authState.isLoading) {
            Scaffold { paddingValues ->
                SkeletonList(
                    count = 7,
                    modifier = Modifier.padding(paddingValues).padding(MaterialTheme.spacing.md),
                )
            }
            return@AppTheme
        }

        val startRoute = when (authState.startDestination) {
            AuthStartDestination.Onboarding -> OnboardingRoute
            AuthStartDestination.Register -> RegisterRoute
            AuthStartDestination.Login -> LoginRoute
        }
        val navigationState = rememberNavigationState(
            authStartRoute = startRoute,
            mainTabs = mainTabRoutes,
        )
        val navigator = remember(navigationState) { Navigator(navigationState) }
        var vaultVisitNonce by remember { mutableIntStateOf(0) }

        val bottomTabs = remember {
            listOf(
                BottomTab(VaultRoute, "密码库", Icons.Default.Lock),
                BottomTab(GeneratorRoute, "生成器", Icons.Default.Key),
                BottomTab(SettingsRoute, "设置", Icons.Default.Settings),
            )
        }

        LaunchedEffect(startRoute) {
            if (navigationState.currentSurface == NavigationSurface.Auth) {
                navigationState.resetAuthStack(startRoute)
            }
        }

        val currentRoute = navigationState.activeBackStack.lastOrNull()
        val showBottomBar = navigationState.currentSurface == NavigationSurface.Main

        LaunchedEffect(navigationState.currentSurface, navigationState.currentTab, currentRoute) {
            val isAtVaultRoot = navigationState.currentSurface == NavigationSurface.Main &&
                navigationState.currentTab == VaultRoute &&
                currentRoute == VaultRoute
            if (isAtVaultRoot) {
                vaultVisitNonce += 1
            }
        }

        LaunchedEffect(unlockState.isUnlocked) {
            if (unlockState.isUnlocked) {
                navigator.resetToMainRoot(VaultRoute)
                unlockViewModel.consumeUnlockEvent()
                vaultViewModel.loadEntries()
                authFlowViewModel.markVaultSetupCompleted()
            }
        }

        LaunchedEffect(addEditState.saveSuccess) {
            if (addEditState.saveSuccess) {
                navigator.resetToMainRoot(VaultRoute)
                addEditViewModel.consumeSaveResult()
                vaultViewModel.loadEntries()
                snackbarHostState.showSnackbar("已保存")
            }
        }

        LaunchedEffect(detailState.deleted) {
            if (detailState.deleted) {
                navigator.resetToMainRoot(VaultRoute)
                vaultViewModel.loadEntries()
            }
        }

        LaunchedEffect(detailState.message) {
            detailState.message?.let { snackbarHostState.showSnackbar(it) }
        }

        LaunchedEffect(securityModeState.message) {
            securityModeState.message?.let {
                snackbarHostState.showSnackbar(it)
                securityModeViewModel.consumeMessage()
            }
        }

        val destinationProvider = entryProvider<NavRoute> {
            entry<OnboardingRoute> {
                OnboardingScreen(
                    onFinish = {
                        authFlowViewModel.completeOnboarding()
                        navigator.resetToAuth(RegisterRoute)
                    }
                )
            }

            entry<RegisterRoute> {
                RegisterScreen(
                    isLoading = unlockState.isLoading,
                    errorMessage = unlockState.errorMessage,
                    onRegister = { password -> unlockViewModel.setupVault(password) },
                    onGoLogin = { navigator.resetToAuth(LoginRoute) },
                )
            }

            entry<LoginRoute> {
                LoginScreen(
                    biometricAvailable = unlockState.biometricAvailable && settingsState.biometricEnabled,
                    isLoading = unlockState.isLoading,
                    errorMessage = unlockState.errorMessage,
                    onLogin = { password -> unlockViewModel.unlockWithPassword(password) },
                    onBiometricLogin = { unlockViewModel.unlockWithBiometric() },
                    onGoRegister = { navigator.navigate(RegisterRoute) },
                )
            }

            entry<VaultRoute> {
                LaunchedEffect(Unit) { vaultViewModel.loadEntries() }
                VaultScreen(
                    entries = vaultState.entries,
                    categories = vaultState.categories,
                    selectedCategory = vaultState.selectedCategory,
                    favoritesOnly = vaultState.favoritesOnly,
                    query = vaultState.query,
                    securityModeEnabled = securityModeState.enabled,
                    vaultVisitNonce = vaultVisitNonce,
                    isLoading = vaultState.isLoading,
                    hasLoadedAtLeastOnce = vaultState.hasLoadedAtLeastOnce,
                    onQueryChange = { vaultViewModel.updateQuery(it) },
                    onFiltersChange = { category, favoritesOnly ->
                        vaultViewModel.updateFilters(
                            category = category,
                            favoritesOnly = favoritesOnly,
                        )
                    },
                    onEntryClick = { entry -> navigator.navigate(DetailRoute(entry.id)) },
                    onAddClick = { navigator.navigate(AddEditRoute(null)) },
                )
            }

            entry<GeneratorRoute> {
                GeneratorScreen(
                    uiState = generatorState,
                    onGeneratePreset = { preset -> generatorViewModel.generateWithPreset(preset) },
                    onGenerateCustom = { config -> generatorViewModel.generateWithConfig(config) },
                    onCopyGenerated = { generatorViewModel.copyGeneratedPassword() }
                )
            }

            entry<SettingsRoute> {
                SettingsScreen(
                    currentTheme = settingsState.themeMode,
                    biometricEnabled = settingsState.biometricEnabled,
                    screenshotAllowed = settingsState.screenshotAllowed,
                    errorMessage = settingsState.errorMessage,
                    onThemeChange = { settingsViewModel.updateTheme(it) },
                    onBiometricChange = { settingsViewModel.updateBiometricEnabled(it) },
                    onScreenshotAllowedChange = { settingsViewModel.updateScreenshotAllowed(it) },
                    onOpenSecurityMode = { navigator.navigate(SecurityModeRoute) },
                    onLock = {
                        settingsViewModel.lockNow()
                        navigator.resetToAuth(LoginRoute)
                    }
                )
            }

            entry<SecurityModeRoute> {
                SecurityModeScreen(
                    enabled = securityModeState.enabled,
                    isLoading = securityModeState.isLoading,
                    showPasswordVerificationDialog = securityModeState.showPasswordVerificationDialog,
                    message = securityModeState.message,
                    onEnabledChange = { securityModeViewModel.updateEnabled(it) },
                    onConfirmDisableWithPassword = { securityModeViewModel.confirmDisableWithPassword(it) },
                    onDismissPasswordDialog = { securityModeViewModel.dismissPasswordVerificationDialog() },
                    onBack = { navigator.goBack() },
                )
            }

            entry<DetailRoute> { key ->
                val id = key.entryId ?: return@entry
                LaunchedEffect(id) { detailViewModel.load(id) }
                val detailEntry = detailState.entry
                if (detailEntry != null) {
                    PasswordDetailScreen(
                        entry = detailEntry,
                        securityModeEnabled = securityModeState.enabled,
                        isLoading = detailState.isLoading,
                        pendingVerificationAction = detailState.pendingVerificationAction,
                        verifiedAction = detailState.verifiedAction,
                        onBack = { navigator.goBack() },
                        onEdit = { navigator.navigate(AddEditRoute(detailEntry.id)) },
                        onDelete = { detailViewModel.delete() },
                        onRequestSensitiveActionVerification = {
                            detailViewModel.requestSensitiveActionVerification(it)
                        },
                        onVerifySensitiveActionWithPassword = {
                            detailViewModel.verifySensitiveActionWithPassword(it)
                        },
                        onDismissSensitiveActionVerification = {
                            detailViewModel.dismissSensitiveActionVerification()
                        },
                        onConsumeVerifiedAction = { detailViewModel.consumeVerifiedAction() },
                        onCopyUsername = { detailViewModel.copyUsername() },
                        onCopyPassword = { detailViewModel.copyPassword() },
                    )
                }
            }

            entry<AddEditRoute> { key ->
                LaunchedEffect(key.entryId) { addEditViewModel.loadEntry(key.entryId) }
                AddEditPasswordScreen(
                    entry = addEditState.entry,
                    securityModeEnabled = securityModeState.enabled,
                    onSave = { updated -> addEditViewModel.save(updated) },
                    onCancel = { navigator.goBack() },
                    onGeneratePassword = { generatorViewModel.generateWithPreset(PasswordPreset.Strong) }
                )
            }
        }

        val navEntries = rememberDecoratedNavEntries(
            backStack = navigationState.activeBackStack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            entryProvider = destinationProvider,
        )

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    MyAppBottomBar(
                        items = bottomTabs.map { tab ->
                            MyAppBottomBarItem(
                                value = tab.route,
                                label = tab.label,
                                icon = tab.icon,
                            )
                        },
                        selectedItem = navigationState.currentTab,
                        onItemSelected = { route -> navigator.navigate(route) },
                    )
                }
            },
            snackbarHost = { SvToastHost(snackbarHostState) },
        ) { paddingValues ->
            NavDisplay(
                entries = navEntries,
                onBack = { navigator.goBack() },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}
