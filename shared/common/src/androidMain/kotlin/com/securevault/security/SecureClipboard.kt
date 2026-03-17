package com.securevault.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual class SecureClipboard {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var clearJob: Job? = null

    actual fun copy(text: String, label: String) {
        val context = AppContextHolder.get() ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    actual fun clear() {
        val context = AppContextHolder.get() ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }

    actual fun scheduleAutoClear(delayMs: Long) {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(delayMs)
            clear()
        }
    }
}
