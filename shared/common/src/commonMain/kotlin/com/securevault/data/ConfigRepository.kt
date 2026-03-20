package com.securevault.data

interface ConfigRepository {
    suspend fun get(key: String): String?

    suspend fun set(key: String, value: String)

    suspend fun delete(key: String)
}

object VaultConfigKeys {
    const val Salt = "salt"
    const val EncryptedDataKeyPassword = "encrypted_data_key_password"
    const val Argon2MemoryKb = "argon2_memory_kb"
    const val Argon2Iterations = "argon2_iterations"
    const val Argon2Parallelism = "argon2_parallelism"
    const val Argon2OutputLength = "argon2_output_length"
    const val BiometricEnabled = "biometric_enabled"
    const val ScreenshotAllowed = "screenshot_allowed"
    const val SecurityModeEnabled = "security_mode_enabled"
    const val EncryptedSecureModeKey = "encrypted_secure_mode_key"
    const val OnboardingCompleted = "onboarding_completed"
    const val VaultSetupCompleted = "vault_setup_completed"
    const val SessionLockTimeoutMs = "session_lock_timeout_ms"
}
