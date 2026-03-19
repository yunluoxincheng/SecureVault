package com.securevault.viewmodel

import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordFilter
import com.securevault.data.PasswordRepository
import com.securevault.security.KeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultUiState(
    val entries: List<PasswordEntry> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val favoritesOnly: Boolean = false,
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class VaultViewModel(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var queryDebounceJob: Job? = null

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
                val currentState = _uiState.value
                val allEntries = passwordRepository.search("", PasswordFilter(), dataKey)
                val categoryOptions = allEntries.map { it.category }.distinct().sorted()
                val selectedCategory = currentState.selectedCategory
                    ?.takeIf { categoryOptions.contains(it) }

                val filteredEntries = passwordRepository.search(
                    currentState.query,
                    PasswordFilter(
                        category = selectedCategory,
                        onlyFavorites = currentState.favoritesOnly
                    ),
                    dataKey
                )

                Triple(filteredEntries, categoryOptions, selectedCategory)
            }.onSuccess { (entries, categories, selectedCategory) ->
                _uiState.update {
                    it.copy(
                        entries = entries,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        isLoading = false
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "加载失败") }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        queryDebounceJob?.cancel()
        queryDebounceJob = scope.launch {
            delay(250)
            loadEntries()
        }
    }

    fun updateCategory(category: String?) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update { it.copy(selectedCategory = category) }
        loadEntries()
    }

    fun updateFavoritesOnly(enabled: Boolean) {
        if (_uiState.value.favoritesOnly == enabled) return
        _uiState.update { it.copy(favoritesOnly = enabled) }
        loadEntries()
    }

    fun deleteEntry(id: Long) {
        val dataKey = keyManager.getDataKey() ?: return
        scope.launch {
            runCatching {
                passwordRepository.deleteById(id)
                val currentState = _uiState.value
                val allEntries = passwordRepository.search("", PasswordFilter(), dataKey)
                val categoryOptions = allEntries.map { it.category }.distinct().sorted()
                val selectedCategory = currentState.selectedCategory
                    ?.takeIf { categoryOptions.contains(it) }
                val filteredEntries = passwordRepository.search(
                    currentState.query,
                    PasswordFilter(
                        category = selectedCategory,
                        onlyFavorites = currentState.favoritesOnly
                    ),
                    dataKey
                )
                Triple(filteredEntries, categoryOptions, selectedCategory)
            }.onSuccess { (entries, categories, selectedCategory) ->
                _uiState.update {
                    it.copy(
                        entries = entries,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message ?: "删除失败") }
            }
        }
    }
}
