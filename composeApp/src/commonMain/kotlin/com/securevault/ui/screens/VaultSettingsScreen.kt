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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun VaultSettingsScreen(
    exportMode: VaultExportMode,
    importConflictStrategy: VaultImportConflictStrategy,
    isExporting: Boolean,
    isImporting: Boolean,
    onExportModeChange: (VaultExportMode) -> Unit,
    onImportConflictStrategyChange: (VaultImportConflictStrategy) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
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
