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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppListItem
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.SettingsSwitchRow
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing
import com.securevault.viewmodel.AutofillSettingsUiState

@Composable
fun AutofillSettingsScreen(
    uiState: AutofillSettingsUiState,
    onAutofillEnabledChange: (Boolean) -> Unit,
    onAskToSaveChange: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
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
                MyAppTopBar(title = "自动填充", onBack = onBack)
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 0),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        SettingsSwitchRow(
                            label = "启用自动填充服务",
                            description = if (uiState.serviceEnabledInSystem) {
                                "已在系统中启用 SecureVault 自动填充服务"
                            } else {
                                "开启后会跳转系统设置，将 SecureVault 设为自动填充服务"
                            },
                            checked = uiState.servicePreferenceEnabled,
                            onCheckedChange = onAutofillEnabledChange,
                            enabled = uiState.serviceSupported,
                        )
                    }
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 1),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        SettingsSwitchRow(
                            label = "登录时询问保存不存在的密码",
                            description = "当识别到新凭据时提示添加到密码库",
                            checked = uiState.askToSaveOnLogin,
                            onCheckedChange = onAskToSaveChange,
                            enabled = uiState.servicePreferenceEnabled,
                        )
                    }
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 2),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        MyAppListItem(
                            headline = "打开系统自动填充设置",
                            supportingText = "用于启用/停用 SecureVault 的系统自动填充权限",
                            onClick = onOpenSystemSettings,
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "打开系统自动填充设置",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }

            item {
                Text(
                    text = uiState.infoMessage ?: "提示：系统开关关闭时，输入框仍不会出现填充建议。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.xs)
                        .animateItemEntrance(index = 3),
                )
            }
        }
    }
}