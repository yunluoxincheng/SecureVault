package com.securevault.ui.navigation

import androidx.navigation3.runtime.NavKey

sealed interface NavRoute : NavKey

sealed interface MainTabRoute : NavRoute

data object OnboardingRoute : NavRoute
data object RegisterRoute : NavRoute
data object LoginRoute : NavRoute

data object VaultRoute : MainTabRoute
data object GeneratorRoute : MainTabRoute
data object SettingsRoute : MainTabRoute

data class DetailRoute(val entryId: Long?) : NavRoute
data class AddEditRoute(val entryId: Long?) : NavRoute

