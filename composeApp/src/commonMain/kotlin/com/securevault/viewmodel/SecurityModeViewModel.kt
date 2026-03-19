package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
import com.securevault.security.BiometricAuth
import com.securevault.security.BiometricResult
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerError
import com.securevault.security.KeyManagerResult
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
    val showPasswordVerificationDialog: Boolean = false,
    val message: String? = null,
)

class SecurityModeViewModel(
    private val securityModeManager: SecurityModeManager,
    private val configRepository: ConfigRepository,
    private val biometricAuth: BiometricAuth,
    private val keyManager: KeyManager,
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
            val currentEnabled = _uiState.value.enabled
            if (!currentEnabled || enabled) {
                applyEnabled(enabled)
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, message = null) }
            val biometricEnabled = configRepository
                .get(VaultConfigKeys.BiometricEnabled)
                ?.toBooleanStrictOrNull()
                ?: false

            if (biometricEnabled && biometricAuth.isAvailable()) {
                when (val authResult = biometricAuth.authenticate("关闭安全模式", "请验证身份以关闭全局安全模式")) {
                    BiometricResult.Success -> applyEnabled(false)
                    BiometricResult.Cancelled -> {
                        _uiState.update { it.copy(isLoading = false, message = "已取消生物识别验证") }
                    }

                    BiometricResult.NotAvailable -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showPasswordVerificationDialog = true,
                                message = "生物识别不可用，请输入主密码验证",
                            )
                        }
                    }

                    BiometricResult.Failed -> {
                        _uiState.update { it.copy(isLoading = false, message = "生物识别验证失败") }
                    }

                    is BiometricResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, message = authResult.message) }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showPasswordVerificationDialog = true,
                        message = null,
                    )
                }
            }
        }
    }

    fun confirmDisableWithPassword(password: String) {
        if (password.isBlank()) {
            _uiState.update { it.copy(message = "请输入主密码") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            when (val result = keyManager.unlockWithPassword(password.toCharArray())) {
                is KeyManagerResult.Success -> {
                    _uiState.update { it.copy(showPasswordVerificationDialog = false) }
                    applyEnabled(false)
                }

                is KeyManagerResult.Error -> {
                    val message = when (result.error) {
                        KeyManagerError.InvalidPassword -> "主密码错误"
                        KeyManagerError.VaultNotSetup -> "未检测到保险库，请先注册"
                        else -> "验证失败，请重试"
                    }
                    _uiState.update { it.copy(isLoading = false, message = message) }
                }
            }
        }
    }

    fun dismissPasswordVerificationDialog() {
        _uiState.update { it.copy(showPasswordVerificationDialog = false) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun applyEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isLoading = true, message = null) }
        runCatching {
            securityModeManager.setEnabled(enabled)
        }.onSuccess {
            _uiState.update {
                it.copy(
                    enabled = enabled,
                    isLoading = false,
                    showPasswordVerificationDialog = false,
                    message = if (enabled) "安全模式已开启" else "安全模式已关闭",
                )
            }
        }.onFailure { throwable ->
            _uiState.update { it.copy(isLoading = false, message = throwable.message ?: "更新安全模式失败") }
        }
    }
}
