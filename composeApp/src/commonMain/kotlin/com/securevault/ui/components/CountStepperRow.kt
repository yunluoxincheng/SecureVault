package com.securevault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun CountStepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeMax = max.coerceAtLeast(min)

    MyAppCard(
        modifier = modifier.fillMaxWidth(),
        variant = MyAppCardVariant.Filled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.layout.cardPaddingHorizontal,
                    vertical = MaterialTheme.layout.cardPaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                IconButton(
                    onClick = onDecrease,
                    enabled = value > min,
                    modifier = Modifier.size(MaterialTheme.layout.minInteractiveSize),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "减少")
                }

                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .widthIn(min = 32.dp)
                        .padding(horizontal = MaterialTheme.spacing.sm),
                )

                IconButton(
                    onClick = onIncrease,
                    enabled = value < safeMax,
                    modifier = Modifier.size(MaterialTheme.layout.minInteractiveSize),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "增加")
                }
            }
        }
    }
}
