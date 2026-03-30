package com.securevault

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.securevault.crypto.LibsodiumManager
import com.securevault.di.appModule
import com.securevault.di.desktopModule
import com.securevault.ui.navigation.SecureVaultApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

fun main() {
    runBlocking(Dispatchers.Default) {
        LibsodiumManager.initialize()
    }
    application {
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
}