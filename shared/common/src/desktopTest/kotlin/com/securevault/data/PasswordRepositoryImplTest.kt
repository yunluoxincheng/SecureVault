package com.securevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.securevault.db.SecureVaultDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasswordRepositoryImplTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    private val database = createDatabase(driver)
    private val repository = PasswordRepositoryImpl(database)
    private val dataKey = ByteArray(32) { (it + 1).toByte() }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun create_andGetById_roundtrip() = runBlocking {
        val now = 1_710_000_000_000L
        val id = repository.create(
            entry = PasswordEntry(
                title = "GitHub",
                username = "alice",
                password = "p@ssw0rd",
                url = "https://github.com",
                notes = "work",
                tags = listOf("dev", "code"),
                category = "work",
                isFavorite = true,
                securityMode = false,
                createdAt = now,
                updatedAt = now
            ),
            dataKey = dataKey
        )

        val saved = repository.getById(id, dataKey)

        assertNotNull(saved)
        assertEquals("GitHub", saved.title)
        assertEquals("alice", saved.username)
        assertEquals("p@ssw0rd", saved.password)
        assertEquals(listOf("dev", "code"), saved.tags)
        assertEquals("work", saved.category)
        assertTrue(saved.isFavorite)
    }

    @Test
    fun create_storesEncryptedFields() = runBlocking {
        val id = repository.create(
            entry = PasswordEntry(
                title = "Bank",
                username = "bob",
                password = "123456",
                createdAt = 100L,
                updatedAt = 100L
            ),
            dataKey = dataKey
        )

        val row = database.secureVaultQueries.selectById(id).executeAsOneOrNull()

        assertNotNull(row)
        assertNotEquals("Bank", row.encrypted_title)
        assertNotEquals("bob", row.encrypted_username)
        assertNotEquals("123456", row.encrypted_password)
    }

    @Test
    fun update_existingEntry_succeeds() = runBlocking {
        val id = repository.create(
            entry = PasswordEntry(
                title = "Mail",
                username = "u1",
                password = "old",
                createdAt = 100L,
                updatedAt = 100L
            ),
            dataKey = dataKey
        )

        val updated = repository.update(
            PasswordEntry(
                id = id,
                title = "Mail Updated",
                username = "u2",
                password = "new",
                tags = listOf("personal"),
                category = "personal",
                isFavorite = true,
                securityMode = true,
                createdAt = 100L,
                updatedAt = 200L
            ),
            dataKey
        )

        val reloaded = repository.getById(id, dataKey)

        assertTrue(updated)
        assertNotNull(reloaded)
        assertEquals("Mail Updated", reloaded.title)
        assertEquals("u2", reloaded.username)
        assertEquals("new", reloaded.password)
        assertEquals(listOf("personal"), reloaded.tags)
        assertEquals("personal", reloaded.category)
        assertTrue(reloaded.isFavorite)
        assertTrue(reloaded.securityMode)
    }

    @Test
    fun search_withFilter_works() = runBlocking {
        val base = 1_000L
        repository.create(
            PasswordEntry(
                title = "GitLab",
                username = "alice",
                password = "aaa",
                category = "work",
                isFavorite = true,
                securityMode = false,
                createdAt = base,
                updatedAt = base
            ),
            dataKey
        )
        repository.create(
            PasswordEntry(
                title = "Personal Mail",
                username = "me",
                password = "bbb",
                category = "personal",
                isFavorite = false,
                securityMode = true,
                createdAt = base + 1,
                updatedAt = base + 1
            ),
            dataKey
        )

        val result1 = repository.search(
            query = "git",
            filter = PasswordFilter(category = "work", onlyFavorites = true),
            dataKey = dataKey
        )
        val result2 = repository.search(
            query = "mail",
            filter = PasswordFilter(onlySecurityMode = true),
            dataKey = dataKey
        )

        assertEquals(1, result1.size)
        assertEquals("GitLab", result1.first().title)
        assertEquals(1, result2.size)
        assertEquals("Personal Mail", result2.first().title)
    }

    @Test
    fun deleteById_thenGet_returnsNull() = runBlocking {
        val id = repository.create(
            PasswordEntry(
                title = "To Delete",
                username = "x",
                password = "y",
                createdAt = 10L,
                updatedAt = 10L
            ),
            dataKey
        )

        repository.deleteById(id)

        val result = repository.getById(id, dataKey)
        assertNull(result)
    }

    @Test
    fun configRepository_setGetDelete() = runBlocking {
        val configRepository = ConfigRepositoryImpl(database)

        configRepository.set(VaultConfigKeys.Salt, "salt-b64")
        assertEquals("salt-b64", configRepository.get(VaultConfigKeys.Salt))

        configRepository.delete(VaultConfigKeys.Salt)
        assertNull(configRepository.get(VaultConfigKeys.Salt))
    }

    private fun createDatabase(driver: JdbcSqliteDriver): SecureVaultDatabase {
        runBlocking {
            SecureVaultDatabase.Schema.create(driver).await()
        }
        return SecureVaultDatabase(driver)
    }
}
