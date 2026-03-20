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
import com.securevault.ui.components.MyAppDropdownOption
import com.securevault.ui.components.MyAppDropdownSelector
import com.securevault.ui.components.MyAppListItem
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.theme.ThemeMode
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onOpenSecuritySessionSettings: () -> Unit,
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
                MyAppDropdownSelector(
                    label = "主题模式",
                    selectedText = currentTheme.displayLabel(),
                    options = ThemeMode.entries.map { mode ->
                        MyAppDropdownOption(mode, mode.displayLabel())
                    },
                    onSelect = onThemeChange,
                    supportingText = "点击选择",
                )
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
                MyAppListItem(
                    headline = "安全与会话",
                    supportingText = "管理开关、会话超时与立即锁定",
                    onClick = onOpenSecuritySessionSettings,
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "进入安全与会话设置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
        }
    }
}

private fun ThemeMode.displayLabel(): String {
    return when (this) {
        ThemeMode.System -> "跟随系统"
        ThemeMode.Light -> "浅色模式"
        ThemeMode.Dark -> "深色模式"
    }
}
