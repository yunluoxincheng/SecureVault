package com.securevault.data

interface VaultFileGateway {
    suspend fun pickExportTarget(suggestedFileName: String): Result<String>

    suspend fun writeText(target: String, content: String): Result<Unit>

    suspend fun pickImportSource(): Result<String>

    suspend fun readText(source: String): Result<String>
}
