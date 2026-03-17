package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
import com.securevault.security.KeyManager
import com.securevault.security.ScreenSecurity
import com.securevault.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val biometricEnabled: Boolean = true,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val configRepository: ConfigRepository,
    private val keyManager: KeyManager,
    private val screenSecurity: ScreenSecurity
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        screenSecurity.enableScreenshotProtection()
        load()
    }

    fun load() {
        scope.launch {
            runCatching {
                val theme = configRepository.get(THEME_KEY)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                val biometric = configRepository.get(VaultConfigKeys.BiometricEnabled)?.toBooleanStrictOrNull()
                theme to biometric
            }.onSuccess { (theme, biometric) ->
                _uiState.update {
                    it.copy(
                        themeMode = theme ?: ThemeMode.System,
                        biometricEnabled = biometric ?: true,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message ?: "设置加载失败") }
            }
        }
    }

    fun updateTheme(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        scope.launch { configRepository.set(THEME_KEY, mode.name) }
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
        scope.launch { configRepository.set(VaultConfigKeys.BiometricEnabled, enabled.toString()) }
    }

    fun lockNow() {
        keyManager.lock()
    }

    private companion object {
        const val THEME_KEY = "theme_mode"
    }
}
