package com.securevault.security

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager

actual class AutofillSystemBridge {
    actual fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    actual fun isServiceEnabled(): Boolean {
        val context = AppContextHolder.get() ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(AutofillManager::class.java) ?: return false
        return manager.hasEnabledAutofillServices()
    }

    actual fun openSystemAutofillSettings(): Boolean {
        val context = AppContextHolder.get() ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
