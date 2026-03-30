package com.securevault.security

import com.securevault.crypto.CryptoUtils
import java.util.prefs.Preferences

actual class PlatformKeyStore {
    private val prefs = Preferences.userRoot().node("com/securevault/keystore")
    private val protectedKeyKey = "device_key_dpapi_b64"
    private val migrationCompletedKey = "device_key_migrated_to_dpapi_v1"
    private val legacyEncryptedKeyKey = "encrypted_device_key"
    private val legacyMasterKeyKey = "master_key"

    actual fun storeDeviceKey(key: ByteArray) {
        if (isSecureStoreEnabled()) {
            val keyBase64 = CryptoUtils.encodeBase64(key)
            val protected = protectStringWithDpapi(keyBase64)
            prefs.put(protectedKeyKey, protected)
            prefs.putBoolean(migrationCompletedKey, true)
            clearLegacyStorage()
            return
        }

        // Rollback drills can force-disable secure store and keep legacy write behavior.
        storeLegacyXor(key)
    }

    actual fun getDeviceKey(): DeviceKeyLoadResult {
        if (!isSecureStoreEnabled()) {
            if (!isLegacyRollbackReadEnabled()) {
                return DeviceKeyLoadResult.KeystoreError("desktop_secure_store_disabled")
            }
            return getLegacyXorDeviceKey()
        }

        val protectedKeyB64 = prefs.get(protectedKeyKey, null)
        if (protectedKeyB64 != null) {
            return try {
                val keyBase64 = unprotectStringWithDpapi(protectedKeyB64)
                DeviceKeyLoadResult.Success(CryptoUtils.decodeBase64(keyBase64))
            } catch (_: IllegalArgumentException) {
                DeviceKeyLoadResult.UnwrapFailed
            } catch (_: Exception) {
                DeviceKeyLoadResult.KeystoreError("desktop_dpapi_read")
            }
        }

        // One-time migration from legacy XOR wrapping (older desktop builds).
        return migrateLegacyIfPresent()
    }

    private fun migrateLegacyIfPresent(): DeviceKeyLoadResult {
        val legacyResult = getLegacyXorDeviceKey()
        val migratedKey = when (legacyResult) {
            is DeviceKeyLoadResult.Success -> legacyResult.key
            DeviceKeyLoadResult.NotPresent -> return DeviceKeyLoadResult.NotPresent
            DeviceKeyLoadResult.UnwrapFailed -> return DeviceKeyLoadResult.UnwrapFailed
            is DeviceKeyLoadResult.KeystoreError -> return legacyResult
        }

        return try {
            storeDeviceKey(migratedKey)
            val verify = getDeviceKey()
            if (verify is DeviceKeyLoadResult.Success && verify.key.contentEquals(migratedKey)) {
                clearLegacyStorage()
                DeviceKeyLoadResult.Success(migratedKey.copyOf())
            } else {
                DeviceKeyLoadResult.KeystoreError("desktop_migration_verify_failed")
            }
        } finally {
            MemorySanitizer.wipe(migratedKey)
        }
    }

    actual fun deleteDeviceKey() {
        prefs.remove(protectedKeyKey)
        prefs.remove(migrationCompletedKey)
        clearLegacyStorage()
    }

    actual fun hasDeviceKey(): Boolean {
        if (isSecureStoreEnabled()) {
            return prefs.get(protectedKeyKey, null) != null || prefs.get(legacyEncryptedKeyKey, null) != null
        }
        return if (isLegacyRollbackReadEnabled()) {
            prefs.get(legacyEncryptedKeyKey, null) != null
        } else {
            false
        }
    }

    actual fun isHardwareBacked(): Boolean = false

    private fun storeLegacyXor(key: ByteArray) {
        val masterKey = getOrCreateLegacyMasterKey()
        val encryptedKey = xorWithKey(key, masterKey)
        prefs.put(legacyEncryptedKeyKey, CryptoUtils.encodeBase64(encryptedKey))
    }

    private fun getLegacyXorDeviceKey(): DeviceKeyLoadResult {
        val encryptedKeyB64 = prefs.get(legacyEncryptedKeyKey, null) ?: return DeviceKeyLoadResult.NotPresent
        return try {
            val masterKey = getOrCreateLegacyMasterKey()
            val encryptedKey = CryptoUtils.decodeBase64(encryptedKeyB64)
            DeviceKeyLoadResult.Success(xorWithKey(encryptedKey, masterKey))
        } catch (_: IllegalArgumentException) {
            DeviceKeyLoadResult.UnwrapFailed
        } catch (_: Exception) {
            DeviceKeyLoadResult.KeystoreError("desktop_legacy_read")
        }
    }

    private fun clearLegacyStorage() {
        prefs.remove(legacyEncryptedKeyKey)
        prefs.remove(legacyMasterKeyKey)
        runCatching { prefs.flush() }
    }

    private fun getOrCreateLegacyMasterKey(): ByteArray {
        val storedKey = prefs.get(legacyMasterKeyKey, null)
        if (storedKey != null) {
            return CryptoUtils.decodeBase64(storedKey)
        }
        val newKey = CryptoUtils.generateSecureRandom(32)
        prefs.put(legacyMasterKeyKey, CryptoUtils.encodeBase64(newKey))
        return newKey
    }

    private fun protectStringWithDpapi(plainText: String): String {
        if (!isWindows()) return plainText
        return runPowerShell(
            "\$secure = ConvertTo-SecureString -String ([Environment]::GetEnvironmentVariable('SV_DPAPI_INPUT')) -AsPlainText -Force; ConvertFrom-SecureString -SecureString \$secure",
            plainText
        )
    }

    private fun unprotectStringWithDpapi(protectedText: String): String {
        if (!isWindows()) return protectedText
        return runPowerShell(
            "\$secure = ConvertTo-SecureString -String ([Environment]::GetEnvironmentVariable('SV_DPAPI_INPUT')); " +
                "\$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR(\$secure); " +
                "try { [Runtime.InteropServices.Marshal]::PtrToStringUni(\$bstr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR(\$bstr) }",
            protectedText
        )
    }

    private fun runPowerShell(commandBody: String, arg: String): String {
        val processBuilder = ProcessBuilder(
            "powershell",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            commandBody
        )
        processBuilder.environment()["SV_DPAPI_INPUT"] = arg
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val stderr = process.errorStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        if (exitCode != 0 || stdout.isEmpty()) {
            throw IllegalStateException("powershell_failed:${if (stderr.isEmpty()) exitCode else stderr}")
        }
        return stdout
    }

    private fun isSecureStoreEnabled(): Boolean {
        if (!isWindows()) return false
        val value = System.getProperty(SYSTEM_PROP_SECURE_STORE_ENABLED)
            ?: System.getenv(ENV_SECURE_STORE_ENABLED)
        return value?.toBooleanStrictOrNull() ?: true
    }

    private fun isLegacyRollbackReadEnabled(): Boolean {
        val value = System.getProperty(SYSTEM_PROP_LEGACY_READ_ENABLED)
            ?: System.getenv(ENV_LEGACY_READ_ENABLED)
        return value?.toBooleanStrictOrNull() ?: false
    }

    private fun isWindows(): Boolean {
        val osName = System.getProperty("os.name") ?: return false
        return osName.contains("windows", ignoreCase = true)
    }

    private fun xorWithKey(data: ByteArray, key: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return result
    }

    private companion object {
        const val ENV_SECURE_STORE_ENABLED = "SV_DESKTOP_DPAPI_ENABLED"
        const val ENV_LEGACY_READ_ENABLED = "SV_DESKTOP_LEGACY_READ_ENABLED"
        const val SYSTEM_PROP_SECURE_STORE_ENABLED = "securevault.keystore.desktop.dpapi.enabled"
        const val SYSTEM_PROP_LEGACY_READ_ENABLED = "securevault.keystore.desktop.legacy_read_enabled"
    }
}