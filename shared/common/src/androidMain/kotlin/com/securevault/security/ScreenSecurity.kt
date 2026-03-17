package com.securevault.security

import android.view.WindowManager

actual class ScreenSecurity {
    actual fun enableScreenshotProtection() {
        AndroidActivityProvider.get()?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    actual fun disableScreenshotProtection() {
        AndroidActivityProvider.get()?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
