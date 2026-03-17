package com.securevault.security

import android.content.Context
import com.securevault.crypto.CryptoUtils
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual class PlatformKeyStore(private val context: Context) {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "securevault_device_key"
    private val prefsName = "securevault_prefs"
    private val encryptedKeyPref = "encrypted_device_key"

    actual fun storeDeviceKey(key: ByteArray) {
        deleteDeviceKey()

        val keyGenerator = KeyGenerator.getInstance(
            "AES",
            "AndroidKeyStore"
        )

        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            keyAlias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        val secretKey = keyGenerator.generateKey()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedKey = cipher.doFinal(key)

        val iv = cipher.iv
        val combined = iv + encryptedKey
        saveEncryptedKeyToPreferences(combined)
    }

    actual fun getDeviceKey(): ByteArray? {
        if (!hasDeviceKey()) return null

        return try {
            val combined = loadEncryptedKeyFromPreferences() ?: return null
            val iv = combined.sliceArray(0 until 12)
            val encryptedKey = combined.sliceArray(12 until combined.size)

            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            cipher.doFinal(encryptedKey)
        } catch (e: Exception) {
            null
        }
    }

    actual fun deleteDeviceKey() {
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
        clearEncryptedKeyFromPreferences()
    }

    actual fun hasDeviceKey(): Boolean {
        return keyStore.containsAlias(keyAlias) && loadEncryptedKeyFromPreferences() != null
    }

    actual fun isHardwareBacked(): Boolean {
        return try {
            if (!keyStore.containsAlias(keyAlias)) return false
            val keyInfo = android.security.keystore.KeyInfo::class.java.cast(
                (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            )
            keyInfo?.securityLevel == android.security.keystore.KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
            keyInfo?.securityLevel == android.security.keystore.KeyProperties.SECURITY_LEVEL_STRONGBOX
        } catch (e: Exception) {
            false
        }
    }

    private fun saveEncryptedKeyToPreferences(data: ByteArray) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(encryptedKeyPref, CryptoUtils.encodeBase64(data))
            .apply()
    }

    private fun loadEncryptedKeyFromPreferences(): ByteArray? {
        val data = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(encryptedKeyPref, null) ?: return null
        return CryptoUtils.decodeBase64(data)
    }

    private fun clearEncryptedKeyFromPreferences() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .remove(encryptedKeyPref)
            .apply()
    }
}