package com.securevault.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.async.coroutines.await
import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordRepositoryImpl
import com.securevault.db.SecureVaultDatabase
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerResult
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SessionManager
import com.securevault.crypto.Argon2Kdf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class Week7ViewModelIntegrationTest {

    @Test
    fun setup_add_lock_unlock_read_roundtrip() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SecureVaultDatabase.Schema.create(driver).await()
        val database = SecureVaultDatabase(driver)
        val repository = PasswordRepositoryImpl(database)

        val keyManager = KeyManager(Argon2Kdf(), PlatformKeyStore(), SessionManager())
        val setup = keyManager.setupVault("week7-master-pass".toCharArray())
        assertIs<KeyManagerResult.Success<Unit>>(setup)

        val firstDataKey = keyManager.getDataKey()
        assertNotNull(firstDataKey)

        val now = 1_710_000_000_000L
        val id = repository.create(
            PasswordEntry(
                title = "Week7",
                username = "dev",
                password = "P@ssw0rd!",
                url = "https://example.com",
                category = "work",
                createdAt = now,
                updatedAt = now
            ),
            firstDataKey
        )

        keyManager.lock()

        val unlock = keyManager.unlockWithPassword("week7-master-pass".toCharArray())
        assertIs<KeyManagerResult.Success<Unit>>(unlock)
        val secondDataKey = keyManager.getDataKey()
        assertNotNull(secondDataKey)

        val saved = repository.getById(id, secondDataKey)
        assertNotNull(saved)
        assertEquals("Week7", saved.title)
        assertEquals("dev", saved.username)
    }
}
