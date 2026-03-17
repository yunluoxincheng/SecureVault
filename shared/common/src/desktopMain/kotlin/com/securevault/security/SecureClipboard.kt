package com.securevault.security

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual class SecureClipboard {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var clearJob: Job? = null

    actual fun copy(text: String, label: String) {
        runCatching {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
        }
    }

    actual fun clear() {
        runCatching {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(""), null)
        }
    }

    actual fun scheduleAutoClear(delayMs: Long) {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(delayMs)
            clear()
        }
    }
}
