package com.securevault.security

expect class BiometricAuth() {
    fun isAvailable(): Boolean
    suspend fun authenticate(title: String, subtitle: String): BiometricResult
}

sealed class BiometricResult {
    object Success : BiometricResult()
    object Failed : BiometricResult()
    object Cancelled : BiometricResult()
    object NotAvailable : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}