package com.securevault.ui.screens

import com.securevault.data.PasswordEntry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultScreenLoadingBehaviorTest {

    @Test
    fun showLoadingSkeleton_whenInitialLoadHasNoEntries() {
        assertTrue(
            shouldShowVaultLoadingSkeleton(
                isLoading = true,
                entries = emptyList(),
                hasLoadedAtLeastOnce = false,
            )
        )
    }

    @Test
    fun hideLoadingSkeleton_whenRefreshingExistingEntries() {
        assertFalse(
            shouldShowVaultLoadingSkeleton(
                isLoading = true,
                entries = listOf(sampleEntry()),
                hasLoadedAtLeastOnce = true,
            )
        )
    }
}

private fun sampleEntry(): PasswordEntry {
    val now = 1_710_000_000_000L
    return PasswordEntry(
        id = 1L,
        title = "Example",
        username = "user",
        password = "secret",
        category = "work",
        createdAt = now,
        updatedAt = now,
    )
}
