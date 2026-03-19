package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.SettingsSwitchRow
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun SecurityModeScreen(
    enabled: Boolean,
    isLoading: Boolean,
    showPasswordVerificationDialog: Boolean = false,
    message: String?,
    onEnabledChange: (Boolean) -> Unit,
    onConfirmDisableWithPassword: (String) -> Unit = {},
    onDismissPasswordDialog: () -> Unit = {},
    onBack: () -> Unit,
) {
    var masterPassword by remember { mutableStateOf("") }

    if (showPasswordVerificationDialog) {
        AlertDialog(
            onDismissRequest = {
                masterPassword = ""
                onDismissPasswordDialog()
            },
            title = {
                Text(
                    text = "验证主密码",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    Text(
                        text = "关闭全局安全模式前，请输入主密码进行验证",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MyAppInput(
                        value = masterPassword,
                        onValueChange = { masterPassword = it },
                        label = "主密码",
                        isPassword = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmDisableWithPassword(masterPassword)
                        masterPassword = ""
                    },
                    enabled = masterPassword.isNotBlank() && !isLoading,
                ) {
                    Text("验证")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        masterPassword = ""
                        onDismissPasswordDialog()
                    },
                    enabled = !isLoading,
                ) {
                    Text("取消")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = MaterialTheme.layout.pageMaxWidth)
            .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.compactContentSpacing),
    ) {
        item {
            MyAppTopBar(
                title = "安全模式",
                onBack = onBack,
            )
        }

        item {
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Filled,
            ) {
                SettingsSwitchRow(
                    label = "开启安全模式",
                    description = "开启后，安全模式条目的密码在 UI 中始终隐藏，仅可使用",
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !isLoading,
                )
            }
        }

        item {
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Filled,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "• 安全模式条目在详情页不会显示明文密码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "• 点击“使用”后，密码将直接复制到剪贴板并在 30 秒后清除",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "• 安全模式条目使用独立 SecureModeKey 进行密码字段加密",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!message.isNullOrBlank()) {
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs),
                )
            }
        }
    }
}
