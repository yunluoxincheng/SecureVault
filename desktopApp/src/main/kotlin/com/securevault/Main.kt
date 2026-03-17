package com.securevault

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.securevault.di.desktopModule
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

@androidx.compose.runtime.Composable
fun SecureVaultApp() {
    androidx.compose.material3.MaterialTheme {
        androidx.compose.material3.Surface {
            androidx.compose.material3.Text("SecureVault")
        }
    }
}