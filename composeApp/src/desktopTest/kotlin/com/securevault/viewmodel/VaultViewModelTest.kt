package com.securevault.viewmodel

import com.securevault.crypto.Argon2Kdf
import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordFilter
import com.securevault.data.PasswordRepository
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerResult
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VaultViewModelTest {

    @Test
    fun updateCategory_whenUnchanged_doesNotReloadEntries() = runBlocking {
        val repository = CountingPasswordRepository(sampleEntries())
        val viewModel = VaultViewModel(repository, unlockedKeyManager())

        viewModel.loadEntries()
        waitForSearchCount(repository, 2)

        viewModel.updateCategory("work")
        waitForSearchCount(repository, 4)
        val searchCountAfterFirstSelection = repository.searchCallCount

        viewModel.updateCategory("work")
        delay(150)

        assertEquals(searchCountAfterFirstSelection, repository.searchCallCount)
    }

    @Test
    fun updateFavoritesOnly_whenUnchanged_doesNotReloadEntries() = runBlocking {
        val repository = CountingPasswordRepository(sampleEntries())
        val viewModel = VaultViewModel(repository, unlockedKeyManager())

        viewModel.loadEntries()
        waitForSearchCount(repository, 2)

        viewModel.updateFavoritesOnly(true)
        waitForSearchCount(repository, 4)
        val searchCountAfterFirstToggle = repository.searchCallCount

        viewModel.updateFavoritesOnly(true)
        delay(150)

        assertEquals(searchCountAfterFirstToggle, repository.searchCallCount)
    }
}

private suspend fun waitForSearchCount(repository: CountingPasswordRepository, expectedCount: Int) {
    withTimeout(5_000) {
        while (repository.searchCallCount < expectedCount) {
            delay(10)
        }
    }
}

private fun unlockedKeyManager(): KeyManager {
    val keyManager = KeyManager(Argon2Kdf(), PlatformKeyStore(), SessionManager())
    val setupResult = runBlocking {
        keyManager.setupVault("vault-viewmodel-test-pass".toCharArray())
    }
    assertIs<KeyManagerResult.Success<Unit>>(setupResult)
    return keyManager
}

private fun sampleEntries(): List<PasswordEntry> {
    val now = 1_710_000_000_000L
    return listOf(
        PasswordEntry(
            id = 1L,
            title = "Work Mail",
            username = "dev@company.com",
            password = "secret-1",
            category = "work",
            createdAt = now,
            updatedAt = now,
        ),
        PasswordEntry(
            id = 2L,
            title = "Personal Bank",
            username = "me",
            password = "secret-2",
            category = "finance",
            isFavorite = true,
            createdAt = now,
            updatedAt = now,
        ),
    )
}

private class CountingPasswordRepository(
    private val entries: List<PasswordEntry>
) : PasswordRepository {
    var searchCallCount: Int = 0
        private set

    override suspend fun create(entry: PasswordEntry, dataKey: ByteArray): Long = error("Not needed in test")

    override suspend fun update(entry: PasswordEntry, dataKey: ByteArray): Boolean = error("Not needed in test")

    override suspend fun deleteById(id: Long) = error("Not needed in test")

    override suspend fun clear() = error("Not needed in test")

    override suspend fun getById(id: Long, dataKey: ByteArray): PasswordEntry? = entries.firstOrNull { it.id == id }

    override suspend fun getAll(dataKey: ByteArray): List<PasswordEntry> = entries

    override suspend fun search(query: String, filter: PasswordFilter, dataKey: ByteArray): List<PasswordEntry> {
        searchCallCount += 1
        return entries
            .asSequence()
            .filter { entry ->
                query.isBlank() || entry.title.contains(query, ignoreCase = true) || entry.username.contains(query, ignoreCase = true)
            }
            .filter { entry -> filter.category == null || entry.category == filter.category }
            .filter { entry -> !filter.onlyFavorites || entry.isFavorite }
            .toList()
    }
}
