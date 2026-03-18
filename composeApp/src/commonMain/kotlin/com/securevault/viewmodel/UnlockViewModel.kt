package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
import com.securevault.security.BiometricAuth
import com.securevault.security.BiometricResult
import com.securevault.security.KeyManagerError
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnlockUiState(
    val isVaultSetup: Boolean = false,
    val isLoading: Boolean = false,
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null,
    val biometricAvailable: Boolean = false
)

class UnlockViewModel(
    private val keyManager: KeyManager,
    private val biometricAuth: BiometricAuth,
    private val configRepository: ConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        scope.launch {
            val biometricEnabled = configRepository
                .get(VaultConfigKeys.BiometricEnabled)
                ?.toBooleanStrictOrNull()
                ?: true

            _uiState.update {
                it.copy(
                    isVaultSetup = keyManager.isVaultSetup(),
                        biometricAvailable = biometricEnabled && biometricAuth.isAvailable() && keyManager.canUnlockWithBiometric()
                )
            }
        }
    }

    fun setupVault(password: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (keyManager.setupVault(password.toCharArray())) {
                is KeyManagerResult.Success -> {
                    configRepository.set(VaultConfigKeys.VaultSetupCompleted, true.toString())
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUnlocked = true,
                            isVaultSetup = true
                        )
                    }
                }

                is KeyManagerResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "设置保险库失败"
                    )
                }
            }
        }
    }

    fun unlockWithPassword(password: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = keyManager.unlockWithPassword(password.toCharArray())) {
                is KeyManagerResult.Success -> _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
                is KeyManagerResult.Error -> {
                    when (result.error) {
                        KeyManagerError.InvalidPassword -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = "主密码错误") }
                        }

                        KeyManagerError.VaultNotSetup -> {
                            configRepository.set(VaultConfigKeys.VaultSetupCompleted, false.toString())
                            _uiState.update { it.copy(isLoading = false, errorMessage = "未检测到已注册保险库，请先注册") }
                        }

                        else -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = "解锁失败，请重试") }
                        }
                    }
                }
            }
        }
    }

    fun unlockWithBiometric() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val biometricEnabled = configRepository
                .get(VaultConfigKeys.BiometricEnabled)
                ?.toBooleanStrictOrNull()
                ?: true
            if (!biometricEnabled) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "请先在设置中开启生物识别") }
                return@launch
            }

            when (val authResult = biometricAuth.authenticate("SecureVault", "验证身份以解锁")) {
                BiometricResult.Success -> {
                    when (val unlockResult = keyManager.unlockWithBiometric()) {
                        is KeyManagerResult.Success -> {
                            _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
                        }

                        is KeyManagerResult.Error -> {
                            val message = when (unlockResult.error) {
                                KeyManagerError.VaultNotSetup -> "未检测到保险库，请先注册"
                                KeyManagerError.BiometricNotEnrolled -> "尚未准备生物识别解锁，请先用主密码登录一次"
                                else -> "生物识别解锁失败，请先使用主密码"
                            }
                            _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                        }
                    }
                }

                BiometricResult.NotAvailable -> _uiState.update { it.copy(isLoading = false, errorMessage = "生物识别不可用") }
                BiometricResult.Cancelled -> _uiState.update { it.copy(isLoading = false, errorMessage = "已取消生物识别") }
                BiometricResult.Failed -> _uiState.update { it.copy(isLoading = false, errorMessage = "生物识别失败") }
                is BiometricResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = authResult.message) }
            }
        }
    }

    fun consumeUnlockEvent() {
        _uiState.update { it.copy(isUnlocked = false) }
    }
}
