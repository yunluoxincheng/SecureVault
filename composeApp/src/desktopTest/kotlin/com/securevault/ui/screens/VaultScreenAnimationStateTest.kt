package com.securevault.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VaultScreenAnimationStateTest {

    @Test
    fun animationResetKey_changes_whenVaultIsReentered() {
        val firstVisit = vaultListAnimationResetKey(
            vaultVisitNonce = 1,
            selectedCategory = null,
            favoritesOnly = false,
        )
        val secondVisit = vaultListAnimationResetKey(
            vaultVisitNonce = 2,
            selectedCategory = null,
            favoritesOnly = false,
        )

        assertNotEquals(firstVisit, secondVisit)
    }

    @Test
    fun animationResetKey_changes_whenFiltersChange() {
        val allEntries = vaultListAnimationResetKey(
            vaultVisitNonce = 2,
            selectedCategory = null,
            favoritesOnly = false,
        )
        val favoriteEntries = vaultListAnimationResetKey(
            vaultVisitNonce = 2,
            selectedCategory = null,
            favoritesOnly = true,
        )
        val workEntries = vaultListAnimationResetKey(
            vaultVisitNonce = 2,
            selectedCategory = "work",
            favoritesOnly = false,
        )

        assertNotEquals(allEntries, favoriteEntries)
        assertNotEquals(allEntries, workEntries)
    }

    @Test
    fun animationResetKey_staysSame_whenInputsStaySame() {
        val first = vaultListAnimationResetKey(
            vaultVisitNonce = 3,
            selectedCategory = "work",
            favoritesOnly = true,
        )
        val second = vaultListAnimationResetKey(
            vaultVisitNonce = 3,
            selectedCategory = "work",
            favoritesOnly = true,
        )

        assertEquals(first, second)
    }
}
