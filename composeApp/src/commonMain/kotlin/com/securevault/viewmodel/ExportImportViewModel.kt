package com.securevault.viewmodel

import com.securevault.data.ExportManager
import com.securevault.data.ImportManager
import com.securevault.data.UserDataTransferManager
import com.securevault.data.VaultExportMode
import com.securevault.data.VaultFileGateway
import com.securevault.data.VaultImportConflictStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExportImportUiState(
    val exportMode: VaultExportMode = VaultExportMode.SecureMode,
    val importConflictStrategy: VaultImportConflictStrategy = VaultImportConflictStrategy.Skip,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isExportingUserData: Boolean = false,
    val message: String? = null,
)

class ExportImportViewModel(
    private val exportManager: ExportManager,
    private val importManager: ImportManager,
    private val vaultFileGateway: VaultFileGateway,
    private val userDataTransferManager: UserDataTransferManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(ExportImportUiState())
    val uiState: StateFlow<ExportImportUiState> = _uiState.asStateFlow()

    fun updateExportMode(mode: VaultExportMode) {
        _uiState.update { it.copy(exportMode = mode) }
    }

    fun updateImportConflictStrategy(strategy: VaultImportConflictStrategy) {
        _uiState.update { it.copy(importConflictStrategy = strategy) }
    }

    fun exportVault() {
        if (_uiState.value.isExporting) return

        scope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }

            val mode = _uiState.value.exportMode
            val fileName = suggestedFileName(mode)

            val result = runCatching {
                val exportContent = exportManager.export(mode).getOrThrow()
                val target = vaultFileGateway.pickExportTarget(fileName).getOrThrow()
                vaultFileGateway.writeText(target, exportContent).getOrThrow()
                "导出成功"
            }

            _uiState.update {
                it.copy(
                    isExporting = false,
                    message = result.getOrElse { error ->
                        "导出失败：${error.message ?: "未知错误"}"
                    },
                )
            }
        }
    }

    fun importVault() {
        if (_uiState.value.isImporting) return

        scope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }

            val strategy = _uiState.value.importConflictStrategy
            val result = runCatching {
                val source = vaultFileGateway.pickImportSource().getOrThrow()
                val content = vaultFileGateway.readText(source).getOrThrow()
                val importResult = importManager.import(content, strategy).getOrThrow()
                "导入完成：新增 ${importResult.imported} 条，覆盖 ${importResult.overwritten} 条，跳过 ${importResult.skippedDuplicates} 条"
            }

            _uiState.update {
                it.copy(
                    isImporting = false,
                    message = result.getOrElse { error ->
                        "导入失败：${error.message ?: "未知错误"}"
                    },
                )
            }
        }
    }

    fun exportUserData(masterPassword: String) {
        if (_uiState.value.isExportingUserData) return

        scope.launch {
            _uiState.update { it.copy(isExportingUserData = true, message = null) }

            val result = runCatching {
                val exported = userDataTransferManager
                    .export(masterPassword.toCharArray())
                    .getOrThrow()
                val target = vaultFileGateway
                    .pickExportTarget("securevault_user_data_${System.currentTimeMillis()}.svu")
                    .getOrThrow()
                vaultFileGateway.writeText(target, exported).getOrThrow()
                "用户数据导出成功"
            }

            _uiState.update {
                it.copy(
                    isExportingUserData = false,
                    message = result.getOrElse { error ->
                        "用户数据导出失败：${error.message ?: "未知错误"}"
                    },
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun suggestedFileName(mode: VaultExportMode): String {
        val timestamp = System.currentTimeMillis()
        val suffix = when (mode) {
            VaultExportMode.Plaintext -> "plain"
            VaultExportMode.Encrypted -> "encrypted"
            VaultExportMode.SecureMode -> "secure"
        }
        return "securevault_${suffix}_$timestamp.svx"
    }
}
