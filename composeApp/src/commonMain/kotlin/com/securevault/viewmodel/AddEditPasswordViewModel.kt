package com.securevault.viewmodel

import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordRepository
import com.securevault.ui.navigation.AutofillDraft
import com.securevault.security.KeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditPasswordUiState(
    val entry: PasswordEntry? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AddEditPasswordViewModel(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pendingAutofillDraft: AutofillDraft? = null

    private val _uiState = MutableStateFlow(AddEditPasswordUiState())
    val uiState: StateFlow<AddEditPasswordUiState> = _uiState.asStateFlow()

    fun loadEntry(id: Long?) {
        if (id == null) {
            val draft = pendingAutofillDraft
            pendingAutofillDraft = null
            if (draft == null) {
                val keep = _uiState.value.entry
                if (keep != null && keep.id == null) {
                    return
                }
                _uiState.update { AddEditPasswordUiState(entry = null) }
            } else {
                val now = System.currentTimeMillis()
                _uiState.update {
                    AddEditPasswordUiState(
                        entry = PasswordEntry(
                            title = draft.title,
                            username = draft.username,
                            password = draft.password,
                            url = draft.url,
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                }
            }
            return
        }

        _uiState.update { it.copy(entry = null, errorMessage = null, saveSuccess = false) }

        val dataKey = keyManager.getDataKey()
        if (dataKey == null) {
            _uiState.update { it.copy(errorMessage = "保险库已锁定") }
            return
        }

        scope.launch {
            runCatching {
                passwordRepository.getById(id, dataKey)
            }.onSuccess { entry ->
                _uiState.update { it.copy(entry = entry, errorMessage = null, saveSuccess = false) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message ?: "加载失败") }
            }
        }
    }

    fun applyAutofillDraft(draft: AutofillDraft) {
        pendingAutofillDraft = draft
    }

    fun save(entry: PasswordEntry) {
        val dataKey = keyManager.getDataKey()
        if (dataKey == null) {
            _uiState.update { it.copy(errorMessage = "保险库已锁定") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false, errorMessage = null) }
            runCatching {
                if (entry.id == null) {
                    passwordRepository.create(entry, dataKey)
                } else {
                    passwordRepository.update(entry, dataKey)
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message ?: "保存失败") }
            }
        }
    }

    fun consumeSaveResult() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
