package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppListItem
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.icons.LOGO_SCREEN_SCALE
import com.securevault.ui.icons.SecureVaultLogoIcon
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun AboutScreen(
    appVersion: String,
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
                MyAppTopBar(title = "关于页面", onBack = onBack)
            }

            item {
                MyAppCard(
                    modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 0),
                    variant = MyAppCardVariant.Filled,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        SecureVaultLogoIcon(
                            contentDescription = "应用图标",
                            modifier = Modifier.size(MaterialTheme.layout.heroIconSize * LOGO_SCREEN_SCALE),
                        )
                        Text(
                            text = "SecureVault",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "版本 $appVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 1),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        MyAppListItem(
                            headline = "用户协议",
                            supportingText = "查看使用条款",
                        )
                    }
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 2),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        MyAppListItem(
                            headline = "隐私政策",
                            supportingText = "查看数据与隐私说明",
                        )
                    }
                    MyAppCard(
                        modifier = Modifier.fillMaxWidth().animateItemEntrance(index = 3),
                        variant = MyAppCardVariant.Filled,
                    ) {
                        MyAppListItem(
                            headline = "开源许可",
                            supportingText = "查看第三方开源组件许可",
                        )
                    }
                }
            }
        }
    }
}