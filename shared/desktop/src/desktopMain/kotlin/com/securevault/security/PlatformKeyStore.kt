package com.securevault.security

import com.securevault.crypto.MemorySanitizer
import com.securevault.crypto.CryptoUtils
import java.util.prefs.Preferences

actual class PlatformKeyStore {
    private val prefs = Preferences.userRoot().node("com/securevault/keystore")
    private val encryptedKeyKey = "encrypted_device_key"
    private val ivKey = "iv"
    private val masterKeyKey = "master_key"

    actual fun storeDeviceKey(key: ByteArray) {
        val iv = CryptoUtils.generateSecureRandom(12)
        val masterKey = getOrCreateMasterKey()
        val encryptedKey = xorWithKey(key, masterKey)

        prefs.put(encryptedKeyKey, CryptoUtils.encodeBase64(encryptedKey))
        prefs.put(ivKey, CryptoUtils.encodeBase64(iv))
    }

    actual fun getDeviceKey(): ByteArray? {
        val encryptedKeyB64 = prefs.get(encryptedKeyKey, null) ?: return null
        val masterKey = getOrCreateMasterKey()
        val encryptedKey = CryptoUtils.decodeBase64(encryptedKeyB64)

        return xorWithKey(encryptedKey, masterKey)
    }

    actual fun deleteDeviceKey() {
        prefs.remove(encryptedKeyKey)
        prefs.remove(ivKey)
    }

    actual fun hasDeviceKey(): Boolean {
        return prefs.get(encryptedKeyKey, null) != null
    }

    actual fun isHardwareBacked(): Boolean {
        return false
    }

    private fun getOrCreateMasterKey(): ByteArray {
        val storedKey = prefs.get(masterKeyKey, null)
        if (storedKey != null) {
            return CryptoUtils.decodeBase64(storedKey)
        }

        val newKey = CryptoUtils.generateSecureRandom(32)
        prefs.put(masterKeyKey, CryptoUtils.encodeBase64(newKey))
        return newKey
    }

    private fun xorWithKey(data: ByteArray, key: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return result
    }
}