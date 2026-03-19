package com.securevault.di

import com.securevault.util.PasswordGenerator
import com.securevault.viewmodel.AddEditPasswordViewModel
import com.securevault.viewmodel.AuthFlowViewModel
import com.securevault.viewmodel.GeneratorViewModel
import com.securevault.viewmodel.PasswordDetailViewModel
import com.securevault.viewmodel.SecurityModeViewModel
import com.securevault.viewmodel.SettingsViewModel
import com.securevault.viewmodel.UnlockViewModel
import com.securevault.viewmodel.VaultViewModel
import org.koin.dsl.module

val appModule = module {
    single { PasswordGenerator() }
    factory { AuthFlowViewModel(get()) }

    factory { VaultViewModel(get(), get()) }
    factory { PasswordDetailViewModel(get(), get(), get(), get()) }
    factory { AddEditPasswordViewModel(get(), get()) }
    factory { UnlockViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get(), get(), get(), get()) }
    factory { SecurityModeViewModel(get()) }
    factory { GeneratorViewModel(get(), get()) }
}
