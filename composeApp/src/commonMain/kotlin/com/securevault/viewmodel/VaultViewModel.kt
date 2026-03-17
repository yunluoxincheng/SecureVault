package com.securevault.viewmodel

import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordFilter
import com.securevault.data.PasswordRepository
import com.securevault.security.KeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultUiState(
    val entries: List<PasswordEntry> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class VaultViewModel(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    fun loadEntries() {
        val dataKey = keyManager.getDataKey()
        if (dataKey == null) {
            _uiState.update { it.copy(entries = emptyList(), errorMessage = "保险库已锁定") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                passwordRepository.search(_uiState.value.query, PasswordFilter(), dataKey)
            }.onSuccess { entries ->
                _uiState.update { it.copy(entries = entries, isLoading = false) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "加载失败") }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        loadEntries()
    }

    fun deleteEntry(id: Long) {
        val dataKey = keyManager.getDataKey() ?: return
        scope.launch {
            runCatching {
                passwordRepository.deleteById(id)
                passwordRepository.search(_uiState.value.query, PasswordFilter(), dataKey)
            }.onSuccess { entries ->
                _uiState.update { it.copy(entries = entries, errorMessage = null) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message ?: "删除失败") }
            }
        }
    }
}
