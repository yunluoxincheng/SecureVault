package com.securevault.autofill

import com.securevault.data.PasswordRepository

class SaveDetector(
    private val repository: PasswordRepository,
) {
    enum class ActionType {
        SaveNew,
        UpdateExisting,
    }

    data class SaveAction(
        val type: ActionType,
        val existingEntryId: Long?,
    )

    suspend fun detect(
        candidate: SaveCandidate,
        dataKey: ByteArray,
    ): SaveAction? {
        if (candidate.password.isBlank()) return null
        val entries = repository.getAll(dataKey)
        val normalizedDomain = normalizeDomain(candidate.webDomain)
        val existing = entries.firstOrNull { entry ->
            val usernameMatches = entry.username.equals(candidate.username, ignoreCase = true)
            if (!usernameMatches) return@firstOrNull false

            if (normalizedDomain == null) {
                entry.url.orEmpty().contains(candidate.packageName, ignoreCase = true)
            } else {
                val entryDomain = normalizeDomain(entry.url)
                entryDomain != null && rootDomain(entryDomain) == rootDomain(normalizedDomain)
            }
        } ?: return SaveAction(type = ActionType.SaveNew, existingEntryId = null)

        return if (existing.password != candidate.password) {
            SaveAction(type = ActionType.UpdateExisting, existingEntryId = existing.id)
        } else {
            null
        }
    }

    private fun normalizeDomain(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore(":")
            .trim()
        return value.takeIf { it.isNotBlank() }
    }

    private fun rootDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) {
            "${parts[parts.lastIndex - 1]}.${parts.last()}"
        } else {
            domain
        }
    }
}
