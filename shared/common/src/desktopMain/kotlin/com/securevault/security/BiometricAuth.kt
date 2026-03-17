package com.securevault.security

actual class BiometricAuth {
    actual fun isAvailable(): Boolean = false

    actual suspend fun authenticate(title: String, subtitle: String): BiometricResult {
        return BiometricResult.NotAvailable
    }
}