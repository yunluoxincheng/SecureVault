package com.securevault.autofill

import com.securevault.data.PasswordRepository

class CredentialMatcher(
    private val repository: PasswordRepository,
) {
    suspend fun match(
        packageName: String,
        domain: String?,
        dataKey: ByteArray,
    ): List<MatchedCredential> {
        val entries = repository.getAll(dataKey)
        val normalizedDomain = normalizeDomain(domain)
        val normalizedPackage = AutofillAppIdentity.normalizePackageName(packageName)

        return entries
            .filter { entry ->
                val entryUrl = entry.url.orEmpty()
                when {
                    normalizedDomain != null -> {
                        val entryDomain = normalizeDomain(entryUrl)
                        entryDomain != null && rootDomain(entryDomain) == rootDomain(normalizedDomain)
                    }

                    else -> {
                        val fromAppUrl = AutofillAppIdentity.extractPackageFromUrl(entryUrl)
                        fromAppUrl == normalizedPackage ||
                            entryUrl.contains(normalizedPackage, ignoreCase = true) ||
                            entry.title.contains(normalizedPackage, ignoreCase = true)
                    }
                }
            }
            .mapNotNull { entry ->
                val id = entry.id ?: return@mapNotNull null
                MatchedCredential(
                    entryId = id,
                    title = entry.title,
                    username = entry.username,
                    password = entry.password,
                    securityMode = entry.securityMode,
                )
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
