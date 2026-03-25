package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.securevault.data.VaultExportMode
import com.securevault.data.VaultImportConflictStrategy
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppDropdownOption
import com.securevault.ui.components.MyAppDropdownSelector
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun VaultSettingsScreen(
    exportMode: VaultExportMode,
    importConflictStrategy: VaultImportConflictStrategy,
    isExporting: Boolean,
    isImporting: Boolean,
    isExportingUserData: Boolean,
    onExportModeChange: (VaultExportMode) -> Unit,
    onImportConflictStrategyChange: (VaultImportConflictStrategy) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportUserDataClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var showExportUserDataDialog by remember { mutableStateOf(false) }
    var masterPassword by remember { mutableStateOf("") }

    if (showExportUserDataDialog) {
        AlertDialog(
            onDismissRequest = {
                masterPassword = ""
                showExportUserDataDialog = false
            },
            title = {
                Text(
                    text = "导出用户数据",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    Text(
                        text = "请输入主密码以加密导出用户数据。该数据可在新设备导入后恢复加密密码库解密能力。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MyAppInput(
                        value = masterPassword,
                        onValueChange = { masterPassword = it },
                        label = "主密码",
                        isPassword = true,
                        enabled = !isExportingUserData,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExportUserDataClick(masterPassword)
                        masterPassword = ""
                        showExportUserDataDialog = false
                    },
                    enabled = masterPassword.isNotBlank() && !isExportingUserData,
                ) {
                    Text("导出")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        masterPassword = ""
                        showExportUserDataDialog = false
                    },
                    enabled = !isExportingUserData,
                ) {
                    Text("取消")
                }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.compactContentSpacing),
            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xl),
        ) {
            item {
                MyAppTopBar(title = "密码库", onBack = onBack)
            }

            item {
                MyAppCard(
                    modifier = Modifier.fillMaxWidth().animateItemEntrance(0),
                    variant = MyAppCardVariant.Filled,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        Text(
                            text = "导出",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        MyAppDropdownSelector(
                            label = "导出模式",
                            selectedText = exportMode.displayLabel(),
                            options = VaultExportMode.entries.map { mode ->
                                MyAppDropdownOption(mode, mode.displayLabel())
                            },
                            onSelect = onExportModeChange,
                            supportingText = "点击选择",
                        )

                        MyAppButton(
                            text = "导出密码库",
                            onClick = onExportClick,
                            variant = MyAppButtonVariant.Primary,
                            isLoading = isExporting,
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                MyAppCard(
                    modifier = Modifier.fillMaxWidth().animateItemEntrance(1),
                    variant = MyAppCardVariant.Filled,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        Text(
                            text = "导入",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        MyAppDropdownSelector(
                            label = "重复记录处理",
                            selectedText = importConflictStrategy.displayLabel(),
                            options = VaultImportConflictStrategy.entries.map { strategy ->
                                MyAppDropdownOption(strategy, strategy.displayLabel())
                            },
                            onSelect = onImportConflictStrategyChange,
                            supportingText = "点击选择",
                        )

                        MyAppButton(
                            text = "导入密码库",
                            onClick = onImportClick,
                            variant = MyAppButtonVariant.Secondary,
                            isLoading = isImporting,
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = "安全模式导出采用 ExportKey 二次封装，导入时仅在已解锁会话内可解密。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                MyAppCard(
                    modifier = Modifier.fillMaxWidth().animateItemEntrance(2),
                    variant = MyAppCardVariant.Filled,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        Text(
                            text = "用户数据迁移",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            text = "导出包含密钥恢复信息的用户数据文件。新设备在登录/创建页面导入并输入主密码后，即可解密加密导出的密码库。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        MyAppButton(
                            text = "导出用户数据",
                            onClick = { showExportUserDataDialog = true },
                            variant = MyAppButtonVariant.Secondary,
                            isLoading = isExportingUserData,
                            enabled = !isExporting && !isImporting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

private fun VaultExportMode.displayLabel(): String {
    return when (this) {
        VaultExportMode.Plaintext -> "普通导出（明文）"
        VaultExportMode.Encrypted -> "普通导出（加密）"
        VaultExportMode.SecureMode -> "安全模式导出"
    }
}

private fun VaultImportConflictStrategy.displayLabel(): String {
    return when (this) {
        VaultImportConflictStrategy.Skip -> "跳过重复记录"
        VaultImportConflictStrategy.Overwrite -> "覆盖重复记录"
    }
}
