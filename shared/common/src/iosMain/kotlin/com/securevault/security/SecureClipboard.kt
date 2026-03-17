package com.securevault.security

actual class SecureClipboard {
    actual fun copy(text: String, label: String) = Unit

    actual fun clear() = Unit

    actual fun scheduleAutoClear(delayMs: Long) = Unit
}
