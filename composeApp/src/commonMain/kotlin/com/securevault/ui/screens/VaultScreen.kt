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
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.PasswordCard
import com.securevault.ui.components.SkeletonList
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun VaultScreen(
    entries: List<PasswordEntry>,
    categories: List<String>,
    selectedCategory: String?,
    favoritesOnly: Boolean,
    query: String,
    vaultVisitNonce: Int = 0,
    isLoading: Boolean = false,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onEntryClick: (PasswordEntry) -> Unit,
    onAddClick: () -> Unit
) {
    val animationResetKey = vaultListAnimationResetKey(
        vaultVisitNonce = vaultVisitNonce,
        selectedCategory = selectedCategory,
        favoritesOnly = favoritesOnly,
    )
    val showLoadingSkeleton = shouldShowVaultLoadingSkeleton(
        isLoading = isLoading,
        entries = entries,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .align(Alignment.TopCenter)
                .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.contentSpacing),
        ) {
            MyAppTopBar(title = "密码库")

            MyAppInput(
                value = query,
                onValueChange = onQueryChange,
                label = "搜索",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Search,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                contentPadding = PaddingValues(vertical = MaterialTheme.spacing.xs),
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

            if (showLoadingSkeleton) {
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
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.compactContentSpacing),
                    contentPadding = PaddingValues(
                        top = MaterialTheme.spacing.xs,
                        bottom = MaterialTheme.layout.fabContentClearance,
                    ),
                ) {
                    itemsIndexed(entries, key = { _, e -> vaultEntryKey(e) }) { index, entry ->
                        PasswordCard(
                            entry = entry,
                            onClick = { onEntryClick(entry) },
                            index = index,
                            animateEntrance = true,
                            animationResetKey = animationResetKey,
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
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = MaterialTheme.layout.fabElevation,
                pressedElevation = MaterialTheme.layout.fabPressedElevation,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加密码")
        }
    }
}

internal fun vaultListAnimationResetKey(
    vaultVisitNonce: Int,
    selectedCategory: String?,
    favoritesOnly: Boolean,
): String {
    return "$vaultVisitNonce|${selectedCategory.orEmpty()}|$favoritesOnly"
}

internal fun shouldShowVaultLoadingSkeleton(
    isLoading: Boolean,
    entries: List<PasswordEntry>,
): Boolean {
    return isLoading && entries.isEmpty()
}

private fun vaultEntryKey(entry: PasswordEntry): Long {
    return entry.id ?: -entry.createdAt
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
            modifier = Modifier.size(MaterialTheme.layout.heroIconSize),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "暂无密码",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
        )
        Text(
            text = "点击右下角 + 添加第一个密码",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
        )
    }
}
