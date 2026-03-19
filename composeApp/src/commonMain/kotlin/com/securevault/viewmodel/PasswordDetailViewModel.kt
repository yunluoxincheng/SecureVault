package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordRepository
import com.securevault.security.BiometricAuth
import com.securevault.security.BiometricResult
import com.securevault.security.KeyManagerError
import com.securevault.security.KeyManagerResult
import com.securevault.security.KeyManager
import com.securevault.security.SecurityModeManager
import com.securevault.security.SecureClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SensitiveAction {
    Edit,
    Delete,
}

data class PasswordDetailUiState(
    val entry: PasswordEntry? = null,
    val isLoading: Boolean = false,
    val deleted: Boolean = false,
    val pendingVerificationAction: SensitiveAction? = null,
    val verifiedAction: SensitiveAction? = null,
    val message: String? = null
)

class PasswordDetailViewModel(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager,
    private val secureClipboard: SecureClipboard,
    private val securityModeManager: SecurityModeManager,
    private val configRepository: ConfigRepository,
    private val biometricAuth: BiometricAuth,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(PasswordDetailUiState())
    val uiState: StateFlow<PasswordDetailUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        val dataKey = keyManager.getDataKey()
        if (dataKey == null) {
            _uiState.update { it.copy(message = "保险库已锁定") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, deleted = false) }
            runCatching {
                passwordRepository.getById(id, dataKey)
            }.onSuccess { entry ->
                _uiState.update { it.copy(entry = entry, isLoading = false) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, message = throwable.message ?: "加载失败") }
            }
        }
    }

    fun delete() {
        val entryId = _uiState.value.entry?.id ?: return
        scope.launch {
            runCatching {
                passwordRepository.deleteById(entryId)
            }.onSuccess {
                _uiState.update { it.copy(deleted = true, message = "已删除") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(message = throwable.message ?: "删除失败") }
            }
        }
    }

    fun requestSensitiveActionVerification(action: SensitiveAction) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val biometricEnabled = configRepository
                .get(VaultConfigKeys.BiometricEnabled)
                ?.toBooleanStrictOrNull()
                ?: false

            if (biometricEnabled && biometricAuth.isAvailable()) {
                when (val result = biometricAuth.authenticate("安全验证", "请验证身份以继续操作")) {
                    BiometricResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingVerificationAction = null,
                                verifiedAction = action,
                                message = null,
                            )
                        }
                    }

                    BiometricResult.Cancelled -> {
                        _uiState.update { it.copy(isLoading = false, message = "已取消生物识别验证") }
                    }

                    BiometricResult.NotAvailable -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingVerificationAction = action,
                                message = "生物识别不可用，请输入主密码验证",
                            )
                        }
                    }

                    BiometricResult.Failed -> {
                        _uiState.update { it.copy(isLoading = false, message = "生物识别验证失败") }
                    }

                    is BiometricResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, message = result.message) }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingVerificationAction = action,
                        message = null,
                    )
                }
            }
        }
    }

    fun verifySensitiveActionWithPassword(password: String) {
        if (password.isBlank()) {
            _uiState.update { it.copy(message = "请输入主密码") }
            return
        }

        val action = _uiState.value.pendingVerificationAction ?: return
        scope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            when (val result = keyManager.unlockWithPassword(password.toCharArray())) {
                is KeyManagerResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingVerificationAction = null,
                            verifiedAction = action,
                            message = null,
                        )
                    }
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

    fun dismissSensitiveActionVerification() {
        _uiState.update { it.copy(pendingVerificationAction = null) }
    }

    fun consumeVerifiedAction() {
        _uiState.update { it.copy(verifiedAction = null) }
    }

    fun copyUsername() {
        val username = _uiState.value.entry?.username ?: return
        secureClipboard.copy(username, "Username")
        secureClipboard.scheduleAutoClear()
        _uiState.update { it.copy(message = "用户名已复制，将在 30 秒后清除") }
    }

    fun copyPassword() {
        val entry = _uiState.value.entry ?: return
        val dataKey = keyManager.getDataKey()
        if (dataKey == null) {
            _uiState.update { it.copy(message = "保险库已锁定") }
            return
        }

        scope.launch {
            runCatching {
                if (entry.securityMode) {
                    val entryId = entry.id ?: throw IllegalStateException("密码条目不存在")
                    val payload = passwordRepository.getPasswordCipherById(entryId)
                        ?: throw IllegalStateException("密码条目不存在")
                    securityModeManager.usePassword(
                        encryptedPassword = payload.encryptedPassword,
                        dataKey = dataKey,
                        securityMode = payload.securityMode,
                    )
                } else {
                    secureClipboard.copy(entry.password, "Password")
                    secureClipboard.scheduleAutoClear()
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        message = if (entry.securityMode) {
                            "密码已使用（已复制），将在 30 秒后清除"
                        } else {
                            "密码已复制，将在 30 秒后清除"
                        }
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(message = throwable.message ?: "密码使用失败") }
            }
        }
    }
}
