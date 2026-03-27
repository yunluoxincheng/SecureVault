package com.securevault.security

actual class AutofillSystemBridge {
    actual fun isSupported(): Boolean = false
    actual fun isServiceEnabled(): Boolean = false
    actual fun openSystemAutofillSettings(): Boolean = false
}
