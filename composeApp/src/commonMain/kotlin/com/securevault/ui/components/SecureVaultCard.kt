package com.securevault.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SvElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MyAppCard(
        modifier = modifier,
        variant = MyAppCardVariant.Elevated,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun SvFilledCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MyAppCard(
        modifier = modifier,
        variant = MyAppCardVariant.Filled,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun SvOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MyAppCard(
        modifier = modifier,
        variant = MyAppCardVariant.Outlined,
        onClick = onClick,
        content = content,
    )
}
