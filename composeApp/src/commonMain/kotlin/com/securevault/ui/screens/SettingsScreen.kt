package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.SettingsSwitchRow
import com.securevault.ui.components.SvDangerButton
import com.securevault.ui.components.SvFilledCard
import com.securevault.ui.components.SvTopBar
import com.securevault.ui.theme.ThemeMode
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xl),
    ) {
        item {
            SvTopBar(title = "设置", onBack = onBack)
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
            SvFilledCard(
                modifier = Modifier.fillMaxWidth().animateItemEntrance(1),
            ) {
                Column {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MaterialTheme.spacing.sm,
                                    vertical = MaterialTheme.spacing.xs,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = { onThemeChange(mode) },
                            )
                            Text(
                                text = when (mode) {
                                    ThemeMode.System -> "跟随系统"
                                    ThemeMode.Light -> "浅色模式"
                                    ThemeMode.Dark -> "深色模式"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
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
            SvFilledCard(
                modifier = Modifier.fillMaxWidth().animateItemEntrance(3),
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
            SvDangerButton(
                text = "立即锁定",
                onClick = onLock,
                modifier = Modifier.fillMaxWidth().animateItemEntrance(4),
                leadingIcon = Icons.Default.Lock,
            )
        }
    }
}
