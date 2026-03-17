package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    entry: PasswordEntry,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("密码详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (entry.securityMode) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        text = "安全模式：密码不可见，仅可使用",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            DetailRow(title = "标题", value = entry.title)
            DetailRow(title = "用户名", value = entry.username, showCopy = true)

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("密码", style = MaterialTheme.typography.bodySmall)
                        val displayValue = if (entry.securityMode || !showPassword) "••••••••" else entry.password
                        Text(displayValue, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (!entry.securityMode) {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "显示/隐藏"
                            )
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ContentCopy, contentDescription = if (entry.securityMode) "使用" else "复制")
                    }
                }
            }

            val url = entry.url
            if (!url.isNullOrBlank()) {
                DetailRow(title = "URL", value = url)
            }
            val notes = entry.notes
            if (!notes.isNullOrBlank()) {
                DetailRow(title = "备注", value = notes)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "分类：${entry.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DetailRow(
    title: String,
    value: String,
    showCopy: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodySmall)
                Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (showCopy) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                }
            }
        }
    }
}
