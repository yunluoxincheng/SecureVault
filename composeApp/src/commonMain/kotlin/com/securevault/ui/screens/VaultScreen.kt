package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.PasswordCard
import com.securevault.ui.components.SkeletonList
import com.securevault.ui.components.SvTextField
import com.securevault.ui.theme.spacing

@Composable
fun VaultScreen(
    entries: List<PasswordEntry>,
    categories: List<String>,
    selectedCategory: String?,
    favoritesOnly: Boolean,
    query: String,
    isLoading: Boolean = false,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onEntryClick: (PasswordEntry) -> Unit,
    onAddClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.md),
        ) {
            Text(
                text = "密码库",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = MaterialTheme.spacing.md, bottom = MaterialTheme.spacing.sm),
            )

            SvTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "搜索",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Search,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                contentPadding = PaddingValues(vertical = MaterialTheme.spacing.sm),
            ) {
                item {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { onFavoritesOnlyChange(!favoritesOnly) },
                        label = { Text("仅收藏") },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == null && !favoritesOnly,
                        onClick = {
                            onCategoryChange(null)
                            if (favoritesOnly) onFavoritesOnlyChange(false)
                        },
                        label = { Text("全部") },
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category) },
                    )
                }
            }

            if (isLoading) {
                SkeletonList(
                    count = 6,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            } else if (entries.isEmpty()) {
                VaultEmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    contentPadding = PaddingValues(
                        top = MaterialTheme.spacing.xs,
                        bottom = 88.dp,
                    ),
                ) {
                    itemsIndexed(entries, key = { _, e -> e.id ?: e.hashCode().toLong() }) { index, entry ->
                        PasswordCard(
                            entry = entry,
                            onClick = { onEntryClick(entry) },
                            index = index,
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.spacing.md),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加密码")
        }
    }
}

@Composable
private fun VaultEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "暂无密码",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
        )
        Text(
            text = "点击右下角 + 添加第一个密码",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
        )
    }
}
