package com.securevault.di

import com.securevault.crypto.Argon2Kdf
import com.securevault.security.BiometricAuth
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SessionManager
import com.securevault.security.KeyManager
import org.koin.dsl.module

val iosModule = module {
    single { PlatformKeyStore() }
    single { BiometricAuth() }
    single { Argon2Kdf() }
    single { SessionManager() }
    factory { KeyManager(get(), get(), get()) }
}