package com.securevault

import android.app.Application
import com.securevault.di.appModule
import com.securevault.di.createAndroidModule
import com.securevault.security.AppContextHolder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SecureVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.set(this)
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