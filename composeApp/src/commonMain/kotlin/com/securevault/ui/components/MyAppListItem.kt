package com.securevault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

enum class MyAppListItemContainer {
    None,
    Filled,
    Elevated,
    Outlined,
}

@Composable
fun MyAppListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    overlineText: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    container: MyAppListItemContainer = MyAppListItemContainer.None,
    onClick: (() -> Unit)? = null,
    maxLines: Int = 3,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.layout.cardPaddingHorizontal,
        vertical = MaterialTheme.layout.cardPaddingVertical,
    ),
) {
    val rowModifier = if (container == MyAppListItemContainer.None) modifier else Modifier
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = rowModifier
                .fillMaxWidth()
                .then(
                    if (onClick != null && container == MyAppListItemContainer.None) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                if (overlineText != null) {
                    Text(
                        text = overlineText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (trailing != null) {
                trailing()
            }
        }
    }

    when (container) {
        MyAppListItemContainer.None -> rowContent()
        MyAppListItemContainer.Filled -> MyAppCard(
            modifier = modifier.fillMaxWidth(),
            variant = MyAppCardVariant.Filled,
            onClick = onClick,
            contentPadding = PaddingValues(0.dp),
        ) { rowContent() }

        MyAppListItemContainer.Elevated -> MyAppCard(
            modifier = modifier.fillMaxWidth(),
            variant = MyAppCardVariant.Elevated,
            onClick = onClick,
            contentPadding = PaddingValues(0.dp),
        ) { rowContent() }

        MyAppListItemContainer.Outlined -> MyAppCard(
            modifier = modifier.fillMaxWidth(),
            variant = MyAppCardVariant.Outlined,
            onClick = onClick,
            contentPadding = PaddingValues(0.dp),
        ) { rowContent() }
    }
}
