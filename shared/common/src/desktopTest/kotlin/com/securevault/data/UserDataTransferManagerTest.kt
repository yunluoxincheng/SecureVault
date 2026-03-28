package com.securevault.data

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.securevault.crypto.Argon2Kdf
import com.securevault.db.SecureVaultDatabase
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerResult
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SessionManager
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserDataTransferManagerTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    private val database = createDatabase(driver)
    private val configRepository = ConfigRepositoryImpl(database)
    private val keyManager = KeyManager(Argon2Kdf(), PlatformKeyStore(), SessionManager(), configRepository)
    private val userDataTransferManager = UserDataTransferManager(configRepository, Argon2Kdf())

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun exportThenImportUserData_allowsUnlockAfterClear() {
        runBlocking {
            val password = "Master#Pass2026"
            assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault(password.toCharArray()))

            val exported = userDataTransferManager.export(password.toCharArray()).getOrThrow()

            keyManager.clear()

            userDataTransferManager.import(exported, password.toCharArray()).getOrThrow()

            val unlockResult = keyManager.unlockWithPassword(password.toCharArray())
            assertIs<KeyManagerResult.Success<Unit>>(unlockResult)
        }
    }

    @Test
    fun importUserData_withUtf8BomAndTrailingWhitespace_succeeds() {
        runBlocking {
            val password = "Master#Pass2026"
            assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault(password.toCharArray()))

            val exported = userDataTransferManager.export(password.toCharArray()).getOrThrow()

            keyManager.clear()

            val withBomAndPadding = "\uFEFF$exported\n  "
            userDataTransferManager.import(withBomAndPadding, password.toCharArray()).getOrThrow()

            val unlockResult = keyManager.unlockWithPassword(password.toCharArray())
            assertIs<KeyManagerResult.Success<Unit>>(unlockResult)
        }
    }

    @Test
    fun importUserData_withWrongPassword_fails() {
        runBlocking {
            val password = "Master#Pass2026"
            assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault(password.toCharArray()))

            val exported = userDataTransferManager.export(password.toCharArray()).getOrThrow()

            keyManager.clear()

            val importResult = userDataTransferManager.import(exported, "Wrong#Password".toCharArray())
            assertTrue(importResult.isFailure)
        }
    }

    private fun createDatabase(driver: JdbcSqliteDriver): SecureVaultDatabase {
        runBlocking {
            SecureVaultDatabase.Schema.create(driver).await()
        }
        return SecureVaultDatabase(driver)
    }
}
