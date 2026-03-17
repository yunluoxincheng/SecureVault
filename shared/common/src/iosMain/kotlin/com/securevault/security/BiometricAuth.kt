package com.securevault.security

import LocalAuthentication.LAContext
import LocalAuthentication.LAPolicy
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume

actual class BiometricAuth {
    actual fun isAvailable(): Boolean {
        val context = LAContext()
        var error: NSError? = null
        return context.canEvaluatePolicy(LAPolicy.kLAPolicyDeviceOwnerAuthenticationWithBiometrics, error)
    }

    actual suspend fun authenticate(title: String, subtitle: String): BiometricResult {
        return suspendCancellableCoroutine { continuation ->
            val context = LAContext()
            var error: NSError? = null
            if (!context.canEvaluatePolicy(LAPolicy.kLAPolicyDeviceOwnerAuthenticationWithBiometrics, error)) {
                continuation.resume(BiometricResult.NotAvailable)
                return@suspendCancellableCoroutine
            }

            context.evaluatePolicy(
                LAPolicy.kLAPolicyDeviceOwnerAuthenticationWithBiometrics,
                subtitle
            ) { success, authError ->
                if (success) {
                    continuation.resume(BiometricResult.Success)
                } else {
                    when (authError?.code) {
                        -3L, -2L -> continuation.resume(BiometricResult.Cancelled)
                        -7L, -8L -> continuation.resume(BiometricResult.NotAvailable)
                        else -> continuation.resume(BiometricResult.Failed)
                    }
                }
            }
        }
    }
}
