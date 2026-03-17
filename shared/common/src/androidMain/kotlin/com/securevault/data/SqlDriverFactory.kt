package com.securevault.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.async.coroutines.synchronous
import com.securevault.db.SecureVaultDatabase
import com.securevault.security.AppContextHolder

actual fun createSqlDriver(): SqlDriver {
    val context = AppContextHolder.get() ?: error("Android context 不可用，无法创建数据库驱动")
    return AndroidSqliteDriver(SecureVaultDatabase.Schema.synchronous(), context, "securevault.db")
}
