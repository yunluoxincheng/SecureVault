package com.securevault.data

import com.securevault.db.SecureVaultDatabase

class ConfigRepositoryImpl(
    private val database: SecureVaultDatabase
) : ConfigRepository {

    override suspend fun get(key: String): String? {
        return database.secureVaultQueries.getConfig(key).executeAsOneOrNull()
    }

    override suspend fun set(key: String, value: String) {
        database.secureVaultQueries.setConfig(key, value)
    }

    override suspend fun delete(key: String) {
        database.secureVaultQueries.deleteConfig(key)
    }
}
