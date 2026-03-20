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
data object SecuritySessionSettingsRoute : NavRoute
data object AppearanceSettingsRoute : NavRoute
data object AutofillSettingsRoute : NavRoute
data object VaultSettingsRoute : NavRoute
data object AboutRoute : NavRoute
data object SecurityModeRoute : NavRoute

data class DetailRoute(val entryId: Long?) : NavRoute
data class AddEditRoute(val entryId: Long?) : NavRoute

