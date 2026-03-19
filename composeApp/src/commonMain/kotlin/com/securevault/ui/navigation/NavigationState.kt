package com.securevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

enum class NavigationSurface {
    Auth,
    Main
}

class NavigationState(
    authStartRoute: NavRoute,
    val mainTabs: Set<MainTabRoute>,
    mainStartTab: MainTabRoute,
) {
    val authBackStack: SnapshotStateList<NavRoute> = mutableStateListOf(authStartRoute)
    val mainBackStacks: Map<MainTabRoute, SnapshotStateList<NavRoute>> = mainTabs.associateWith { tab ->
        mutableStateListOf<NavRoute>(tab)
    }

    var currentSurface by mutableStateOf(NavigationSurface.Auth)
    var currentTab by mutableStateOf(mainStartTab)

    val activeBackStack: SnapshotStateList<NavRoute>
        get() = when (currentSurface) {
            NavigationSurface.Auth -> authBackStack
            NavigationSurface.Main -> mainBackStacks.getValue(currentTab)
        }

    fun resetAuthStack(startRoute: NavRoute) {
        authBackStack.clear()
        authBackStack.add(startRoute)
        currentSurface = NavigationSurface.Auth
    }

    fun resetMainTabToRoot(tab: MainTabRoute) {
        mainBackStacks.getValue(tab).apply {
            clear()
            add(tab)
        }
        currentTab = tab
        currentSurface = NavigationSurface.Main
    }
}

@Composable
fun rememberNavigationState(
    authStartRoute: NavRoute,
    mainTabs: Set<MainTabRoute>,
    mainStartTab: MainTabRoute = VaultRoute,
): NavigationState = remember(mainTabs, mainStartTab) {
    NavigationState(
        authStartRoute = authStartRoute,
        mainTabs = mainTabs,
        mainStartTab = mainStartTab,
    )
}

