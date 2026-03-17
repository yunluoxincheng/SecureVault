package com.securevault

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.securevault.di.appModule
import com.securevault.di.desktopModule
import com.securevault.ui.navigation.SecureVaultApp
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(
            desktopModule,
            appModule
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "SecureVault"
    ) {
        SecureVaultApp()
    }
}