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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.SettingsSwitchRow
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun AutofillSettingsScreen(
    onBack: (() -> Unit)? = null,
) {
    var autofillServiceEnabled by rememberSaveable { mutableStateOf(false) }
    var askToAddMissingPasswordOnLogin by rememberSaveable { mutableStateOf(true) }

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
                            description = "当前为 UI 占位，暂未接入系统自动填充能力",
                            checked = autofillServiceEnabled,
                            onCheckedChange = { autofillServiceEnabled = it },
                        )
                    }
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 1),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        SettingsSwitchRow(
                            label = "登录时询问保存不存在的密码",
                            description = "当识别到新凭据时提示添加到密码库",
                            checked = askToAddMissingPasswordOnLogin,
                            onCheckedChange = { askToAddMissingPasswordOnLogin = it },
                        )
                    }
                }
            }

            item {
                Text(
                    text = "说明：本页仅实现界面，功能将在后续版本接入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.xs)
                        .animateItemEntrance(index = 2),
                )
            }
        }
    }
}