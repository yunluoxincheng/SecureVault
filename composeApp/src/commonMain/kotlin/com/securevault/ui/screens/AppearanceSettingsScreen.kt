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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppDropdownOption
import com.securevault.ui.components.MyAppDropdownSelector
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.SettingsSwitchRow
import com.securevault.ui.theme.ThemeMode
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing
import com.securevault.viewmodel.AppLanguage

@Composable
fun AppearanceSettingsScreen(
    currentTheme: ThemeMode,
    currentLanguage: AppLanguage,
    dynamicColorEnabled: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
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
                MyAppTopBar(title = "外观", onBack = onBack)
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
                        MyAppDropdownSelector(
                            label = "主题",
                            selectedText = currentTheme.displayLabel(),
                            options = ThemeMode.entries.map { mode ->
                                MyAppDropdownOption(mode, mode.displayLabel())
                            },
                            onSelect = onThemeChange,
                            supportingText = "点击选择",
                        )
                    }
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 1),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        MyAppDropdownSelector(
                            label = "语言",
                            selectedText = currentLanguage.displayLabel(),
                            options = AppLanguage.entries.map { language ->
                                MyAppDropdownOption(language, language.displayLabel())
                            },
                            onSelect = onLanguageChange,
                            supportingText = "点击选择",
                        )
                    }
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 2),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        SettingsSwitchRow(
                            label = "动态颜色",
                            description = "开启后优先跟随系统动态配色",
                            checked = dynamicColorEnabled,
                            onCheckedChange = onDynamicColorChange,
                        )
                    }
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

private fun AppLanguage.displayLabel(): String {
    return when (this) {
        AppLanguage.System -> "跟随系统"
        AppLanguage.ZhCN -> "简体中文"
        AppLanguage.EnUS -> "English"
    }
}