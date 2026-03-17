package com.securevault.viewmodel

import com.securevault.security.BiometricAuth
import com.securevault.security.BiometricResult
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
    private val biometricAuth: BiometricAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _uiState.update {
            it.copy(
                isVaultSetup = keyManager.isVaultSetup(),
                biometricAvailable = biometricAuth.isAvailable()
            )
        }
    }

    fun setupVault(password: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (keyManager.setupVault(password.toCharArray())) {
                is KeyManagerResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUnlocked = true,
                        isVaultSetup = true
                    )
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
            when (keyManager.unlockWithPassword(password.toCharArray())) {
                is KeyManagerResult.Success -> _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
                is KeyManagerResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = "主密码错误") }
            }
        }
    }

    fun unlockWithBiometric() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = biometricAuth.authenticate("SecureVault", "验证身份以解锁")
            when (result) {
                BiometricResult.Success -> _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
                BiometricResult.NotAvailable -> _uiState.update { it.copy(isLoading = false, errorMessage = "生物识别不可用") }
                BiometricResult.Cancelled -> _uiState.update { it.copy(isLoading = false, errorMessage = "已取消生物识别") }
                BiometricResult.Failed -> _uiState.update { it.copy(isLoading = false, errorMessage = "生物识别失败") }
                is BiometricResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun consumeUnlockEvent() {
        _uiState.update { it.copy(isUnlocked = false) }
    }
}
