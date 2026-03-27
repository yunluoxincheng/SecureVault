package com.securevault.autofill

import java.util.Locale

object AutofillAppIdentity {
    private const val APP_URL_SCHEME = "androidapp://"

    fun normalizePackageName(rawPackageName: String): String {
        return rawPackageName
            .trim()
            .lowercase(Locale.US)
            .substringBefore(':')
    }

    fun appUrlForPackage(packageName: String): String {
        val normalized = normalizePackageName(packageName)
        return "$APP_URL_SCHEME$normalized"
    }

    fun extractPackageFromUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val value = rawUrl.trim().lowercase(Locale.US)
        if (!value.startsWith(APP_URL_SCHEME)) return null
        return value.removePrefix(APP_URL_SCHEME).substringBefore('/').ifBlank { null }
    }

    fun inferTitle(webDomain: String?, packageName: String): String {
        val domain = webDomain?.trim()?.ifBlank { null }
        if (domain != null) return domain

        val normalizedPackage = normalizePackageName(packageName)
        val segment = normalizedPackage.substringAfterLast('.').ifBlank { normalizedPackage }
        val words = segment
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return if (words.isEmpty()) {
            normalizedPackage
        } else {
            words.joinToString(" ") { word ->
                word.replaceFirstChar { c -> c.titlecase(Locale.US) }
            }
        }
    }
}
