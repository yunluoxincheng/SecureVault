package com.securevault.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
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
        val clipData = ClipData.newPlainText(label, text)
        markClipAsSensitive(clipData)
        clipboard.setPrimaryClip(clipData)
    }

    actual fun clear() {
        val context = AppContextHolder.get() ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        runCatching { clipboard.clearPrimaryClip() }
        if (clipboard.hasPrimaryClip()) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    actual fun scheduleAutoClear(delayMs: Long) {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(delayMs)
            clear()
        }
    }

    private fun markClipAsSensitive(clipData: ClipData) {
        val extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            } else {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipData.description.extras = extras
    }
}
