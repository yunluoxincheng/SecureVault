package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.unit.dp
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
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.OptionSwitchRow
import com.securevault.ui.components.PasswordStrengthBar
import com.securevault.ui.components.SvButton
import com.securevault.ui.components.SvOutlinedButton
import com.securevault.ui.components.SvPasswordTextField
import com.securevault.ui.components.SvTextField
import com.securevault.ui.components.SvTopBar
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
                .imePadding()
                .padding(horizontal = MaterialTheme.spacing.md)
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            SvTopBar(
                title = if (isEditing) "编辑密码" else "添加密码",
                onBack = onCancel,
            )

            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SvTextField(
                value = title,
                onValueChange = { title = it },
                label = "标题 *",
                modifier = Modifier.fillMaxWidth(),
            )

            SvTextField(
                value = username,
                onValueChange = { username = it },
                label = "用户名 *",
                modifier = Modifier.fillMaxWidth(),
            )

            SvPasswordTextField(
                value = password,
                onValueChange = { password = it },
                label = "密码 *",
                modifier = Modifier.fillMaxWidth(),
            )

            SvOutlinedButton(
                text = "生成强密码",
                onClick = {
                    val generated = onGeneratePassword()
                    if (generated.isNotBlank()) password = generated
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.AutoAwesome,
            )

            PasswordStrengthBar(password = password, modifier = Modifier.fillMaxWidth())

            SvTextField(
                value = url,
                onValueChange = { url = it },
                label = "URL（可选）",
                modifier = Modifier.fillMaxWidth(),
            )

            SvTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "备注（可选）",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
            )

            SvTextField(
                value = category,
                onValueChange = { category = it },
                label = "分类",
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "选项",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OptionSwitchRow(
                label = "收藏",
                checked = isFavorite,
                onCheckedChange = { isFavorite = it },
            )

            OptionSwitchRow(
                label = "安全模式（密码不可见）",
                checked = securityMode,
                onCheckedChange = { securityMode = it },
            )
        }

        SvButton(
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
                .align(Alignment.BottomCenter)
                .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
            enabled = canSave,
        )
    }
}
