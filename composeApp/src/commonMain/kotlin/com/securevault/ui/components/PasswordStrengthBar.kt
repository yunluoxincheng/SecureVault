package com.securevault.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.theme.spacing
import com.securevault.util.PasswordStrengthCalculator
import com.securevault.util.PasswordStrengthLevel

enum class PasswordStrength(val label: String, val progress: Float, val color: Color) {
    VeryWeak("非常弱", 0.1f, Color(0xFFD32F2F)),
    Weak("弱", 0.3f, Color(0xFFF57C00)),
    Medium("一般", 0.5f, Color(0xFFFBC02D)),
    Strong("强", 0.75f, Color(0xFF388E3C)),
    VeryStrong("非常强", 1f, Color(0xFF1B6B4F))
}

fun calculatePasswordStrength(password: String): PasswordStrength {
    return when (PasswordStrengthCalculator.calculate(password)) {
        PasswordStrengthLevel.VeryWeak -> PasswordStrength.VeryWeak
        PasswordStrengthLevel.Weak -> PasswordStrength.Weak
        PasswordStrengthLevel.Medium -> PasswordStrength.Medium
        PasswordStrengthLevel.Strong -> PasswordStrength.Strong
        PasswordStrengthLevel.VeryStrong -> PasswordStrength.VeryStrong
    }
}

@Composable
fun PasswordStrengthBar(password: String, modifier: Modifier = Modifier) {
    val strength = calculatePasswordStrength(password)
    val targetProgress = if (password.isBlank()) {
        0f
    } else {
        (PasswordStrengthCalculator.calculateScore(password) / 100f).coerceIn(0.05f, 1f)
    }

    val animatedProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = AnimationTokens.strengthBarDuration,
            easing = AnimationTokens.easeInOut,
        ),
        label = "passwordStrengthProgress",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "密码强度", style = MaterialTheme.typography.bodyMedium)
            Text(text = strength.label, style = MaterialTheme.typography.bodyMedium, color = strength.color)
        }
        LinearProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxWidth().height(MaterialTheme.spacing.sm),
            color = strength.color
        )
    }
}
