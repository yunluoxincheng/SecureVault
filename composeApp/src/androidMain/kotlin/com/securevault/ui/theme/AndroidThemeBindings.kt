package com.securevault.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun ProvideAndroidThemeBindings(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context.findActivity()

    val dynamicProvider: DynamicColorSchemeProvider = { isDark ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            null
        }
    }

    val systemBarsApplier: SystemBarsStyleApplier = { isDark ->
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalDynamicColorSchemeProvider provides dynamicProvider,
        LocalSystemBarsStyleApplier provides systemBarsApplier
    ) {
        content()
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext ?: break
    }
    return null
}
