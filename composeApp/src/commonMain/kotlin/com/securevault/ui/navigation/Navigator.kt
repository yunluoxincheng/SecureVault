package com.securevault.ui.navigation

class Navigator(
    private val state: NavigationState,
    private val rootTab: MainTabRoute = VaultRoute,
) {
    fun navigate(route: NavRoute) {
        val topLevelRoute = route as? MainTabRoute
        if (topLevelRoute != null && topLevelRoute in state.mainTabs) {
            state.currentTab = topLevelRoute
            state.currentSurface = NavigationSurface.Main
            return
        }
        state.activeBackStack.add(route)
    }

    fun goBack() {
        when (state.currentSurface) {
            NavigationSurface.Auth -> {
                if (state.authBackStack.size > 1) {
                    state.authBackStack.removeAt(state.authBackStack.lastIndex)
                }
            }
            NavigationSurface.Main -> {
                val stack = state.mainBackStacks.getValue(state.currentTab)
                if (stack.size > 1) {
                    stack.removeAt(stack.lastIndex)
                } else if (state.currentTab != rootTab) {
                    state.currentTab = rootTab
                }
            }
        }
    }

    fun resetToAuth(route: NavRoute) {
        state.resetAuthStack(route)
    }

    fun resetToMainRoot(tab: MainTabRoute = rootTab) {
        state.resetMainTabToRoot(tab)
    }
}

