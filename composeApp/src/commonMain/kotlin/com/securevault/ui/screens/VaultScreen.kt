package com.securevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun VaultScreen(
    entries: List<PasswordEntry>,
    onEntryClick: (PasswordEntry) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredEntries = entries.filter {
        if (query.isBlank()) return@filter true
        val text = listOf(it.title, it.username, it.url.orEmpty(), it.category).joinToString(" ")
        text.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("密码库") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredEntries, key = { it.id ?: it.hashCode().toLong() }) { entry ->
                    PasswordCard(entry = entry, onClick = { onEntryClick(entry) })
                }
            }
        }
    }
}

@Composable
private fun PasswordCard(
    entry: PasswordEntry,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                Text(text = entry.username, style = MaterialTheme.typography.bodyMedium)
                val url = entry.url
                if (!url.isNullOrBlank()) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (entry.securityMode) {
                Card {
                    Text(
                        text = "安全模式",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
