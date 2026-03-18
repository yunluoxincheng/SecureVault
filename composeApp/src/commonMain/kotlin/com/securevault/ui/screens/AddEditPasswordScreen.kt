package com.securevault.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.PasswordStrengthBar

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
    var showPassword by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(if (entry == null) "添加密码" else "编辑密码", style = MaterialTheme.typography.headlineSmall)
            }

            OutlinedTextField(
                title,
                { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                username,
                { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("用户名") },
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                password,
                { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                        )
                    }
                }
            )
            Button(
                onClick = {
                    val generated = onGeneratePassword()
                    if (generated.isNotBlank()) {
                        password = generated
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("生成强密码")
            }
            PasswordStrengthBar(password = password, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                url,
                { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL") },
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                notes,
                { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                category,
                { category = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分类") },
                shape = RoundedCornerShape(16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("收藏", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isFavorite, onCheckedChange = { isFavorite = it })
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("安全模式", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = securityMode, onCheckedChange = { securityMode = it })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

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
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            enabled = canSave,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("保存")
        }
    }
}
