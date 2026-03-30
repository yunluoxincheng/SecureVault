package com.securevault

import android.app.Application
import com.securevault.crypto.LibsodiumManager
import com.securevault.di.appModule
import com.securevault.di.createAndroidModule
import com.securevault.security.AppContextHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SecureVaultApp : Application() {
    private val applicationCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.set(this)
        applicationCoroutineScope.launch {
            LibsodiumManager.initialize()
        }
        startKoin {
            androidLogger()
            androidContext(this@SecureVaultApp)
            modules(
                createAndroidModule(this@SecureVaultApp),
                appModule
            )
        }
    }
}