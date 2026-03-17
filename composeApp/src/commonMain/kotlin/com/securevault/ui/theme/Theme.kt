package com.securevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    System,
    Light,
    Dark
}

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1B6B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA4F4D2),
    secondary = Color(0xFF4A635A),
    surface = Color(0xFFF8FAF7),
    surfaceVariant = Color(0xFFDBE5DE),
    error = Color(0xFFBA1A1A),
    outline = Color(0xFF707973)
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF7DDCB5),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF005138),
    secondary = Color(0xFFB3CCC1),
    surface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFF404944),
    error = Color(0xFFFFB4AB),
    outline = Color(0xFF8A938D)
)

@Composable
fun SecureVaultTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        content = content
    )
}
