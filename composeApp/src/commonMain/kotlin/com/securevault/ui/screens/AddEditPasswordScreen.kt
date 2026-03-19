package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.components.MyAppListItemContainer
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.OptionSwitchRow
import com.securevault.ui.components.PasswordStrengthBar
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun AddEditPasswordScreen(
    entry: PasswordEntry?,
    onSave: (PasswordEntry) -> Unit,
    onCancel: () -> Unit,
    onGeneratePassword: () -> String
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("默认") }
    var isFavorite by remember { mutableStateOf(false) }
    var securityMode by remember { mutableStateOf(false) }

    LaunchedEffect(entry?.id, entry == null) {
        title = entry?.title.orEmpty()
        username = entry?.username.orEmpty()
        password = entry?.password.orEmpty()
        url = entry?.url.orEmpty()
        notes = entry?.notes.orEmpty()
        category = entry?.category ?: "默认"
        isFavorite = entry?.isFavorite ?: false
        securityMode = entry?.securityMode ?: false
    }

    val canSave = title.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    val scrollState = rememberScrollState()
    val isEditing = entry != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .align(Alignment.TopCenter)
                .imePadding()
                .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding)
                .verticalScroll(scrollState)
                .padding(bottom = MaterialTheme.layout.bottomBarActionInset),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.contentSpacing),
        ) {
            MyAppTopBar(
                title = if (isEditing) "编辑密码" else "添加密码",
                onBack = onCancel,
            )

            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs),
            )

            MyAppInput(
                value = title,
                onValueChange = { title = it },
                label = "标题 *",
                modifier = Modifier.fillMaxWidth(),
            )

            MyAppInput(
                value = username,
                onValueChange = { username = it },
                label = "用户名 *",
                modifier = Modifier.fillMaxWidth(),
            )

            MyAppInput(
                value = password,
                onValueChange = { password = it },
                label = "密码 *",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true,
            )

            MyAppButton(
                text = "生成强密码",
                onClick = {
                    val generated = onGeneratePassword()
                    if (generated.isNotBlank()) password = generated
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.AutoAwesome,
                variant = MyAppButtonVariant.Secondary,
            )

            PasswordStrengthBar(password = password, modifier = Modifier.fillMaxWidth())

            MyAppInput(
                value = url,
                onValueChange = { url = it },
                label = "URL（可选）",
                modifier = Modifier.fillMaxWidth(),
            )

            MyAppInput(
                value = notes,
                onValueChange = { notes = it },
                label = "备注（可选）",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
            )

            MyAppInput(
                value = category,
                onValueChange = { category = it },
                label = "分类",
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "选项",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.xs,
                    end = MaterialTheme.spacing.xs,
                    top = MaterialTheme.spacing.sm,
                ),
            )

            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Filled,
                contentPadding = PaddingValues(0.dp),
            ) {
                OptionSwitchRow(
                    label = "收藏",
                    checked = isFavorite,
                    onCheckedChange = { isFavorite = it },
                    container = MyAppListItemContainer.None,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                OptionSwitchRow(
                    label = "安全模式（密码不可见）",
                    checked = securityMode,
                    onCheckedChange = { securityMode = it },
                    container = MyAppListItemContainer.None,
                )
            }
        }

        MyAppButton(
            text = "保存",
            onClick = {
                val now = System.currentTimeMillis()
                onSave(
                    PasswordEntry(
                        id = entry?.id,
                        title = title,
                        username = username,
                        password = password,
                        url = url.ifBlank { null },
                        notes = notes.ifBlank { null },
                        category = category.ifBlank { "默认" },
                        isFavorite = isFavorite,
                        securityMode = securityMode,
                        tags = entry?.tags ?: emptyList(),
                        createdAt = entry?.createdAt ?: now,
                        updatedAt = now
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .align(Alignment.BottomCenter)
                .padding(
                    horizontal = MaterialTheme.layout.pageHorizontalPadding,
                    vertical = MaterialTheme.spacing.sm,
                ),
            enabled = canSave,
        )
    }
}
