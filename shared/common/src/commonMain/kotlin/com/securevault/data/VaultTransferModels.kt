package com.securevault.data

import kotlinx.serialization.Serializable

const val VAULT_EXPORT_FORMAT_VERSION = 1

enum class VaultExportMode {
    Plaintext,
    Encrypted,
    SecureMode,
}

enum class VaultImportConflictStrategy {
    Skip,
    Overwrite,
}

enum class VaultExportType {
    Plain,
    Encrypted,
    Secure,
}

data class VaultImportResult(
    val totalInFile: Int,
    val imported: Int,
    val skippedDuplicates: Int,
    val overwritten: Int,
)

@Serializable
data class VaultExportEnvelope(
    val version: Int,
    val type: String,
    val createdAt: Long,
    val entryCount: Int,
    val encryptedKey: String? = null,
    val encryptedData: String? = null,
    val plainData: String? = null,
)

@Serializable
data class VaultExportPayload(
    val entries: List<VaultExportEntry>,
)

@Serializable
data class VaultExportEntry(
    val title: String,
    val username: String,
    val password: String,
    val url: String? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val category: String = DEFAULT_CATEGORY,
    val isFavorite: Boolean = false,
    val securityMode: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

internal fun VaultExportEntry.toPasswordEntry(
    id: Long? = null,
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt,
): PasswordEntry {
    return PasswordEntry(
        id = id,
        title = title,
        username = username,
        password = password,
        url = url,
        notes = notes,
        tags = tags,
        category = category,
        isFavorite = isFavorite,
        securityMode = securityMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun PasswordEntry.toExportEntry(): VaultExportEntry {
    return VaultExportEntry(
        title = title,
        username = username,
        password = password,
        url = url,
        notes = notes,
        tags = tags,
        category = category,
        isFavorite = isFavorite,
        securityMode = securityMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun VaultExportType.toRawValue(): String {
    return when (this) {
        VaultExportType.Plain -> "plain_export"
        VaultExportType.Encrypted -> "encrypted_export"
        VaultExportType.Secure -> "secure_export"
    }
}

internal fun String.toVaultExportTypeOrNull(): VaultExportType? {
    return when (this) {
        "plain_export" -> VaultExportType.Plain
        "encrypted_export" -> VaultExportType.Encrypted
        "secure_export" -> VaultExportType.Secure
        else -> null
    }
}
