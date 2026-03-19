package com.securevault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.securevault.data.PasswordEntry
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.theme.FavoriteColor
import com.securevault.ui.theme.SecurityModeColor
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun PasswordCard(
    entry: PasswordEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
) {
    MyAppCard(
        onClick = onClick,
        modifier = modifier
            .animateItemEntrance(index),
        variant = MyAppCardVariant.Elevated,
    ) {
        MyAppListItem(
            headline = entry.title,
            supportingText = entry.username.takeIf { it.isNotBlank() },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = MaterialTheme.layout.cardPaddingHorizontal,
                vertical = MaterialTheme.layout.cardPaddingVertical,
            ),
            leading = {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.layout.listItemIconContainerSize)
                        .clip(CircleShape)
                        .background(
                            if (entry.securityMode) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.layout.listItemIconSize),
                        tint = if (entry.securityMode) {
                            SecurityModeColor
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            },
            trailing = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                ) {
                    if (entry.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "已收藏",
                            modifier = Modifier.size(MaterialTheme.layout.smallStatusIconSize),
                            tint = FavoriteColor.copy(alpha = 0.9f),
                        )
                    }
                    if (entry.securityMode) {
                        val badgeBg by animateColorAsState(
                            targetValue = SecurityModeColor.copy(alpha = 0.1f),
                            animationSpec = tween(300),
                            label = "securityBadgeBg"
                        )
                        Text(
                            text = "安全",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecurityModeColor,
                            modifier = Modifier
                                .background(badgeBg, MaterialTheme.shapes.extraSmall)
                                .padding(
                                    horizontal = MaterialTheme.layout.badgeHorizontalPadding,
                                    vertical = MaterialTheme.layout.badgeVerticalPadding,
                                ),
                        )
                    }
                }
            },
        )

        val url = entry.url
        if (!url.isNullOrBlank()) {
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    start = MaterialTheme.layout.cardPaddingHorizontal + MaterialTheme.layout.listItemIconContainerSize + MaterialTheme.spacing.md,
                    end = MaterialTheme.layout.cardPaddingHorizontal,
                    bottom = MaterialTheme.layout.cardPaddingVertical,
                ),
            )
        }
    }
}
