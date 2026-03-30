package com.securevault.viewmodel

import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordFilter
import com.securevault.data.PasswordRepository
import com.securevault.security.KeyManager
import kotlinx.coroutines.CancellationException
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
    val hasLoadedAtLeastOnce: Boolean = false,
    val errorMessage: String? = null
)

class VaultViewModel(
    private val passwordRepository: PasswordRepository,
    private val keyManager: KeyManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var queryDebounceJob: Job? = null
    private var loadEntriesJob: Job? = null
    private var loadRequestId: Long = 0L

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    fun loadEntries() {
        val targetState = _uiState.value
        val dataKey = keyManager.getDataKey()
        if (dataKey == null) {
            _uiState.update { it.copy(entries = emptyList(), errorMessage = "保险库已锁定") }
            return
        }

        loadEntriesJob?.cancel()
        val requestId = ++loadRequestId
        loadEntriesJob = scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val allEntries = passwordRepository.search("", PasswordFilter(), dataKey)
                val categoryOptions = allEntries.map { it.category }.distinct().sorted()
                val selectedCategory = targetState.selectedCategory
                    ?.takeIf { categoryOptions.contains(it) }

                val filteredEntries = passwordRepository.search(
                    targetState.query,
                    PasswordFilter(
                        category = selectedCategory,
                        onlyFavorites = targetState.favoritesOnly
                    ),
                    dataKey
                )

                if (requestId != loadRequestId) return@launch
                _uiState.update {
                    it.copy(
                        entries = filteredEntries,
                        categories = categoryOptions,
                        selectedCategory = selectedCategory,
                        favoritesOnly = targetState.favoritesOnly,
                        query = targetState.query,
                        hasLoadedAtLeastOnce = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId != loadRequestId) return@launch
                _uiState.update { it.copy(errorMessage = e.message ?: "加载失败") }
            } finally {
                if (requestId == loadRequestId) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * Called when the vault list route leaves composition (tab switch, push, etc.).
     * Cancels in-flight list loads and debounced queries so [VaultUiState.isLoading] cannot stick.
     */
    fun onLeaveVaultList() {
        queryDebounceJob?.cancel()
        queryDebounceJob = null
        loadEntriesJob?.cancel()
        loadEntriesJob = null
        _uiState.update { state ->
            if (state.isLoading) state.copy(isLoading = false) else state
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
        updateFilters(category = category)
    }

    fun updateFavoritesOnly(enabled: Boolean) {
        updateFilters(favoritesOnly = enabled)
    }

    fun updateFilters(
        category: String? = _uiState.value.selectedCategory,
        favoritesOnly: Boolean = _uiState.value.favoritesOnly,
    ) {
        val currentState = _uiState.value
        if (currentState.selectedCategory == category && currentState.favoritesOnly == favoritesOnly) return
        _uiState.update {
            it.copy(
                selectedCategory = category,
                favoritesOnly = favoritesOnly,
            )
        }
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
