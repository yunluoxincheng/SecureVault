package com.securevault.security

import com.securevault.crypto.CryptoUtils
import platform.Foundation.NSUserDefaults

actual class PlatformKeyStore {
    private val deviceKeyTag = "device_key"
    private val masterKeyTag = "master_key"

    actual fun storeDeviceKey(key: ByteArray) {
        deleteDeviceKey()
        val masterKey = getOrCreateMasterKey()
        val encryptedKey = xorWithKey(key, masterKey)
        NSUserDefaults.standardUserDefaults.setObject(CryptoUtils.encodeBase64(encryptedKey), forKey = deviceKeyTag)
    }

    actual fun getDeviceKey(): ByteArray? {
        val encryptedKeyB64 = NSUserDefaults.standardUserDefaults.stringForKey(deviceKeyTag) ?: return null
        val masterKey = getOrCreateMasterKey()
        val encryptedKey = CryptoUtils.decodeBase64(encryptedKeyB64)
        return xorWithKey(encryptedKey, masterKey)
    }

    actual fun deleteDeviceKey() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(deviceKeyTag)
    }

    actual fun hasDeviceKey(): Boolean {
        return NSUserDefaults.standardUserDefaults.stringForKey(deviceKeyTag) != null
    }

    actual fun isHardwareBacked(): Boolean = false

    private fun getOrCreateMasterKey(): ByteArray {
        val storedKey = NSUserDefaults.standardUserDefaults.stringForKey(masterKeyTag)
        if (storedKey != null) {
            return CryptoUtils.decodeBase64(storedKey)
        }
        val newKey = CryptoUtils.generateSecureRandom(32)
        NSUserDefaults.standardUserDefaults.setObject(CryptoUtils.encodeBase64(newKey), forKey = masterKeyTag)
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
