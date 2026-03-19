package com.securevault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val DefaultRadius = Radius()

val SecureVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(DefaultRadius.xs),
    small = RoundedCornerShape(DefaultRadius.sm),
    medium = RoundedCornerShape(DefaultRadius.md),
    large = RoundedCornerShape(DefaultRadius.lg),
    extraLarge = RoundedCornerShape(DefaultRadius.xl),
)
