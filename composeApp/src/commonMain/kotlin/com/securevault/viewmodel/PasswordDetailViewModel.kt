package com.securevault.viewmodel

import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordRepository
import com.securevault.security.KeyManager
import com.securevault.security.SecureClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasswordDetailUiState(
    val entry: PasswordEntry? = null,
    val isLoading: Boolean = false,
    val deleted: Boolean = false,
    val message: String? = null
)

class PasswordDetailViewModel(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager,
    private val secureClipboard: SecureClipboard
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

    fun copyUsername() {
        val username = _uiState.value.entry?.username ?: return
        secureClipboard.copy(username, "Username")
        secureClipboard.scheduleAutoClear()
        _uiState.update { it.copy(message = "用户名已复制，将在 30 秒后清除") }
    }

    fun copyPassword() {
        val password = _uiState.value.entry?.password ?: return
        secureClipboard.copy(password, "Password")
        secureClipboard.scheduleAutoClear()
        _uiState.update { it.copy(message = "密码已复制，将在 30 秒后清除") }
    }
}
