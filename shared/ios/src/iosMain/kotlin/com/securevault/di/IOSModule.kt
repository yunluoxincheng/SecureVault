package com.securevault.di

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
import com.securevault.security.SecurityModeManager
import com.securevault.security.SessionManager
import com.securevault.security.KeyManager
import org.koin.dsl.module

val iosModule = module {
    single { createSqlDriver() }
    single { SecureVaultDatabase(get()) }
    single<ConfigRepository> { ConfigRepositoryImpl(get()) }

    single { PlatformKeyStore() }
    single { BiometricAuth() }
    single { ScreenSecurity() }
    single { SecureClipboard() }
    single { SecurityModeManager(get(), get()) }
    single<PasswordRepository> { PasswordRepositoryImpl(get(), get()) }
    single { Argon2Kdf() }
    single { SessionManager() }
    single { KeyManager(get(), get(), get(), get()) }
}