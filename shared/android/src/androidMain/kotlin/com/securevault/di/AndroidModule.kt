package com.securevault.di

import android.content.Context
import com.securevault.crypto.Argon2Kdf
import com.securevault.data.ConfigRepository
import com.securevault.data.ConfigRepositoryImpl
import com.securevault.data.PasswordRepository
import com.securevault.data.PasswordRepositoryImpl
import com.securevault.data.createSqlDriver
import com.securevault.db.SecureVaultDatabase
import com.securevault.security.BiometricAuth
import com.securevault.security.ScreenSecurity
import com.securevault.security.SecureClipboard
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SessionManager
import com.securevault.security.KeyManager
import org.koin.dsl.module

fun createAndroidModule(context: Context) = module {
    single { createSqlDriver() }
    single { SecureVaultDatabase(get()) }
    single<PasswordRepository> { PasswordRepositoryImpl(get()) }
    single<ConfigRepository> { ConfigRepositoryImpl(get()) }

    single { PlatformKeyStore() }
    single { BiometricAuth() }
    single { ScreenSecurity() }
    single { SecureClipboard() }
    single { Argon2Kdf() }
    single { SessionManager() }
    factory { KeyManager(get(), get(), get(), get()) }
}