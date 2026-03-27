package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
import com.securevault.security.AutofillSystemBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AutofillSettingsUiState(
    val servicePreferenceEnabled: Boolean = true,
    val askToSaveOnLogin: Boolean = true,
    val serviceSupported: Boolean = false,
    val serviceEnabledInSystem: Boolean = false,
    val infoMessage: String? = null,
)

class AutofillSettingsViewModel(
    private val configRepository: ConfigRepository,
    private val autofillSystemBridge: AutofillSystemBridge,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(AutofillSettingsUiState())
    val uiState: StateFlow<AutofillSettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        scope.launch {
            val preferenceEnabled = configRepository.get(VaultConfigKeys.AutofillEnabled)?.toBooleanStrictOrNull() ?: true
            val askSave = configRepository.get(VaultConfigKeys.AutofillAskToSaveOnLogin)?.toBooleanStrictOrNull() ?: true
            _uiState.update {
                it.copy(
                    servicePreferenceEnabled = preferenceEnabled,
                    askToSaveOnLogin = askSave,
                    serviceSupported = autofillSystemBridge.isSupported(),
                    serviceEnabledInSystem = autofillSystemBridge.isServiceEnabled(),
                    infoMessage = null,
                )
            }
        }
    }

    fun updateAutofillEnabled(enabled: Boolean) {
        _uiState.update { it.copy(servicePreferenceEnabled = enabled) }
        scope.launch {
            configRepository.set(VaultConfigKeys.AutofillEnabled, enabled.toString())
            if (enabled) {
                val opened = autofillSystemBridge.openSystemAutofillSettings()
                _uiState.update {
                    it.copy(
                        serviceEnabledInSystem = autofillSystemBridge.isServiceEnabled(),
                        infoMessage = if (opened) "请在系统页面将 SecureVault 设为自动填充服务" else "无法打开系统自动填充设置",
                    )
                }
            }
        }
    }

    fun updateAskToSaveOnLogin(enabled: Boolean) {
        _uiState.update { it.copy(askToSaveOnLogin = enabled) }
        scope.launch {
            configRepository.set(VaultConfigKeys.AutofillAskToSaveOnLogin, enabled.toString())
        }
    }

    fun openSystemSettings() {
        val opened = autofillSystemBridge.openSystemAutofillSettings()
        _uiState.update {
            it.copy(
                serviceEnabledInSystem = autofillSystemBridge.isServiceEnabled(),
                infoMessage = if (opened) "已跳转到系统自动填充设置" else "无法打开系统自动填充设置",
            )
        }
    }

    fun refreshSystemStatus() {
        _uiState.update {
            it.copy(
                serviceSupported = autofillSystemBridge.isSupported(),
                serviceEnabledInSystem = autofillSystemBridge.isServiceEnabled(),
            )
        }
    }

    fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
