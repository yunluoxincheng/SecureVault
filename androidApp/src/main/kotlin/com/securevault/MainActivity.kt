package com.securevault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.securevault.security.AndroidActivityProvider
import com.securevault.security.KeyManager
import com.securevault.security.ScreenSecurity
import com.securevault.ui.navigation.SecureVaultApp
import com.securevault.ui.theme.ProvideAndroidThemeBindings
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val screenSecurity: ScreenSecurity by inject()
    private val keyManager: KeyManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidActivityProvider.set(this)
        screenSecurity.enableScreenshotProtection()
        enableEdgeToEdge()
        setContent {
            ProvideAndroidThemeBindings {
                SecureVaultApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidActivityProvider.set(this)
        keyManager.onAppForeground()
    }

    override fun onStop() {
        keyManager.onAppBackground()
        super.onStop()
    }

    override fun onDestroy() {
        AndroidActivityProvider.clear(this)
        super.onDestroy()
    }
}