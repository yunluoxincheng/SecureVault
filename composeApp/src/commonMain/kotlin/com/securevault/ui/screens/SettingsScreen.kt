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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppListItem
import com.securevault.ui.components.SettingsSwitchRow
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.theme.ThemeMode
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    biometricEnabled: Boolean,
    screenshotAllowed: Boolean,
    errorMessage: String?,
    onThemeChange: (ThemeMode) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onScreenshotAllowedChange: (Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    onLock: () -> Unit
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
                MyAppTopBar(title = "设置", onBack = onBack)
            }

        item {
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacing.xs)
                    .animateItemEntrance(0),
            )
        }

        item {
            MyAppCard(
                modifier = Modifier.fillMaxWidth().animateItemEntrance(1),
                variant = MyAppCardVariant.Filled,
            ) {
                Column {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        MyAppListItem(
                            headline = when (mode) {
                                ThemeMode.System -> "跟随系统"
                                ThemeMode.Light -> "浅色模式"
                                ThemeMode.Dark -> "深色模式"
                            },
                            onClick = { onThemeChange(mode) },
                            trailing = {
                                RadioButton(
                                    selected = currentTheme == mode,
                                    onClick = { onThemeChange(mode) },
                                )
                            },
                        )
                        if (index < ThemeMode.entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "安全",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacing.xs)
                    .animateItemEntrance(2),
            )
        }

        item {
            MyAppCard(
                modifier = Modifier.fillMaxWidth().animateItemEntrance(3),
                variant = MyAppCardVariant.Filled,
            ) {
                Column {
                    SettingsSwitchRow(
                        label = "生物识别解锁",
                        description = "使用指纹或面容 ID 快速解锁",
                        checked = biometricEnabled,
                        onCheckedChange = onBiometricChange,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingsSwitchRow(
                        label = "允许应用内截图",
                        description = "关闭可防止密码出现在截图或应用切换界面",
                        checked = screenshotAllowed,
                        onCheckedChange = onScreenshotAllowedChange,
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            item {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs),
                )
            }
        }

            item {
                MyAppButton(
                    text = "立即锁定",
                    onClick = onLock,
                    modifier = Modifier.fillMaxWidth().animateItemEntrance(4),
                    leadingIcon = Icons.Default.Lock,
                    variant = MyAppButtonVariant.Danger,
                )
            }
        }
    }
}
