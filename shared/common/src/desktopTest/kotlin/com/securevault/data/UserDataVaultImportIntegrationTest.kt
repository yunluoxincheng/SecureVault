package com.securevault.data

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.securevault.crypto.Argon2Kdf
import com.securevault.db.SecureVaultDatabase
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerResult
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SecureClipboard
import com.securevault.security.SecurityModeManager
import com.securevault.security.SessionManager
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserDataVaultImportIntegrationTest {

    private var oldDriver: JdbcSqliteDriver? = null
    private var newDriver: JdbcSqliteDriver? = null

    @AfterTest
    fun tearDown() {
        oldDriver?.close()
        newDriver?.close()
    }

    @Test
    fun importUserData_thenImportEncryptedVault_shouldSucceedAcrossInstall() {
        runBlocking {
            val password = "Master#Pass2026"

            val old = buildContext().also { oldDriver = it.driver }
            assertIs<KeyManagerResult.Success<Unit>>(old.keyManager.setupVault(password.toCharArray()))

            val oldDataKey = old.keyManager.getDataKey() ?: error("dataKey should not be null")
            old.passwordRepository.create(
                PasswordEntry(
                    title = "Mail",
                    username = "alice@example.com",
                    password = "P@ssw0rd",
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                ),
                oldDataKey,
            )

            val encryptedVault = old.exportManager.export(VaultExportMode.Encrypted).getOrThrow()
            val userData = old.userDataTransferManager.export(password.toCharArray()).getOrThrow()

            val fresh = buildContext().also { newDriver = it.driver }
            fresh.userDataTransferManager.import(userData, password.toCharArray()).getOrThrow()
            assertIs<KeyManagerResult.Success<Unit>>(fresh.keyManager.unlockWithPassword(password.toCharArray()))

            val importResult = fresh.importManager.import(
                encryptedVault,
                VaultImportConflictStrategy.Skip,
            ).getOrThrow()

            val freshDataKey = fresh.keyManager.getDataKey() ?: error("dataKey should not be null")
            val entries = fresh.passwordRepository.getAll(freshDataKey)

            assertEquals(1, importResult.imported)
            assertEquals(1, entries.size)
            assertEquals("Mail", entries.first().title)
        }
    }

    @Test
    fun importMismatchedUserData_thenImportEncryptedVault_shouldFailWithReadableMessage() {
        runBlocking {
            val passwordA = "Master#Pass-A"
            val passwordB = "Master#Pass-B"

            val sourceA = buildContext().also { oldDriver = it.driver }
            assertIs<KeyManagerResult.Success<Unit>>(sourceA.keyManager.setupVault(passwordA.toCharArray()))
            val sourceADataKey = sourceA.keyManager.getDataKey() ?: error("dataKey should not be null")
            sourceA.passwordRepository.create(
                PasswordEntry(
                    title = "Mail",
                    username = "alice@example.com",
                    password = "P@ssw0rd",
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                ),
                sourceADataKey,
            )
            val encryptedVaultFromA = sourceA.exportManager.export(VaultExportMode.Encrypted).getOrThrow()

            val sourceB = buildContext().also { newDriver = it.driver }
            assertIs<KeyManagerResult.Success<Unit>>(sourceB.keyManager.setupVault(passwordB.toCharArray()))
            val userDataFromB = sourceB.userDataTransferManager.export(passwordB.toCharArray()).getOrThrow()

            val fresh = buildContext()
            fresh.userDataTransferManager.import(userDataFromB, passwordB.toCharArray()).getOrThrow()
            assertIs<KeyManagerResult.Success<Unit>>(fresh.keyManager.unlockWithPassword(passwordB.toCharArray()))

            val importResult = fresh.importManager.import(
                encryptedVaultFromA,
                VaultImportConflictStrategy.Skip,
            )

            kotlin.test.assertTrue(importResult.isFailure)
            val message = importResult.exceptionOrNull()?.message.orEmpty()
            kotlin.test.assertTrue(message.contains("不匹配"))
        }
    }

    private fun buildContext(): TestContext {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        runBlocking {
            SecureVaultDatabase.Schema.create(driver).await()
        }
        val database = SecureVaultDatabase(driver)
        val configRepository = ConfigRepositoryImpl(database)
        val securityModeManager = SecurityModeManager(configRepository, SecureClipboard())
        val passwordRepository = PasswordRepositoryImpl(database, securityModeManager)
        val keyManager = KeyManager(Argon2Kdf(), PlatformKeyStore(), SessionManager(), configRepository, passwordRepository)
        val exportManager = ExportManager(passwordRepository, keyManager, configRepository)
        val importManager = ImportManager(passwordRepository, keyManager, configRepository)
        val userDataTransferManager = UserDataTransferManager(configRepository, Argon2Kdf())
        return TestContext(
            driver = driver,
            keyManager = keyManager,
            passwordRepository = passwordRepository,
            exportManager = exportManager,
            importManager = importManager,
            userDataTransferManager = userDataTransferManager,
        )
    }

    private data class TestContext(
        val driver: JdbcSqliteDriver,
        val keyManager: KeyManager,
        val passwordRepository: PasswordRepository,
        val exportManager: ExportManager,
        val importManager: ImportManager,
        val userDataTransferManager: UserDataTransferManager,
    )
}
