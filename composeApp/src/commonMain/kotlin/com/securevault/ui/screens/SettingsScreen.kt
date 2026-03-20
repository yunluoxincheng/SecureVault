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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppListItem
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun SettingsScreen(
    onOpenAccountSecuritySettings: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onOpenAutofillSettings: () -> Unit,
    onOpenVaultSettings: () -> Unit,
    onOpenAboutSettings: () -> Unit,
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
                MyAppTopBar(title = "设置", onBack = onBack)
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    SettingEntryItem(
                        title = "账户安全",
                        description = "控制安全和会话相关设置",
                        onClick = onOpenAccountSecuritySettings,
                        contentDescription = "进入账户安全设置",
                        animationIndex = 0,
                    )
                    SettingEntryItem(
                        title = "外观",
                        description = "控制主题跟随、语言和动态颜色",
                        onClick = onOpenAppearanceSettings,
                        contentDescription = "进入外观设置",
                        animationIndex = 1,
                    )
                    SettingEntryItem(
                        title = "自动填充",
                        description = "管理自动填充相关偏好",
                        onClick = onOpenAutofillSettings,
                        contentDescription = "进入自动填充设置",
                        animationIndex = 2,
                    )
                    SettingEntryItem(
                        title = "密码库",
                        description = "管理密码库相关偏好",
                        onClick = onOpenVaultSettings,
                        contentDescription = "进入密码库设置",
                        animationIndex = 3,
                    )
                    SettingEntryItem(
                        title = "关于页面",
                        description = "查看应用版本和相关协议",
                        onClick = onOpenAboutSettings,
                        contentDescription = "进入关于页面",
                        animationIndex = 4,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingEntryItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    contentDescription: String,
    animationIndex: Int,
) {
    MyAppCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateItemEntrance(index = animationIndex),
        variant = MyAppCardVariant.Filled,
    ) {
        MyAppListItem(
            headline = title,
            supportingText = description,
            onClick = onClick,
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
