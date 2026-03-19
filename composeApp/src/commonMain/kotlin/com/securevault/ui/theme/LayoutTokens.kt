package com.securevault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LayoutTokens(
    val minInteractiveSize: Dp = 48.dp,
    val buttonHeight: Dp = 48.dp,
    val buttonIconSize: Dp = 18.dp,
    val buttonProgressStrokeWidth: Dp = 2.dp,
    val inputHeight: Dp = 56.dp,
    val heroIconSize: Dp = 64.dp,
    val listItemIconContainerSize: Dp = 40.dp,
    val listItemIconSize: Dp = 20.dp,
    val smallStatusIconSize: Dp = 16.dp,
    val pageHorizontalPadding: Dp = 16.dp,
    val pageMaxWidth: Dp = 720.dp,
    val pageTopPadding: Dp = 16.dp,
    val sectionSpacing: Dp = 24.dp,
    val contentSpacing: Dp = 16.dp,
    val compactContentSpacing: Dp = 12.dp,
    val cardPaddingHorizontal: Dp = 16.dp,
    val cardPaddingVertical: Dp = 12.dp,
    val topBarTopPadding: Dp = 12.dp,
    val topBarBottomPadding: Dp = 8.dp,
    val topBarSideWidth: Dp = 48.dp,
    val badgeHorizontalPadding: Dp = 6.dp,
    val badgeVerticalPadding: Dp = 2.dp,
    val fabContentClearance: Dp = 88.dp,
    val bottomBarActionInset: Dp = 96.dp,
    val fabElevation: Dp = 8.dp,
    val fabPressedElevation: Dp = 3.dp,
)

val LocalLayoutTokens = staticCompositionLocalOf { LayoutTokens() }

val MaterialTheme.layout: LayoutTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalLayoutTokens.current
