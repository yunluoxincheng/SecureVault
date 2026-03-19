package com.securevault.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

data class MyAppBottomBarItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun MyAppIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(MaterialTheme.layout.minInteractiveSize),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

@Composable
fun MyAppDivider(
    modifier: Modifier = Modifier,
    horizontalInset: Dp = MaterialTheme.spacing.md,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = horizontalInset),
        color = color,
    )
}

@Composable
fun MyAppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = MaterialTheme.layout.minInteractiveSize),
        label = { Text(label) },
    )
}

@Composable
fun MyAppFloatingActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = MaterialTheme.layout.fabElevation,
            pressedElevation = MaterialTheme.layout.fabPressedElevation,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun MyAppSelectionRow(
    headline: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    container: MyAppListItemContainer = MyAppListItemContainer.None,
) {
    MyAppListItem(
        headline = headline,
        supportingText = supportingText,
        modifier = modifier,
        container = container,
        onClick = onClick,
        trailing = {
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun MyAppSwitchRow(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    container: MyAppListItemContainer = MyAppListItemContainer.None,
) {
    MyAppListItem(
        headline = headline,
        supportingText = supportingText,
        modifier = modifier,
        container = container,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun MyAppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        enabled = enabled,
        steps = steps,
    )
}

@Composable
fun <T> MyAppBottomBar(
    items: List<MyAppBottomBarItem<T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val isSelected = item.value == selectedItem
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) onItemSelected(item.value)
                },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
            )
        }
    }
}
