package com.securevault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.AnimationTokens
import kotlinx.coroutines.delay

@Composable
fun DetailRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    showCopy: Boolean = false,
    onCopy: () -> Unit = {},
    maxLines: Int = 3,
) {
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    val iconTint by animateColorAsState(
        targetValue = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AnimationTokens.copyFeedbackDuration),
        label = "copyIconTint"
    )

    MyAppListItem(
        headline = value,
        modifier = modifier,
        overlineText = title,
        container = MyAppListItemContainer.Filled,
        maxLines = maxLines,
        trailing = if (showCopy) {
            {
                MyAppIconAction(
                    icon = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "复制",
                    onClick = {
                    copied = true
                    onCopy()
                    },
                    tint = iconTint,
                )
            }
        } else {
            null
        },
    )
}
