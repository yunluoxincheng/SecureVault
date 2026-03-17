package com.securevault.data

interface PasswordRepository {
    suspend fun create(entry: PasswordEntry, dataKey: ByteArray): Long

    suspend fun update(entry: PasswordEntry, dataKey: ByteArray): Boolean

    suspend fun deleteById(id: Long)

    suspend fun clear()

    suspend fun getById(id: Long, dataKey: ByteArray): PasswordEntry?

    suspend fun getAll(dataKey: ByteArray): List<PasswordEntry>

    suspend fun search(query: String, filter: PasswordFilter, dataKey: ByteArray): List<PasswordEntry>
}
