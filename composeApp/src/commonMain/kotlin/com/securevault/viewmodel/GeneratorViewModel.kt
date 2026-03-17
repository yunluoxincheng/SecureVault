package com.securevault.viewmodel

import com.securevault.util.PasswordGenerator
import com.securevault.util.PasswordGeneratorConfig
import com.securevault.util.PasswordPreset
import com.securevault.util.PasswordStrengthCalculator
import com.securevault.util.PasswordStrengthLevel
import com.securevault.security.SecureClipboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GeneratorUiState(
    val generatedPassword: String = "",
    val config: PasswordGeneratorConfig = PasswordPreset.Strong.config,
    val history: List<String> = emptyList(),
    val strength: PasswordStrengthLevel = PasswordStrengthLevel.VeryWeak,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class GeneratorViewModel(
    private val passwordGenerator: PasswordGenerator,
    private val secureClipboard: SecureClipboard
) {
    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    fun generateWithPreset(preset: PasswordPreset): String {
        return generateWithConfig(preset.config)
    }

    fun generateWithConfig(config: PasswordGeneratorConfig): String {
        return runCatching {
            passwordGenerator.generateCustom(config)
        }.onSuccess { password ->
            _uiState.update {
                it.copy(
                    generatedPassword = password,
                    config = config,
                    history = passwordGenerator.getHistory(),
                    strength = PasswordStrengthCalculator.calculate(password),
                    errorMessage = null,
                    infoMessage = null
                )
            }
        }.onFailure { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "生成失败", infoMessage = null) }
        }.getOrDefault("")
    }

    fun copyGeneratedPassword() {
        val password = _uiState.value.generatedPassword
        if (password.isBlank()) {
            _uiState.update { it.copy(infoMessage = null, errorMessage = "暂无可复制的密码") }
            return
        }

        secureClipboard.copy(password, "Generated Password")
        secureClipboard.scheduleAutoClear()
        _uiState.update { it.copy(infoMessage = "已复制，30 秒后自动清除", errorMessage = null) }
    }
}
