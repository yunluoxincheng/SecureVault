package com.securevault.security

import com.securevault.crypto.CryptoConstants

expect class SecureClipboard() {
    fun copy(text: String, label: String = "Password")
    fun clear()
    fun scheduleAutoClear(delayMs: Long = CryptoConstants.Clipboard.DEFAULT_CLEAR_TIMEOUT_MS)
}
