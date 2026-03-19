package com.securevault.viewmodel

import com.securevault.security.SecurityModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityModeUiState(
    val enabled: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
)

class SecurityModeViewModel(
    private val securityModeManager: SecurityModeManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(SecurityModeUiState())
    val uiState: StateFlow<SecurityModeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                securityModeManager.isEnabled()
            }.onSuccess { enabled ->
                _uiState.update { it.copy(enabled = enabled, isLoading = false, message = null) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, message = throwable.message ?: "加载安全模式失败") }
            }
        }
    }

    fun updateEnabled(enabled: Boolean) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                securityModeManager.setEnabled(enabled)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        enabled = enabled,
                        isLoading = false,
                        message = if (enabled) "安全模式已开启" else "安全模式已关闭",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, message = throwable.message ?: "更新安全模式失败") }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
