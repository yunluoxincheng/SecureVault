package com.securevault

import android.app.Application
import com.securevault.di.createAndroidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SecureVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@SecureVaultApp)
            modules(createAndroidModule(this@SecureVaultApp))
        }
    }
}