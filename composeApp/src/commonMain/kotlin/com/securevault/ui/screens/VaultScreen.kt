package com.securevault.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.securevault.data.PasswordEntry
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.components.MyAppFilterChip
import com.securevault.ui.components.MyAppFloatingActionButton
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.components.PasswordCard
import com.securevault.ui.components.SkeletonList
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing
import kotlinx.coroutines.delay

@Composable
fun VaultScreen(
    entries: List<PasswordEntry>,
    categories: List<String>,
    selectedCategory: String?,
    favoritesOnly: Boolean,
    query: String,
    vaultVisitNonce: Int = 0,
    isLoading: Boolean = false,
    hasLoadedAtLeastOnce: Boolean = false,
    onQueryChange: (String) -> Unit,
    onFiltersChange: (String?, Boolean) -> Unit,
    onEntryClick: (PasswordEntry) -> Unit,
    onAddClick: () -> Unit
) {
    var hasPlayedEntranceInVisit by rememberSaveable(vaultVisitNonce) { mutableStateOf(false) }
    val animationResetKey = vaultListAnimationResetKey(
        vaultVisitNonce = vaultVisitNonce,
        selectedCategory = selectedCategory,
        favoritesOnly = favoritesOnly,
    )
    val showLoadingSkeleton = shouldShowVaultLoadingSkeleton(
        isLoading = isLoading,
        entries = entries,
        hasLoadedAtLeastOnce = hasLoadedAtLeastOnce,
    )
    val animateVaultListEntrance = shouldAnimateVaultListEntrance(
        hasPlayedEntranceInVisit = hasPlayedEntranceInVisit,
        isLoading = isLoading,
        hasEntries = entries.isNotEmpty(),
    )

    fun applyFilters(targetCategory: String?, targetFavoritesOnly: Boolean) {
        onFiltersChange(targetCategory, targetFavoritesOnly)
    }

    LaunchedEffect(vaultVisitNonce, animateVaultListEntrance, entries.size) {
        if (!animateVaultListEntrance) return@LaunchedEffect

        val totalDurationMs =
            (entries.lastIndex.coerceAtLeast(0) * AnimationTokens.staggerItemDelay) +
                AnimationTokens.cardAppearDuration
        delay(totalDurationMs.toLong())
        hasPlayedEntranceInVisit = true
    }

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
                    MyAppFilterChip(
                        selected = favoritesOnly,
                        onClick = { applyFilters(selectedCategory, !favoritesOnly) },
                        label = "仅收藏",
                    )
                }
                item {
                    MyAppFilterChip(
                        selected = selectedCategory == null && !favoritesOnly,
                        onClick = { applyFilters(null, false) },
                        label = "全部",
                    )
                }
                items(categories) { category ->
                    MyAppFilterChip(
                        selected = selectedCategory == category,
                        onClick = { applyFilters(category, favoritesOnly) },
                        label = category,
                    )
                }
            }

            if (showLoadingSkeleton) {
                SkeletonList(
                    count = 6,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                )
            } else {
                AnimatedContent(
                    targetState = entries,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationTokens.crossFadeDuration,
                                easing = AnimationTokens.easeOut,
                            )
                        ) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis = AnimationTokens.crossFadeDuration,
                                easing = AnimationTokens.easeIn,
                            )
                        )
                    },
                    label = "vaultEntriesSwap",
                ) { animatedEntries ->
                    if (animatedEntries.isEmpty()) {
                        VaultEmptyState(
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.compactContentSpacing),
                            contentPadding = PaddingValues(
                                top = MaterialTheme.spacing.xs,
                                bottom = MaterialTheme.layout.fabContentClearance,
                            ),
                        ) {
                            itemsIndexed(animatedEntries, key = { _, e -> vaultEntryKey(e) }) { index, entry ->
                                PasswordCard(
                                    entry = entry,
                                    onClick = { onEntryClick(entry) },
                                    index = index,
                                    animateEntrance = animateVaultListEntrance,
                                    animationResetKey = animationResetKey,
                                )
                            }
                        }
                    }
                }
            }
        }

        MyAppFloatingActionButton(
            icon = Icons.Default.Add,
            contentDescription = "添加密码",
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.spacing.md),
        )
    }
}

internal fun vaultListAnimationResetKey(
    vaultVisitNonce: Int,
    selectedCategory: String?,
    favoritesOnly: Boolean,
): String {
    return vaultVisitNonce.toString()
}

internal fun shouldShowVaultLoadingSkeleton(
    isLoading: Boolean,
    entries: List<PasswordEntry>,
    hasLoadedAtLeastOnce: Boolean,
): Boolean {
    return isLoading && entries.isEmpty() && !hasLoadedAtLeastOnce
}

internal fun shouldAnimateVaultListEntrance(
    hasPlayedEntranceInVisit: Boolean,
    isLoading: Boolean,
    hasEntries: Boolean,
): Boolean {
    return !hasPlayedEntranceInVisit && !isLoading && hasEntries
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
