package com.securevault.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.async.coroutines.await
import com.securevault.db.SecureVaultDatabase
import kotlinx.coroutines.runBlocking

actual fun createSqlDriver(): SqlDriver {
    val driver = JdbcSqliteDriver("jdbc:sqlite:securevault.db")
    runBlocking {
        SecureVaultDatabase.Schema.create(driver).await()
    }
    return driver
}
