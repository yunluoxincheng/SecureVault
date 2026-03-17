package com.securevault.security

expect class SecureClipboard() {
    fun copy(text: String, label: String = "Password")
    fun clear()
    fun scheduleAutoClear(delayMs: Long = 30_000)
}
