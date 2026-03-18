package com.securevault.ui.navigation

import androidx.navigation.NavController

fun NavController.navigateToLoginAfterLock(loginRoute: String, vaultRoute: String) {
    navigate(loginRoute) {
        popUpTo(vaultRoute) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateToLoginFromRegister(loginRoute: String, registerRoute: String) {
    navigate(loginRoute) {
        popUpTo(registerRoute) { inclusive = true }
        launchSingleTop = true
    }
}
