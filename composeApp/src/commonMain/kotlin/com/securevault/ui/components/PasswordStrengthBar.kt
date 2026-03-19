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
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.theme.StrengthMedium
import com.securevault.ui.theme.StrengthStrong
import com.securevault.ui.theme.StrengthVeryStrong
import com.securevault.ui.theme.StrengthVeryWeak
import com.securevault.ui.theme.StrengthWeak
import com.securevault.ui.theme.spacing
import com.securevault.util.PasswordStrengthCalculator
import com.securevault.util.PasswordStrengthLevel

enum class PasswordStrength(val label: String, val progress: Float) {
    VeryWeak("非常弱", 0.1f),
    Weak("弱", 0.3f),
    Medium("一般", 0.5f),
    Strong("强", 0.75f),
    VeryStrong("非常强", 1f)
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
    val strengthColor = when (strength) {
        PasswordStrength.VeryWeak -> StrengthVeryWeak
        PasswordStrength.Weak -> StrengthWeak
        PasswordStrength.Medium -> StrengthMedium
        PasswordStrength.Strong -> StrengthStrong
        PasswordStrength.VeryStrong -> StrengthVeryStrong
    }
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
            Text(text = strength.label, style = MaterialTheme.typography.bodyMedium, color = strengthColor)
        }
        LinearProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxWidth().height(MaterialTheme.spacing.sm),
            color = strengthColor
        )
    }
}
