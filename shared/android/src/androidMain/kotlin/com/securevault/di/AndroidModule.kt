package com.securevault.di

import android.content.Context
import com.securevault.crypto.Argon2Kdf
import com.securevault.security.BiometricAuth
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SessionManager
import com.securevault.security.KeyManager
import org.koin.dsl.module

fun createAndroidModule(context: Context) = module {
    single { PlatformKeyStore(context) }
    single { BiometricAuth(context) }
    single { Argon2Kdf() }
    single { SessionManager() }
    factory { KeyManager(get(), get(), get()) }
}