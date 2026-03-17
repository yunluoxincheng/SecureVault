package com.securevault

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.securevault.di.desktopModule
import com.securevault.ui.navigation.SecureVaultApp
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(desktopModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "SecureVault"
    ) {
        SecureVaultApp()
    }
}