package com.securevault.security

import com.securevault.crypto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class KeyManagerResult<out T> {
    data class Success<T>(val data: T) : KeyManagerResult<T>()
    data class Error(val error: KeyManagerError) : KeyManagerResult<Nothing>()
}

sealed class KeyManagerError {
    object InvalidPassword : KeyManagerError()
    object VaultNotSetup : KeyManagerError()
    object VaultAlreadySetup : KeyManagerError()
    object SessionLocked : KeyManagerError()
    object BiometricNotEnrolled : KeyManagerError()
    data class CryptoError(val message: String) : KeyManagerError()
    data class StorageError(val message: String) : KeyManagerError()
    data class Unknown(val throwable: Throwable) : KeyManagerError()
}

class KeyManager(
    private val argon2Kdf: Argon2Kdf,
    private val platformKeyStore: PlatformKeyStore,
    private val sessionManager: SessionManager
) {
    private var vaultConfig: VaultConfig? = null

    private val _state = MutableStateFlow<KeyManagerState>(KeyManagerState.NotSetup)
    val state: StateFlow<KeyManagerState> = _state.asStateFlow()

    fun isVaultSetup(): Boolean {
        return vaultConfig?.isSetup == true
    }

    suspend fun setupVault(password: CharArray): KeyManagerResult<Unit> {
        if (isVaultSetup()) {
            return KeyManagerResult.Error(KeyManagerError.VaultAlreadySetup)
        }

        return try {
            val salt = argon2Kdf.generateSalt()
            val config = AdaptiveArgon2Config.getStandardConfig()
            val passwordKey = argon2Kdf.deriveKey(password, salt, config)

            val dataKey = CryptoUtils.generateSecureRandom(CryptoConstants.AES_KEY_SIZE)

            val encryptedDataKey = AesGcmCipher.encrypt(dataKey, passwordKey)

            MemorySanitizer.wipe(passwordKey)
            MemorySanitizer.wipe(dataKey)

            vaultConfig = VaultConfig(
                salt = salt,
                encryptedDataKey = encryptedDataKey,
                argon2Config = config,
                isSetup = true
            )

            _state.value = KeyManagerState.Ready
            KeyManagerResult.Success(Unit)
        } catch (e: Exception) {
            KeyManagerResult.Error(KeyManagerError.CryptoError(e.message ?: "Unknown error"))
        }
    }

    suspend fun unlockWithPassword(password: CharArray): KeyManagerResult<Unit> {
        val config = vaultConfig ?: return KeyManagerResult.Error(KeyManagerError.VaultNotSetup)

        if (!config.isSetup) {
            return KeyManagerResult.Error(KeyManagerError.VaultNotSetup)
        }

        return try {
            val passwordKey = argon2Kdf.deriveKey(
                password,
                config.salt,
                config.argon2Config
            )

            val dataKey = AesGcmCipher.decrypt(config.encryptedDataKey, passwordKey)

            MemorySanitizer.wipe(passwordKey)

            sessionManager.unlock(dataKey)
            MemorySanitizer.wipe(dataKey)

            _state.value = KeyManagerState.Unlocked
            KeyManagerResult.Success(Unit)
        } catch (e: Exception) {
            KeyManagerResult.Error(KeyManagerError.InvalidPassword)
        }
    }

    fun lock() {
        sessionManager.lock()
        _state.value = KeyManagerState.Locked
    }

    fun getDataKey(): ByteArray? {
        return if (sessionManager.isUnlocked()) {
            sessionManager.getDataKey()
        } else {
            null
        }
    }

    fun clear() {
        vaultConfig = null
        sessionManager.clear()
        platformKeyStore.deleteDeviceKey()
        _state.value = KeyManagerState.NotSetup
    }
}

data class VaultConfig(
    val salt: ByteArray,
    val encryptedDataKey: EncryptedData,
    val argon2Config: Argon2Config,
    val isSetup: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VaultConfig) return false
        return salt.contentEquals(other.salt)
    }

    override fun hashCode(): Int = salt.contentHashCode()
}

sealed class KeyManagerState {
    object NotSetup : KeyManagerState()
    object Ready : KeyManagerState()
    object Locked : KeyManagerState()
    object Unlocked : KeyManagerState()
    data class Error(val message: String) : KeyManagerState()
}