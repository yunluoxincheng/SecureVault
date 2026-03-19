package com.securevault.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.securevault.ui.animation.NavTransitions
import com.securevault.ui.components.SkeletonList
import com.securevault.ui.components.SvToastHost
import com.securevault.ui.screens.AddEditPasswordScreen
import com.securevault.ui.screens.GeneratorScreen
import com.securevault.ui.screens.LoginScreen
import com.securevault.ui.screens.OnboardingScreen
import com.securevault.ui.screens.PasswordDetailScreen
import com.securevault.ui.screens.RegisterScreen
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
import com.securevault.viewmodel.UnlockViewModel
import com.securevault.viewmodel.VaultViewModel
import org.koin.compose.koinInject

private object Route {
    const val Onboarding = "onboarding"
    const val Register = "register"
    const val Login = "login"

    const val Vault = "vault"
    const val Generator = "generator"
    const val Settings = "settings"

    const val Detail = "detail"
    const val AddEdit = "add-edit"
}

private val mainTabRoutes = setOf(Route.Vault, Route.Generator, Route.Settings)

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun SecureVaultApp() {
    val authFlowViewModel: AuthFlowViewModel = koinInject()
    val unlockViewModel: UnlockViewModel = koinInject()
    val vaultViewModel: VaultViewModel = koinInject()
    val detailViewModel: PasswordDetailViewModel = koinInject()
    val addEditViewModel: AddEditPasswordViewModel = koinInject()
    val settingsViewModel: SettingsViewModel = koinInject()
    val generatorViewModel: GeneratorViewModel = koinInject()

    val authState by authFlowViewModel.uiState.collectAsState()
    val unlockState by unlockViewModel.uiState.collectAsState()
    val vaultState by vaultViewModel.uiState.collectAsState()
    val detailState by detailViewModel.uiState.collectAsState()
    val addEditState by addEditViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
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
            AuthStartDestination.Onboarding -> Route.Onboarding
            AuthStartDestination.Register -> Route.Register
            AuthStartDestination.Login -> Route.Login
        }

        val navController = rememberNavController()
        val selectedEntryId = remember { mutableStateOf<Long?>(null) }
        var vaultVisitNonce by remember { mutableIntStateOf(0) }

        val bottomTabs = remember {
            listOf(
                BottomTab(Route.Vault, "密码库") { Icon(Icons.Default.Lock, contentDescription = null) },
                BottomTab(Route.Generator, "生成器") { Icon(Icons.Default.Key, contentDescription = null) },
                BottomTab(Route.Settings, "设置") { Icon(Icons.Default.Settings, contentDescription = null) },
            )
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val showBottomBar = backStackEntry
            ?.destination
            ?.hierarchy
            ?.any { it.route in mainTabRoutes } == true

        LaunchedEffect(backStackEntry?.destination?.route) {
            val isVaultDestination = backStackEntry
                ?.destination
                ?.hierarchy
                ?.any { it.route == Route.Vault } == true
            if (isVaultDestination) {
                vaultVisitNonce += 1
            }
        }

        LaunchedEffect(unlockState.isUnlocked) {
            if (unlockState.isUnlocked) {
                navController.navigate(Route.Vault) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
                unlockViewModel.consumeUnlockEvent()
                vaultViewModel.loadEntries()
                authFlowViewModel.markVaultSetupCompleted()
            }
        }

        LaunchedEffect(addEditState.saveSuccess) {
            if (addEditState.saveSuccess) {
                navController.popBackStack(Route.Vault, false)
                addEditViewModel.consumeSaveResult()
                vaultViewModel.loadEntries()
                snackbarHostState.showSnackbar("已保存")
            }
        }

        LaunchedEffect(detailState.deleted) {
            if (detailState.deleted) {
                navController.popBackStack(Route.Vault, false)
                vaultViewModel.loadEntries()
            }
        }

        LaunchedEffect(detailState.message) {
            detailState.message?.let { snackbarHostState.showSnackbar(it) }
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        val destination = backStackEntry?.destination
                        bottomTabs.forEach { tab ->
                            val isSelected = destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) return@NavigationBarItem
                                    navController.navigate(tab.route) {
                                        popUpTo(Route.Vault)
                                        launchSingleTop = true
                                    }
                                },
                                icon = tab.icon,
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
            snackbarHost = { SvToastHost(snackbarHostState) },
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(paddingValues),
                enterTransition = { NavTransitions.enterForward },
                exitTransition = { NavTransitions.exitForward },
                popEnterTransition = { NavTransitions.enterBackward },
                popExitTransition = { NavTransitions.exitBackward },
            ) {
                composable(
                    Route.Onboarding,
                    enterTransition = { NavTransitions.enterTab },
                    exitTransition = { NavTransitions.exitTab },
                ) {
                    OnboardingScreen(
                        onFinish = {
                            authFlowViewModel.completeOnboarding()
                            navController.navigate(Route.Register) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    Route.Register,
                    enterTransition = { NavTransitions.enterTab },
                    exitTransition = { NavTransitions.exitTab },
                ) {
                    RegisterScreen(
                        isLoading = unlockState.isLoading,
                        errorMessage = unlockState.errorMessage,
                        onRegister = { password -> unlockViewModel.setupVault(password) },
                        onGoLogin = {
                            navController.navigateToLoginFromRegister(
                                loginRoute = Route.Login,
                                registerRoute = Route.Register
                            )
                        }
                    )
                }

                composable(
                    Route.Login,
                    enterTransition = { NavTransitions.enterTab },
                    exitTransition = { NavTransitions.exitTab },
                ) {
                    LoginScreen(
                        biometricAvailable = unlockState.biometricAvailable && settingsState.biometricEnabled,
                        isLoading = unlockState.isLoading,
                        errorMessage = unlockState.errorMessage,
                        onLogin = { password -> unlockViewModel.unlockWithPassword(password) },
                        onBiometricLogin = { unlockViewModel.unlockWithBiometric() },
                        onGoRegister = {
                            navController.navigate(Route.Register) { launchSingleTop = true }
                        }
                    )
                }

                composable(
                    Route.Vault,
                    enterTransition = { NavTransitions.enterTab },
                    exitTransition = { NavTransitions.exitTab },
                ) {
                    LaunchedEffect(Unit) { vaultViewModel.loadEntries() }
                    VaultScreen(
                        entries = vaultState.entries,
                        categories = vaultState.categories,
                        selectedCategory = vaultState.selectedCategory,
                        favoritesOnly = vaultState.favoritesOnly,
                        query = vaultState.query,
                        vaultVisitNonce = vaultVisitNonce,
                        isLoading = vaultState.isLoading,
                        onQueryChange = { vaultViewModel.updateQuery(it) },
                        onCategoryChange = { vaultViewModel.updateCategory(it) },
                        onFavoritesOnlyChange = { vaultViewModel.updateFavoritesOnly(it) },
                        onEntryClick = { entry ->
                            selectedEntryId.value = entry.id
                            navController.navigate(Route.Detail)
                        },
                        onAddClick = {
                            selectedEntryId.value = null
                            navController.navigate(Route.AddEdit)
                        }
                    )
                }

                composable(
                    Route.Generator,
                    enterTransition = { NavTransitions.enterTab },
                    exitTransition = { NavTransitions.exitTab },
                ) {
                    GeneratorScreen(
                        uiState = generatorState,
                        onGeneratePreset = { preset -> generatorViewModel.generateWithPreset(preset) },
                        onGenerateCustom = { config -> generatorViewModel.generateWithConfig(config) },
                        onCopyGenerated = { generatorViewModel.copyGeneratedPassword() }
                    )
                }

                composable(
                    Route.Settings,
                    enterTransition = { NavTransitions.enterTab },
                    exitTransition = { NavTransitions.exitTab },
                ) {
                    SettingsScreen(
                        currentTheme = settingsState.themeMode,
                        biometricEnabled = settingsState.biometricEnabled,
                        screenshotAllowed = settingsState.screenshotAllowed,
                        errorMessage = settingsState.errorMessage,
                        onThemeChange = { settingsViewModel.updateTheme(it) },
                        onBiometricChange = { settingsViewModel.updateBiometricEnabled(it) },
                        onScreenshotAllowedChange = { settingsViewModel.updateScreenshotAllowed(it) },
                        onLock = {
                            settingsViewModel.lockNow()
                            navController.navigateToLoginAfterLock(
                                loginRoute = Route.Login,
                                vaultRoute = Route.Vault
                            )
                        }
                    )
                }

                composable(Route.Detail) {
                    val id = selectedEntryId.value ?: return@composable
                    LaunchedEffect(id) { detailViewModel.load(id) }

                    val detailEntry = detailState.entry
                    if (detailEntry != null) {
                        PasswordDetailScreen(
                            entry = detailEntry,
                            onBack = { navController.popBackStack() },
                            onEdit = {
                                selectedEntryId.value = detailEntry.id
                                navController.navigate(Route.AddEdit)
                            },
                            onDelete = { detailViewModel.delete() },
                            onCopyUsername = { detailViewModel.copyUsername() },
                            onCopyPassword = {
                                detailViewModel.copyPassword()
                            }
                        )
                    }
                }

                composable(Route.AddEdit) {
                    LaunchedEffect(selectedEntryId.value) { addEditViewModel.loadEntry(selectedEntryId.value) }
                    AddEditPasswordScreen(
                        entry = addEditState.entry,
                        onSave = { updated -> addEditViewModel.save(updated) },
                        onCancel = { navController.popBackStack() },
                        onGeneratePassword = { generatorViewModel.generateWithPreset(PasswordPreset.Strong) }
                    )
                }
            }
        }
    }
}
