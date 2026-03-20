package com.securevault.security

import com.securevault.crypto.*
import com.securevault.data.ConfigRepository
import com.securevault.data.VaultConfigKeys
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
    private val sessionManager: SessionManager,
    private val configRepository: ConfigRepository? = null
) {
    private var vaultConfig: VaultConfig? = null

    private val _state = MutableStateFlow<KeyManagerState>(KeyManagerState.NotSetup)
    val state: StateFlow<KeyManagerState> = _state.asStateFlow()

    fun isVaultSetup(): Boolean {
        return vaultConfig?.isSetup == true
    }

    suspend fun setupVault(password: CharArray): KeyManagerResult<Unit> {
        if (isVaultSetup() || loadVaultConfigIfNeeded()) {
            return KeyManagerResult.Error(KeyManagerError.VaultAlreadySetup)
        }

        return try {
            val salt = argon2Kdf.generateSalt()
            val config = AdaptiveArgon2Config.getStandardConfig()
            val passwordKey = argon2Kdf.deriveKey(password, salt, config)

            val dataKey = CryptoUtils.generateSecureRandom(CryptoConstants.AES_KEY_SIZE)

            val encryptedDataKey = AesGcmCipher.encrypt(dataKey, passwordKey)
            platformKeyStore.storeDeviceKey(dataKey)

            sessionManager.unlock(dataKey)

            MemorySanitizer.wipe(passwordKey)
            MemorySanitizer.wipe(dataKey)

            vaultConfig = VaultConfig(
                salt = salt,
                encryptedDataKey = encryptedDataKey,
                argon2Config = config,
                isSetup = true
            )
            saveVaultConfig(vaultConfig!!)

            _state.value = KeyManagerState.Unlocked
            KeyManagerResult.Success(Unit)
        } catch (e: Exception) {
            KeyManagerResult.Error(KeyManagerError.CryptoError(e.message ?: "Unknown error"))
        } finally {
            MemorySanitizer.wipe(password)
        }
    }

    suspend fun unlockWithPassword(password: CharArray): KeyManagerResult<Unit> {
        if (vaultConfig == null) {
            loadVaultConfigIfNeeded()
        }
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
            platformKeyStore.storeDeviceKey(dataKey)

            MemorySanitizer.wipe(passwordKey)

            sessionManager.unlock(dataKey)
            MemorySanitizer.wipe(dataKey)

            _state.value = KeyManagerState.Unlocked
            KeyManagerResult.Success(Unit)
        } catch (e: Exception) {
            KeyManagerResult.Error(KeyManagerError.InvalidPassword)
        } finally {
            MemorySanitizer.wipe(password)
        }
    }

    suspend fun changeMasterPassword(currentPassword: CharArray, newPassword: CharArray): KeyManagerResult<Unit> {
        if (vaultConfig == null) {
            loadVaultConfigIfNeeded()
        }
        val config = vaultConfig ?: return KeyManagerResult.Error(KeyManagerError.VaultNotSetup)

        return try {
            val currentPasswordKey = argon2Kdf.deriveKey(currentPassword, config.salt, config.argon2Config)
            val dataKey = AesGcmCipher.decrypt(config.encryptedDataKey, currentPasswordKey)

            val newSalt = argon2Kdf.generateSalt()
            val newConfig = AdaptiveArgon2Config.getStandardConfig()
            val newPasswordKey = argon2Kdf.deriveKey(newPassword, newSalt, newConfig)
            val newEncryptedDataKey = AesGcmCipher.encrypt(dataKey, newPasswordKey)
            platformKeyStore.storeDeviceKey(dataKey)

            vaultConfig = config.copy(
                salt = newSalt,
                encryptedDataKey = newEncryptedDataKey,
                argon2Config = newConfig
            )
            saveVaultConfig(vaultConfig!!)

            sessionManager.unlock(dataKey)

            MemorySanitizer.wipe(currentPasswordKey)
            MemorySanitizer.wipe(newPasswordKey)
            MemorySanitizer.wipe(dataKey)

            _state.value = KeyManagerState.Unlocked
            KeyManagerResult.Success(Unit)
        } catch (e: Exception) {
            KeyManagerResult.Error(KeyManagerError.InvalidPassword)
        } finally {
            MemorySanitizer.wipe(currentPassword)
            MemorySanitizer.wipe(newPassword)
        }
    }

    fun lock() {
        sessionManager.lock()
        _state.value = KeyManagerState.Locked
    }

    fun setSessionLockTimeout(timeoutMs: Long) {
        sessionManager.setLockTimeout(timeoutMs)
    }

    fun onAppBackground() {
        val locked = sessionManager.onAppBackground()
        if (locked) {
            _state.value = KeyManagerState.Locked
        }
    }

    fun onAppForeground(): Boolean {
        val locked = sessionManager.onAppForeground()
        if (locked) {
            _state.value = KeyManagerState.Locked
        }
        return locked
    }

    fun checkAutoLock(): Boolean {
        val locked = sessionManager.checkAutoLock()
        if (locked) {
            _state.value = KeyManagerState.Locked
        }
        return locked
    }

    fun canUnlockWithBiometric(): Boolean {
        return platformKeyStore.hasDeviceKey()
    }

    suspend fun unlockWithBiometric(): KeyManagerResult<Unit> {
        if (vaultConfig == null) {
            loadVaultConfigIfNeeded()
        }
        if (vaultConfig == null || !vaultConfig!!.isSetup) {
            return KeyManagerResult.Error(KeyManagerError.VaultNotSetup)
        }

        val dataKey = platformKeyStore.getDeviceKey()
            ?: return KeyManagerResult.Error(KeyManagerError.BiometricNotEnrolled)

        return try {
            sessionManager.unlock(dataKey)
            _state.value = KeyManagerState.Unlocked
            KeyManagerResult.Success(Unit)
        } catch (e: Exception) {
            KeyManagerResult.Error(KeyManagerError.StorageError(e.message ?: "Device key unavailable"))
        } finally {
            MemorySanitizer.wipe(dataKey)
        }
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

    private suspend fun loadVaultConfigIfNeeded(): Boolean {
        if (vaultConfig != null) return true
        val repository = configRepository ?: return false

        val saltBase64 = repository.get(VaultConfigKeys.Salt) ?: return false
        val encryptedDataKeyStorage = repository.get(VaultConfigKeys.EncryptedDataKeyPassword) ?: return false
        val memory = repository.get(VaultConfigKeys.Argon2MemoryKb)?.toIntOrNull() ?: return false
        val iterations = repository.get(VaultConfigKeys.Argon2Iterations)?.toIntOrNull() ?: return false
        val parallelism = repository.get(VaultConfigKeys.Argon2Parallelism)?.toIntOrNull() ?: return false
        val outputLength = repository.get(VaultConfigKeys.Argon2OutputLength)?.toIntOrNull() ?: return false

        vaultConfig = VaultConfig(
            salt = CryptoUtils.decodeBase64(saltBase64),
            encryptedDataKey = EncryptedData.fromStorageFormat(encryptedDataKeyStorage),
            argon2Config = Argon2Config(
                memoryKB = memory,
                iterations = iterations,
                parallelism = parallelism,
                outputLength = outputLength
            ),
            isSetup = true
        )
        _state.value = KeyManagerState.Locked
        return true
    }

    private suspend fun saveVaultConfig(config: VaultConfig) {
        val repository = configRepository ?: return
        repository.set(VaultConfigKeys.Salt, CryptoUtils.encodeBase64(config.salt))
        repository.set(VaultConfigKeys.EncryptedDataKeyPassword, config.encryptedDataKey.toStorageFormat())
        repository.set(VaultConfigKeys.Argon2MemoryKb, config.argon2Config.memoryKB.toString())
        repository.set(VaultConfigKeys.Argon2Iterations, config.argon2Config.iterations.toString())
        repository.set(VaultConfigKeys.Argon2Parallelism, config.argon2Config.parallelism.toString())
        repository.set(VaultConfigKeys.Argon2OutputLength, config.argon2Config.outputLength.toString())
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