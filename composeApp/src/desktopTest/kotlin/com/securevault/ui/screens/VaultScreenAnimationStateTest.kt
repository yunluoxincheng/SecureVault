package com.securevault.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun animationResetKey_staysSame_whenFiltersChangeWithinVault() {
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

        assertEquals(allEntries, favoriteEntries)
        assertEquals(allEntries, workEntries)
    }

    @Test
    fun shouldAnimateVaultListEntrance_onlyBeforeFirstPlayInVisit() {
        assertTrue(
            shouldAnimateVaultListEntrance(
                hasPlayedEntranceInVisit = false,
                isLoading = false,
                hasEntries = true,
            )
        )
        assertFalse(
            shouldAnimateVaultListEntrance(
                hasPlayedEntranceInVisit = true,
                isLoading = false,
                hasEntries = true,
            )
        )
    }

    @Test
    fun shouldAnimateVaultListEntrance_falseWhileLoadingOrEmpty() {
        assertFalse(
            shouldAnimateVaultListEntrance(
                hasPlayedEntranceInVisit = false,
                isLoading = true,
                hasEntries = true,
            )
        )
        assertFalse(
            shouldAnimateVaultListEntrance(
                hasPlayedEntranceInVisit = false,
                isLoading = false,
                hasEntries = false,
            )
        )
    }

    @Test
    fun shouldShowVaultLoadingSkeleton_true_onlyDuringFirstLoad() {
        assertTrue(
            shouldShowVaultLoadingSkeleton(
                isLoading = true,
                entries = emptyList(),
                hasLoadedAtLeastOnce = false,
            )
        )
    }

    @Test
    fun shouldShowVaultLoadingSkeleton_false_afterInitialLoadEvenIfListTemporarilyEmpty() {
        assertFalse(
            shouldShowVaultLoadingSkeleton(
                isLoading = true,
                entries = emptyList(),
                hasLoadedAtLeastOnce = true,
            )
        )
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
