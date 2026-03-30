package com.securevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.async.coroutines.await
import com.securevault.crypto.Argon2Kdf
import com.securevault.db.SecureVaultDatabase
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerResult
import com.securevault.security.PlatformKeyStore
import com.securevault.security.SecureClipboard
import com.securevault.security.SecurityModeManager
import com.securevault.security.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExportImportManagerTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    private val database = createDatabase(driver)
    private val configRepository = ConfigRepositoryImpl(database)
    private val securityModeManager = SecurityModeManager(configRepository, SecureClipboard())
    private val passwordRepository = PasswordRepositoryImpl(database, securityModeManager)
    private val keyManager = KeyManager(Argon2Kdf(), PlatformKeyStore(), SessionManager(), configRepository, passwordRepository)
    private val exportManager = ExportManager(passwordRepository, keyManager)
    private val importManager = ImportManager(passwordRepository, keyManager)

    private val json = Json { ignoreUnknownKeys = true }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun secureExport_thenImport_roundtrip() = runBlocking {
        assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault("roundtrip-pass".toCharArray()))

        seedEntries()

        val exported = exportManager.export(VaultExportMode.SecureMode).getOrThrow()
        passwordRepository.clear()

        val importResult = importManager.import(exported, VaultImportConflictStrategy.Skip).getOrThrow()
        val dataKey = keyManager.getDataKey() ?: error("dataKey should not be null")
        val restored = passwordRepository.getAll(dataKey)

        assertEquals(2, importResult.totalInFile)
        assertEquals(2, importResult.imported)
        assertEquals(0, importResult.skippedDuplicates)
        assertEquals(0, importResult.overwritten)
        assertEquals(2, restored.size)
        assertTrue(restored.any { it.securityMode })
    }

    @Test
    fun import_withSkipConflict_keepsExistingEntry() = runBlocking {
        assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault("skip-pass".toCharArray()))

        val now = 1_000L
        val dataKey = keyManager.getDataKey() ?: error("dataKey should not be null")
        passwordRepository.create(
            PasswordEntry(
                title = "GitHub",
                username = "alice",
                password = "old-password",
                url = "https://github.com",
                createdAt = now,
                updatedAt = now,
            ),
            dataKey,
        )

        val envelope = plainEnvelopeWithSingleEntry(
            VaultExportEntry(
                title = "GitHub",
                username = "alice",
                password = "new-password",
                url = "https://github.com",
                createdAt = now + 1,
                updatedAt = now + 1,
            ),
        )

        val importResult = importManager.import(envelope, VaultImportConflictStrategy.Skip).getOrThrow()
        val allEntries = passwordRepository.getAll(dataKey)

        assertEquals(1, importResult.totalInFile)
        assertEquals(0, importResult.imported)
        assertEquals(1, importResult.skippedDuplicates)
        assertEquals(0, importResult.overwritten)
        assertEquals("old-password", allEntries.first().password)
    }

    @Test
    fun import_withOverwriteConflict_updatesExistingEntry() = runBlocking {
        assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault("overwrite-pass".toCharArray()))

        val now = 2_000L
        val dataKey = keyManager.getDataKey() ?: error("dataKey should not be null")
        passwordRepository.create(
            PasswordEntry(
                title = "GitHub",
                username = "alice",
                password = "old-password",
                url = "https://github.com",
                createdAt = now,
                updatedAt = now,
            ),
            dataKey,
        )

        val envelope = plainEnvelopeWithSingleEntry(
            VaultExportEntry(
                title = "GitHub",
                username = "alice",
                password = "new-password",
                url = "https://github.com",
                createdAt = now + 1,
                updatedAt = now + 1,
            ),
        )

        val importResult = importManager.import(envelope, VaultImportConflictStrategy.Overwrite).getOrThrow()
        val allEntries = passwordRepository.getAll(dataKey)

        assertEquals(1, importResult.totalInFile)
        assertEquals(0, importResult.imported)
        assertEquals(0, importResult.skippedDuplicates)
        assertEquals(1, importResult.overwritten)
        assertEquals("new-password", allEntries.first().password)
    }

    @Test
    fun import_midBatchFailure_rollBacksEntireBatch() = runBlocking {
        assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault("rollback-pass".toCharArray()))
        val dataKey = keyManager.getDataKey() ?: error("dataKey")
        val envelope = plainEnvelopeWithEntries(
            listOf(
                VaultExportEntry(
                    title = "A",
                    username = "a",
                    password = "1",
                    url = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
                VaultExportEntry(
                    title = "B",
                    username = "b",
                    password = "2",
                    url = null,
                    createdAt = 2L,
                    updatedAt = 2L,
                ),
                VaultExportEntry(
                    title = "C",
                    username = "c",
                    password = "3",
                    url = null,
                    createdAt = 3L,
                    updatedAt = 3L,
                ),
            ),
        )
        val failingRepo = FailOnNthCreateRepository(passwordRepository, failOnCreateCall = 2)
        val failingImport = ImportManager(failingRepo, keyManager, null)
        assertTrue(failingImport.import(envelope, VaultImportConflictStrategy.Skip).isFailure)
        assertEquals(0, passwordRepository.getAll(dataKey).size)
    }

    @Test
    fun export_whenLocked_returnsFailure() = runBlocking {
        assertIs<KeyManagerResult.Success<Unit>>(keyManager.setupVault("lock-pass".toCharArray()))
        keyManager.lock()

        val result = exportManager.export(VaultExportMode.Encrypted)

        assertTrue(result.isFailure)
    }

    private suspend fun seedEntries() {
        val now = 1_710_000_000_000L
        val dataKey = keyManager.getDataKey() ?: error("dataKey should not be null")

        passwordRepository.create(
            PasswordEntry(
                title = "Email",
                username = "dev@securevault.com",
                password = "email-password",
                category = "work",
                isFavorite = true,
                securityMode = false,
                createdAt = now,
                updatedAt = now,
            ),
            dataKey,
        )

        passwordRepository.create(
            PasswordEntry(
                title = "Bank",
                username = "alice",
                password = "bank-password",
                category = "finance",
                securityMode = true,
                createdAt = now + 1,
                updatedAt = now + 1,
            ),
            dataKey,
        )
    }

    private fun plainEnvelopeWithEntries(entries: List<VaultExportEntry>): String {
        val payload = VaultExportPayload(entries = entries)
        val envelope = VaultExportEnvelope(
            version = VAULT_EXPORT_FORMAT_VERSION,
            type = VaultExportType.Plain.toRawValue(),
            createdAt = System.currentTimeMillis(),
            entryCount = entries.size,
            plainData = json.encodeToString(payload),
        )
        return json.encodeToString(envelope)
    }

    private fun plainEnvelopeWithSingleEntry(entry: VaultExportEntry): String {
        return plainEnvelopeWithEntries(listOf(entry))
    }

    private fun createDatabase(driver: JdbcSqliteDriver): SecureVaultDatabase {
        runBlocking {
            SecureVaultDatabase.Schema.create(driver).await()
        }
        return SecureVaultDatabase(driver)
    }
}

private class FailOnNthCreateRepository(
    private val inner: PasswordRepository,
    private val failOnCreateCall: Int,
) : PasswordRepository {
    private var createCalls = 0

    override fun invalidateDecryptCache() = inner.invalidateDecryptCache()

    override suspend fun <T> runInTransaction(block: suspend PasswordRepository.() -> T): T {
        return inner.runInTransaction { block(this@FailOnNthCreateRepository) }
    }

    override suspend fun create(entry: PasswordEntry, dataKey: ByteArray): Long {
        createCalls++
        if (createCalls == failOnCreateCall) error("simulated import failure")
        return inner.create(entry, dataKey)
    }

    override suspend fun update(entry: PasswordEntry, dataKey: ByteArray): Boolean =
        inner.update(entry, dataKey)

    override suspend fun deleteById(id: Long) = inner.deleteById(id)

    override suspend fun clear() = inner.clear()

    override suspend fun getById(id: Long, dataKey: ByteArray): PasswordEntry? =
        inner.getById(id, dataKey)

    override suspend fun getPasswordCipherById(id: Long): PasswordCipherPayload? =
        inner.getPasswordCipherById(id)

    override suspend fun getAll(dataKey: ByteArray): List<PasswordEntry> = inner.getAll(dataKey)

    override suspend fun search(query: String, filter: PasswordFilter, dataKey: ByteArray): List<PasswordEntry> =
        inner.search(query, filter, dataKey)
}
