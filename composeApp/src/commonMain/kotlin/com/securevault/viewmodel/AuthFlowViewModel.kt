package com.securevault.viewmodel

import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStartDestination {
    Onboarding,
    Register,
    Login
}

data class AuthFlowUiState(
    val isLoading: Boolean = true,
    val startDestination: AuthStartDestination = AuthStartDestination.Onboarding
)

class AuthFlowViewModel(
    private val configRepository: ConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(AuthFlowUiState())
    val uiState: StateFlow<AuthFlowUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            val onboardingCompleted = configRepository.get(VaultConfigKeys.OnboardingCompleted)?.toBooleanStrictOrNull() ?: false
            val vaultSetupCompleted = configRepository.get(VaultConfigKeys.VaultSetupCompleted)?.toBooleanStrictOrNull() ?: false

            val destination = when {
                !onboardingCompleted -> AuthStartDestination.Onboarding
                !vaultSetupCompleted -> AuthStartDestination.Register
                else -> AuthStartDestination.Login
            }

            _uiState.update { it.copy(isLoading = false, startDestination = destination) }
        }
    }

    fun completeOnboarding() {
        scope.launch {
            configRepository.set(VaultConfigKeys.OnboardingCompleted, true.toString())
            refresh()
        }
    }

    fun markVaultSetupCompleted() {
        scope.launch {
            configRepository.set(VaultConfigKeys.VaultSetupCompleted, true.toString())
            refresh()
        }
    }
}
