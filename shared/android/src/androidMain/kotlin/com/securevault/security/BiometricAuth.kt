package com.securevault.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class BiometricAuth(private val context: Context) {
    private val biometricManager = BiometricManager.from(context)

    actual fun isAvailable(): Boolean {
        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(title: String, subtitle: String): BiometricResult {
        return suspendCancellableCoroutine { continuation ->
            val activity = context as? FragmentActivity
            if (activity == null) {
                continuation.resume(BiometricResult.Error("Not an activity context"))
                return@suspendCancellableCoroutine
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    continuation.resume(BiometricResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            continuation.resume(BiometricResult.Cancelled)
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                            continuation.resume(BiometricResult.NotAvailable)
                        }
                        else -> {
                            continuation.resume(BiometricResult.Error(errString.toString()))
                        }
                    }
                }
            }

            val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context), callback)
            biometricPrompt.authenticate(promptInfo)
        }
    }
}