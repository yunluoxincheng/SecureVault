package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
import com.securevault.crypto.CryptoConstants
import com.securevault.security.BiometricAuth
import com.securevault.security.BiometricResult
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
    val screenshotAllowed: Boolean = false,
    val sessionTimeoutMs: Long = CryptoConstants.Session.DEFAULT_LOCK_TIMEOUT_MS,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val configRepository: ConfigRepository,
    private val keyManager: KeyManager,
    private val screenSecurity: ScreenSecurity,
    private val biometricAuth: BiometricAuth
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
                val screenshotAllowed = configRepository.get(VaultConfigKeys.ScreenshotAllowed)?.toBooleanStrictOrNull()
                val sessionTimeoutMs = configRepository.get(VaultConfigKeys.SessionLockTimeoutMs)?.toLongOrNull()
                Quadruple(theme, biometric, screenshotAllowed, sessionTimeoutMs)
            }.onSuccess { (theme, biometric, screenshotAllowed, sessionTimeoutMs) ->
                val allowScreenshot = screenshotAllowed ?: false
                val timeoutMs = normalizeSessionTimeout(sessionTimeoutMs)

                keyManager.setSessionLockTimeout(timeoutMs)

                if (allowScreenshot) {
                    screenSecurity.disableScreenshotProtection()
                } else {
                    screenSecurity.enableScreenshotProtection()
                }

                _uiState.update {
                    it.copy(
                        themeMode = theme ?: ThemeMode.System,
                        biometricEnabled = biometric ?: false,
                        screenshotAllowed = allowScreenshot,
                        sessionTimeoutMs = timeoutMs,
                        infoMessage = null,
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
        scope.launch {
            if (!enabled) {
                _uiState.update { it.copy(biometricEnabled = false, errorMessage = null) }
                configRepository.set(VaultConfigKeys.BiometricEnabled, false.toString())
                return@launch
            }

            if (!biometricAuth.isAvailable() || !keyManager.canUnlockWithBiometric()) {
                _uiState.update {
                    it.copy(
                        biometricEnabled = false,
                        errorMessage = "当前设备不可用或尚未完成密码解锁，无法开启生物识别"
                    )
                }
                configRepository.set(VaultConfigKeys.BiometricEnabled, false.toString())
                return@launch
            }

            when (biometricAuth.authenticate("启用生物识别", "请验证身份以开启生物识别解锁")) {
                BiometricResult.Success -> {
                    _uiState.update { it.copy(biometricEnabled = true, errorMessage = null) }
                    configRepository.set(VaultConfigKeys.BiometricEnabled, true.toString())
                }

                BiometricResult.Cancelled -> {
                    _uiState.update { it.copy(biometricEnabled = false, errorMessage = "已取消生物识别验证") }
                    configRepository.set(VaultConfigKeys.BiometricEnabled, false.toString())
                }

                BiometricResult.NotAvailable -> {
                    _uiState.update { it.copy(biometricEnabled = false, errorMessage = "生物识别不可用") }
                    configRepository.set(VaultConfigKeys.BiometricEnabled, false.toString())
                }

                BiometricResult.Failed -> {
                    _uiState.update { it.copy(biometricEnabled = false, errorMessage = "生物识别验证失败") }
                    configRepository.set(VaultConfigKeys.BiometricEnabled, false.toString())
                }

                is BiometricResult.Error -> {
                    _uiState.update { it.copy(biometricEnabled = false, errorMessage = "生物识别错误") }
                    configRepository.set(VaultConfigKeys.BiometricEnabled, false.toString())
                }
            }
        }
    }

    fun updateScreenshotAllowed(allowed: Boolean) {
        _uiState.update { it.copy(screenshotAllowed = allowed) }
        if (allowed) {
            screenSecurity.disableScreenshotProtection()
        } else {
            screenSecurity.enableScreenshotProtection()
        }
        scope.launch {
            configRepository.set(VaultConfigKeys.ScreenshotAllowed, allowed.toString())
        }
    }

    fun updateSessionTimeout(timeoutMs: Long) {
        val normalizedTimeout = normalizeSessionTimeout(timeoutMs)
        val currentTimeout = _uiState.value.sessionTimeoutMs
        if (currentTimeout == normalizedTimeout) return

        _uiState.update {
            it.copy(
                sessionTimeoutMs = normalizedTimeout,
                infoMessage = "会话超时已生效：${sessionTimeoutLabel(normalizedTimeout)}"
            )
        }
        keyManager.setSessionLockTimeout(normalizedTimeout)
        scope.launch {
            configRepository.set(VaultConfigKeys.SessionLockTimeoutMs, normalizedTimeout.toString())
        }
    }

    fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun lockNow() {
        keyManager.lock()
    }

    companion object {
        const val THEME_KEY = "theme_mode"
        const val DEFAULT_SESSION_TIMEOUT_MS = CryptoConstants.Session.DEFAULT_LOCK_TIMEOUT_MS
        const val IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS = CryptoConstants.Session.IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS
        private const val LEGACY_IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS = 1_000L
        val SUPPORTED_SESSION_TIMEOUTS_MS = setOf(0L, IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS, 60_000L, 300_000L, 900_000L, 1_800_000L)

        fun normalizeSessionTimeout(timeoutMs: Long?): Long {
            val value = timeoutMs ?: DEFAULT_SESSION_TIMEOUT_MS
            if (value == LEGACY_IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS) {
                return IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS
            }
            return if (value in SUPPORTED_SESSION_TIMEOUTS_MS) value else DEFAULT_SESSION_TIMEOUT_MS
        }

        fun sessionTimeoutLabel(timeoutMs: Long): String {
            return when (timeoutMs) {
                IMMEDIATE_BACKGROUND_LOCK_TIMEOUT_MS -> "后台后立即锁定"
                60_000L -> "1 分钟"
                300_000L -> "5 分钟"
                900_000L -> "15 分钟"
                1_800_000L -> "30 分钟"
                0L -> "永不自动锁定"
                else -> "5 分钟"
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
