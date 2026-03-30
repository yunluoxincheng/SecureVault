package com.securevault.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous
import com.securevault.db.SecureVaultDatabase

actual fun createSqlDriver(): SqlDriver {
    val driver = NativeSqliteDriver(SecureVaultDatabase.Schema.synchronous(), "securevault.db")
    runCatching { driver.execute(null, "PRAGMA journal_mode=WAL;", 0) }
    return driver
}
