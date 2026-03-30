package com.securevault.security

import android.app.Application
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import co.touchlab.kermit.Logger
import com.securevault.crypto.CryptoUtils
import java.security.KeyStore
import java.security.KeyFactory
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual class PlatformKeyStore {
    private val log = Logger.withTag("SvDeviceKey")
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    actual fun storeDeviceKey(key: ByteArray) {
        val context = getApplicationContext() ?: return
        val wrappingKey = getOrCreateWrappingKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val encrypted = cipher.doFinal(key)
        val iv = cipher.iv

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(DEVICE_KEY_CIPHER_B64, CryptoUtils.encodeBase64(encrypted))
            .putString(DEVICE_KEY_IV_B64, CryptoUtils.encodeBase64(iv))
            .apply()
    }

    actual fun getDeviceKey(): DeviceKeyLoadResult {
        val context = getApplicationContext() ?: run {
            log.w { "getDeviceKey: application context unavailable" }
            return DeviceKeyLoadResult.KeystoreError("no_application_context")
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedB64 = prefs.getString(DEVICE_KEY_CIPHER_B64, null)
        val ivB64 = prefs.getString(DEVICE_KEY_IV_B64, null)
        if (encryptedB64 == null || ivB64 == null) {
            return DeviceKeyLoadResult.NotPresent
        }

        return try {
            val encrypted = CryptoUtils.decodeBase64(encryptedB64)
            val iv = CryptoUtils.decodeBase64(ivB64)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrappingKey(), gcmSpec)
            val plain = cipher.doFinal(encrypted)
            DeviceKeyLoadResult.Success(plain)
        } catch (e: AEADBadTagException) {
            log.w { "getDeviceKey: authentication/decrypt failed (AEAD)" }
            DeviceKeyLoadResult.UnwrapFailed
        } catch (e: javax.crypto.BadPaddingException) {
            log.w { "getDeviceKey: decrypt failed (bad padding)" }
            DeviceKeyLoadResult.UnwrapFailed
        } catch (e: IllegalArgumentException) {
            log.w { "getDeviceKey: invalid stored encoding" }
            DeviceKeyLoadResult.UnwrapFailed
        } catch (e: java.security.GeneralSecurityException) {
            log.w { "getDeviceKey: crypto operation failed: ${e::class.simpleName}" }
            DeviceKeyLoadResult.UnwrapFailed
        } catch (e: Throwable) {
            log.e(e) { "getDeviceKey: unexpected error" }
            DeviceKeyLoadResult.KeystoreError("unexpected:${e::class.simpleName}")
        }
    }

    actual fun deleteDeviceKey() {
        val context = getApplicationContext()
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(DEVICE_KEY_CIPHER_B64)
            ?.remove(DEVICE_KEY_IV_B64)
            ?.apply()

        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    actual fun hasDeviceKey(): Boolean {
        val context = getApplicationContext() ?: return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasStoredBlob = prefs.contains(DEVICE_KEY_CIPHER_B64) && prefs.contains(DEVICE_KEY_IV_B64)
        return hasStoredBlob && keyStore.containsAlias(KEY_ALIAS)
    }

    actual fun isHardwareBacked(): Boolean {
        if (!keyStore.containsAlias(KEY_ALIAS)) return false
        return try {
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return false
            val keyFactory = KeyFactory.getInstance(secretKey.algorithm, ANDROID_KEYSTORE)
            val keyInfo = keyFactory.getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo
            keyInfo.isInsideSecureHardware
        } catch (_: Throwable) {
            false
        }
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getApplicationContext(): Context? {
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApplication = activityThread.getMethod("currentApplication").invoke(null)
            currentApplication as? Application
        } catch (_: Throwable) {
            null
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ALIAS = "securevault.device.wrapping.key"
        const val PREFS_NAME = "securevault.keystore"
        const val DEVICE_KEY_CIPHER_B64 = "device_key_cipher_b64"
        const val DEVICE_KEY_IV_B64 = "device_key_iv_b64"
        const val KEY_SIZE_BITS = 256
        const val TAG_SIZE_BITS = 128
    }
}
