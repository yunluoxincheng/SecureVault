package com.securevault.security

expect class AutofillSystemBridge() {
    fun isSupported(): Boolean
    fun isServiceEnabled(): Boolean
    fun openSystemAutofillSettings(): Boolean
}
