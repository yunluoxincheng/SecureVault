package com.securevault.data

data class PasswordEntry(
    val id: Long? = null,
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
    val updatedAt: Long
)

data class PasswordFilter(
    val category: String? = null,
    val onlyFavorites: Boolean = false,
    val onlySecurityMode: Boolean = false
)

const val DEFAULT_CATEGORY = "default"
