package com.securevault.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous
import com.securevault.db.SecureVaultDatabase

actual fun createSqlDriver(): SqlDriver {
    return NativeSqliteDriver(SecureVaultDatabase.Schema.synchronous(), "securevault.db")
}
