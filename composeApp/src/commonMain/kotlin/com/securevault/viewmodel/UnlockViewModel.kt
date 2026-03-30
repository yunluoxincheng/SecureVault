package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.UserDataTransferManager
import com.securevault.data.VaultConfigKeys
import com.securevault.data.VaultFileGateway
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
import kotlinx.coroutines.withTimeoutOrNull

data class UnlockUiState(
    val isVaultSetup: Boolean = false,
    val isLoading: Boolean = false,
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null,
    val biometricAvailable: Boolean = false,
    val showImportUserDataPasswordDialog: Boolean = false,
)

class UnlockViewModel(
    private val keyManager: KeyManager,
    private val biometricAuth: BiometricAuth,
    private val configRepository: ConfigRepository,
    private val vaultFileGateway: VaultFileGateway,
    private val userDataTransferManager: UserDataTransferManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingUserDataContent: String? = null

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

    fun setupVault(password: CharArray) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (keyManager.setupVault(password)) {
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

    fun unlockWithPassword(password: CharArray) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = keyManager.unlockWithPassword(password)) {
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

            val authResult = withTimeoutOrNull(BIOMETRIC_AUTH_TIMEOUT_MS) {
                biometricAuth.authenticate("SecureVault", "验证身份以解锁")
            } ?: BiometricResult.Cancelled

            when (authResult) {
                BiometricResult.Success -> {
                    when (val unlockResult = keyManager.unlockWithBiometric()) {
                        is KeyManagerResult.Success -> {
                            _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
                        }

                        is KeyManagerResult.Error -> {
                            val message = when (unlockResult.error) {
                                KeyManagerError.VaultNotSetup -> "未检测到保险库，请先注册"
                                KeyManagerError.BiometricNotEnrolled -> "尚未准备生物识别解锁，请先用主密码登录一次"
                                KeyManagerError.DeviceKeyDecryptFailed ->
                                    "设备密钥无法解密，请使用主密码解锁"
                                KeyManagerError.DeviceKeyKeystoreFailure ->
                                    "设备密钥不可用，请使用主密码解锁"
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

    fun resetToLoginState() {
        pendingUserDataContent = null
        _uiState.update {
            it.copy(
                isLoading = false,
                isUnlocked = false,
                errorMessage = null,
                showImportUserDataPasswordDialog = false,
            )
        }
    }

    fun onLoginRouteEntered() {
        resetToLoginState()
        refreshState()
    }

    fun startImportUserData() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = runCatching {
                val source = vaultFileGateway.pickImportSource().getOrThrow()
                vaultFileGateway.readText(source).getOrThrow()
            }

            _uiState.update {
                if (result.isSuccess) {
                    pendingUserDataContent = result.getOrNull()
                    it.copy(
                        isLoading = false,
                        showImportUserDataPasswordDialog = true,
                        errorMessage = null,
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        showImportUserDataPasswordDialog = false,
                        errorMessage = ImportExportErrorMapper.userDataSelect(
                            result.exceptionOrNull() ?: IllegalStateException("选择失败")
                        ),
                    )
                }
            }
        }
    }

    fun confirmImportUserData(masterPassword: CharArray) {
        if (masterPassword.isEmpty() || masterPassword.all { it.isWhitespace() }) {
            _uiState.update { it.copy(errorMessage = "请输入主密码") }
            return
        }

        val content = pendingUserDataContent
        if (content == null) {
            _uiState.update {
                it.copy(
                    showImportUserDataPasswordDialog = false,
                    errorMessage = "未找到待导入的用户数据，请重新选择文件",
                )
            }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = runCatching {
                userDataTransferManager.import(content, masterPassword).getOrThrow()
                keyManager.clearVaultConfigCache()
                when (keyManager.unlockWithPassword(masterPassword)) {
                    is KeyManagerResult.Success -> Unit
                    is KeyManagerResult.Error -> error("用户数据已导入，但主密码验证失败")
                }
                configRepository.set(VaultConfigKeys.VaultSetupCompleted, true.toString())
            }

            _uiState.update {
                if (result.isSuccess) {
                    pendingUserDataContent = null
                    it.copy(
                        isLoading = false,
                        isUnlocked = true,
                        isVaultSetup = true,
                        showImportUserDataPasswordDialog = false,
                        errorMessage = null,
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        showImportUserDataPasswordDialog = false,
                        errorMessage = ImportExportErrorMapper.userDataImport(
                            result.exceptionOrNull() ?: IllegalStateException("导入失败")
                        ),
                    )
                }
            }
        }
    }

    fun dismissImportUserDataDialog() {
        pendingUserDataContent = null
        _uiState.update { it.copy(showImportUserDataPasswordDialog = false) }
    }

    companion object {
        private const val BIOMETRIC_AUTH_TIMEOUT_MS = 20_000L
    }
}
