package com.securevault.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorRegressionTest {

    private val mainTabs = setOf(VaultRoute, GeneratorRoute, SettingsRoute)

    @Test
    fun onboarding_to_register_to_login_flow_resets_auth_stack() {
        val state = NavigationState(
            authStartRoute = OnboardingRoute,
            mainTabs = mainTabs,
            mainStartTab = VaultRoute,
        )
        val navigator = Navigator(state)

        navigator.resetToAuth(RegisterRoute)
        assertEquals(listOf(RegisterRoute), state.authBackStack.toList())

        navigator.resetToAuth(LoginRoute)
        assertEquals(listOf(LoginRoute), state.authBackStack.toList())

        navigator.goBack()
        assertEquals(listOf(LoginRoute), state.authBackStack.toList())
        assertEquals(NavigationSurface.Auth, state.currentSurface)
    }

    @Test
    fun unlock_to_vault_resets_to_main_root_and_back_is_stable() {
        val state = NavigationState(
            authStartRoute = LoginRoute,
            mainTabs = mainTabs,
            mainStartTab = VaultRoute,
        )
        val navigator = Navigator(state)

        navigator.navigate(RegisterRoute)
        assertEquals(listOf(LoginRoute, RegisterRoute), state.authBackStack.toList())

        navigator.resetToMainRoot(VaultRoute)
        assertEquals(NavigationSurface.Main, state.currentSurface)
        assertEquals(VaultRoute, state.currentTab)
        assertEquals(listOf(VaultRoute), state.activeBackStack.toList())

        navigator.goBack()
        assertEquals(listOf(VaultRoute), state.activeBackStack.toList())
        assertEquals(VaultRoute, state.currentTab)
    }

    @Test
    fun bottom_tab_switch_and_back_behaviour_matches_expected_path() {
        val state = NavigationState(
            authStartRoute = LoginRoute,
            mainTabs = mainTabs,
            mainStartTab = VaultRoute,
        )
        val navigator = Navigator(state)
        navigator.resetToMainRoot(VaultRoute)

        navigator.navigate(GeneratorRoute)
        assertEquals(GeneratorRoute, state.currentTab)
        assertEquals(listOf(GeneratorRoute), state.activeBackStack.toList())

        navigator.navigate(DetailRoute(1L))
        assertEquals(listOf(GeneratorRoute, DetailRoute(1L)), state.activeBackStack.toList())

        navigator.goBack()
        assertEquals(listOf(GeneratorRoute), state.activeBackStack.toList())

        navigator.goBack()
        assertEquals(VaultRoute, state.currentTab)
        assertEquals(listOf(VaultRoute), state.activeBackStack.toList())
    }

    @Test
    fun detail_to_add_edit_save_returns_to_vault_root() {
        val state = NavigationState(
            authStartRoute = LoginRoute,
            mainTabs = mainTabs,
            mainStartTab = VaultRoute,
        )
        val navigator = Navigator(state)
        navigator.resetToMainRoot(VaultRoute)

        navigator.navigate(DetailRoute(7L))
        navigator.navigate(AddEditRoute(7L))
        assertEquals(listOf(VaultRoute, DetailRoute(7L), AddEditRoute(7L)), state.activeBackStack.toList())

        navigator.resetToMainRoot(VaultRoute)
        assertEquals(NavigationSurface.Main, state.currentSurface)
        assertEquals(VaultRoute, state.currentTab)
        assertEquals(listOf(VaultRoute), state.activeBackStack.toList())
    }

    @Test
    fun lock_from_settings_goes_to_login_and_cannot_go_back_to_main() {
        val state = NavigationState(
            authStartRoute = LoginRoute,
            mainTabs = mainTabs,
            mainStartTab = VaultRoute,
        )
        val navigator = Navigator(state)
        navigator.resetToMainRoot(VaultRoute)
        navigator.navigate(SettingsRoute)
        assertEquals(SettingsRoute, state.currentTab)

        navigator.resetToAuth(LoginRoute)
        assertEquals(NavigationSurface.Auth, state.currentSurface)
        assertEquals(listOf(LoginRoute), state.authBackStack.toList())

        navigator.goBack()
        assertEquals(NavigationSurface.Auth, state.currentSurface)
        assertEquals(listOf(LoginRoute), state.authBackStack.toList())
    }
}
