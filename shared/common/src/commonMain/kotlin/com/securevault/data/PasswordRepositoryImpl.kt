package com.securevault.data

import com.securevault.crypto.AesGcmCipher
import com.securevault.crypto.CryptoUtils
import com.securevault.crypto.EncryptedData
import com.securevault.db.Password_entries
import com.securevault.db.SecureVaultDatabase
import com.securevault.security.SecurityModeManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PasswordRepositoryImpl(
    private val database: SecureVaultDatabase,
    private val securityModeManager: SecurityModeManager? = null,
) : PasswordRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun create(entry: PasswordEntry, dataKey: ByteArray): Long {
        val encryptedTitle = encryptField(entry.title, dataKey)
        val encryptedUsername = encryptField(entry.username, dataKey)
        val encryptedPassword = encryptPassword(entry.password, dataKey, entry.securityMode)
        val encryptedUrl = encryptNullableField(entry.url, dataKey)
        val encryptedNotes = encryptNullableField(entry.notes, dataKey)
        val encryptedTags = encryptField(json.encodeToString(entry.tags), dataKey)
        val iv = extractIvBase64(encryptedTitle)

        database.secureVaultQueries.insertEntry(
            encrypted_title = encryptedTitle,
            encrypted_username = encryptedUsername,
            encrypted_password = encryptedPassword,
            encrypted_url = encryptedUrl,
            encrypted_notes = encryptedNotes,
            encrypted_tags = encryptedTags,
            iv = iv,
            category = entry.category,
            is_favorite = entry.isFavorite.toDbBoolean(),
            security_mode = entry.securityMode.toDbBoolean(),
            created_at = entry.createdAt,
            updated_at = entry.updatedAt
        )

        return database.secureVaultQueries.lastInsertedId().executeAsOne()
    }

    override suspend fun update(entry: PasswordEntry, dataKey: ByteArray): Boolean {
        val id = entry.id ?: return false
        val existing = database.secureVaultQueries.selectById(id).executeAsOneOrNull() ?: return false

        val encryptedTitle = encryptField(entry.title, dataKey)
        val encryptedUsername = encryptField(entry.username, dataKey)
        val encryptedPassword = encryptPassword(entry.password, dataKey, entry.securityMode)
        val encryptedUrl = encryptNullableField(entry.url, dataKey)
        val encryptedNotes = encryptNullableField(entry.notes, dataKey)
        val encryptedTags = encryptField(json.encodeToString(entry.tags), dataKey)
        val iv = extractIvBase64(encryptedTitle)

        database.secureVaultQueries.updateEntry(
            encrypted_title = encryptedTitle,
            encrypted_username = encryptedUsername,
            encrypted_password = encryptedPassword,
            encrypted_url = encryptedUrl,
            encrypted_notes = encryptedNotes,
            encrypted_tags = encryptedTags,
            iv = iv,
            category = entry.category,
            is_favorite = entry.isFavorite.toDbBoolean(),
            security_mode = entry.securityMode.toDbBoolean(),
            updated_at = entry.updatedAt,
            id = existing.id
        )

        return true
    }

    override suspend fun deleteById(id: Long) {
        database.secureVaultQueries.deleteEntry(id)
    }

    override suspend fun clear() {
        database.secureVaultQueries.deleteAllEntries()
    }

    override suspend fun getById(id: Long, dataKey: ByteArray): PasswordEntry? {
        val row = database.secureVaultQueries.selectById(id).executeAsOneOrNull() ?: return null
        return row.toDomain(dataKey)
    }

    override suspend fun getPasswordCipherById(id: Long): PasswordCipherPayload? {
        val row = database.secureVaultQueries.selectById(id).executeAsOneOrNull() ?: return null
        return PasswordCipherPayload(
            encryptedPassword = row.encrypted_password,
            securityMode = row.security_mode == 1L,
        )
    }

    override suspend fun getAll(dataKey: ByteArray): List<PasswordEntry> {
        val rows = database.secureVaultQueries.selectAll().executeAsList()
        val entries = ArrayList<PasswordEntry>(rows.size)
        for (row in rows) {
            entries.add(row.toDomain(dataKey))
        }
        return entries
    }

    override suspend fun search(query: String, filter: PasswordFilter, dataKey: ByteArray): List<PasswordEntry> {
        val normalizedQuery = query.trim().lowercase()
        val rows = when {
            filter.category != null -> database.secureVaultQueries.selectByCategory(filter.category).executeAsList()
            filter.onlyFavorites && !filter.onlySecurityMode -> database.secureVaultQueries.selectFavorites().executeAsList()
            filter.onlySecurityMode && !filter.onlyFavorites -> database.secureVaultQueries.selectSecurityMode().executeAsList()
            else -> database.secureVaultQueries.selectAll().executeAsList()
        }

        val result = mutableListOf<PasswordEntry>()
        for (row in rows) {
            val entry = row.toDomain(dataKey)

            if (filter.category != null && entry.category != filter.category) {
                continue
            }
            if (filter.onlyFavorites && !entry.isFavorite) {
                continue
            }
            if (filter.onlySecurityMode && !entry.securityMode) {
                continue
            }
            if (normalizedQuery.isBlank()) {
                result.add(entry)
                continue
            }

            val searchableText = listOf(
                entry.title,
                entry.username,
                entry.url.orEmpty(),
                entry.notes.orEmpty(),
                entry.tags.joinToString(" ")
            ).joinToString(" ").lowercase()

            if (searchableText.contains(normalizedQuery)) {
                result.add(entry)
            }
        }

        return result
    }

    private suspend fun Password_entries.toDomain(dataKey: ByteArray): PasswordEntry {
        return PasswordEntry(
            id = id,
            title = decryptField(encrypted_title, dataKey),
            username = decryptField(encrypted_username, dataKey),
            password = decryptPassword(encrypted_password, dataKey, security_mode == 1L),
            url = decryptNullableField(encrypted_url, dataKey),
            notes = decryptNullableField(encrypted_notes, dataKey),
            tags = decryptTags(encrypted_tags, dataKey),
            category = category,
            isFavorite = is_favorite == 1L,
            securityMode = security_mode == 1L,
            createdAt = created_at,
            updatedAt = updated_at
        )
    }

    private suspend fun encryptField(value: String, dataKey: ByteArray): String {
        return AesGcmCipher.encryptToStorageFormat(value.encodeToByteArray(), dataKey)
    }

    private suspend fun encryptNullableField(value: String?, dataKey: ByteArray): String? {
        if (value == null) return null
        return encryptField(value, dataKey)
    }

    private suspend fun encryptPassword(value: String, dataKey: ByteArray, securityMode: Boolean): String {
        val manager = securityModeManager
        return if (manager != null) {
            manager.encryptPasswordForStorage(
                plaintextPassword = value,
                dataKey = dataKey,
                securityMode = securityMode,
            )
        } else {
            encryptField(value, dataKey)
        }
    }

    private suspend fun decryptField(value: String, dataKey: ByteArray): String {
        return AesGcmCipher.decryptFromStorageFormat(value, dataKey).decodeToString()
    }

    private suspend fun decryptNullableField(value: String?, dataKey: ByteArray): String? {
        if (value == null) return null
        return decryptField(value, dataKey)
    }

    private suspend fun decryptPassword(value: String, dataKey: ByteArray, securityMode: Boolean): String {
        val manager = securityModeManager
        return if (manager != null) {
            manager.decryptPasswordForRead(
                encryptedPassword = value,
                dataKey = dataKey,
                securityMode = securityMode,
            )
        } else {
            decryptField(value, dataKey)
        }
    }

    private suspend fun decryptTags(value: String?, dataKey: ByteArray): List<String> {
        if (value == null) return emptyList()

        val decoded = decryptField(value, dataKey)
        return runCatching { json.decodeFromString<List<String>>(decoded) }
            .getOrElse { emptyList() }
    }

    private fun extractIvBase64(storageFormat: String): String {
        return CryptoUtils.encodeBase64(EncryptedData.fromStorageFormat(storageFormat).iv)
    }
}

private fun Boolean.toDbBoolean(): Long = if (this) 1L else 0L
