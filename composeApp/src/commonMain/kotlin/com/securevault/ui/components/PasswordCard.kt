package com.securevault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.theme.FavoriteColor
import com.securevault.ui.theme.SecurityModeColor
import com.securevault.ui.theme.spacing

@Composable
fun PasswordCard(
    entry: PasswordEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
) {
    SvElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateItemEntrance(index),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leading icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.securityMode)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (entry.securityMode)
                        SecurityModeColor
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.username.isNotBlank()) {
                    Text(
                        text = entry.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val url = entry.url
                if (!url.isNullOrBlank()) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Trailing indicators
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (entry.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "已收藏",
                        modifier = Modifier.size(16.dp),
                        tint = FavoriteColor,
                    )
                }
                if (entry.securityMode) {
                    val badgeBg by animateColorAsState(
                        targetValue = SecurityModeColor.copy(alpha = 0.12f),
                        animationSpec = tween(300),
                        label = "securityBadgeBg"
                    )
                    Text(
                        text = "安全",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecurityModeColor,
                        modifier = Modifier
                            .padding(top = if (entry.isFavorite) 2.dp else 0.dp)
                            .background(badgeBg, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
