package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.PasswordStrengthBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPasswordScreen(
    entry: PasswordEntry?,
    onSave: (PasswordEntry) -> Unit,
    onCancel: () -> Unit,
    onGeneratePassword: () -> String
) {
    var title by remember { mutableStateOf(entry?.title.orEmpty()) }
    var username by remember { mutableStateOf(entry?.username.orEmpty()) }
    var password by remember { mutableStateOf(entry?.password.orEmpty()) }
    var url by remember { mutableStateOf(entry?.url.orEmpty()) }
    var notes by remember { mutableStateOf(entry?.notes.orEmpty()) }
    var category by remember { mutableStateOf(entry?.category ?: "默认") }
    var isFavorite by remember { mutableStateOf(entry?.isFavorite ?: false) }
    var securityMode by remember { mutableStateOf(entry?.securityMode ?: false) }

    val canSave = title.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entry == null) "添加密码" else "编辑密码") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("标题") })
            OutlinedTextField(username, { username = it }, modifier = Modifier.fillMaxWidth(), label = { Text("用户名") })
            OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("密码") })
            Button(
                onClick = {
                    val generated = onGeneratePassword()
                    if (generated.isNotBlank()) {
                        password = generated
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("生成强密码")
            }
            PasswordStrengthBar(password = password, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(url, { url = it }, modifier = Modifier.fillMaxWidth(), label = { Text("URL") })
            OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注") })
            OutlinedTextField(category, { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("分类") })

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("收藏", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isFavorite, onCheckedChange = { isFavorite = it })
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("安全模式", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = securityMode, onCheckedChange = { securityMode = it })
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
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
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text("保存")
            }
        }
    }
}
