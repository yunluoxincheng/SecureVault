package com.securevault.data

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopVaultFileGateway : VaultFileGateway {
    override suspend fun pickExportTarget(suggestedFileName: String): Result<String> {
        return runCatching {
            val chooser = JFileChooser().apply {
                dialogTitle = "选择导出文件"
                selectedFile = File(suggestedFileName)
                fileFilter = FileNameExtensionFilter("SecureVault 导出文件 (*.svx, *.json)", "svx", "json")
            }
            val result = chooser.showSaveDialog(null)
            if (result != JFileChooser.APPROVE_OPTION) {
                error("未选择导出文件")
            }
            chooser.selectedFile.absolutePath
        }
    }

    override suspend fun writeText(target: String, content: String): Result<Unit> {
        return runCatching {
            File(target).writeText(content)
        }
    }

    override suspend fun pickImportSource(): Result<String> {
        return runCatching {
            val chooser = JFileChooser().apply {
                dialogTitle = "选择导入文件"
                fileFilter = FileNameExtensionFilter("SecureVault 导出文件 (*.svx, *.json)", "svx", "json")
            }
            val result = chooser.showOpenDialog(null)
            if (result != JFileChooser.APPROVE_OPTION) {
                error("未选择导入文件")
            }
            chooser.selectedFile.absolutePath
        }
    }

    override suspend fun readText(source: String): Result<String> {
        return runCatching {
            File(source).readText()
        }
    }
}
