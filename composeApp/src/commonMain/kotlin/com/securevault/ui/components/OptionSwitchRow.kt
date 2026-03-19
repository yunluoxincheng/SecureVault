package com.securevault.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OptionSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: MyAppListItemContainer = MyAppListItemContainer.Filled,
) {
    MyAppSwitchRow(
        headline = label,
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        container = container,
    )
}

@Composable
fun SettingsSwitchRow(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MyAppSwitchRow(
        headline = label,
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        supportingText = description,
        enabled = enabled,
    )
}
