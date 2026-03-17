package com.securevault.ui.components

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
import androidx.compose.ui.unit.dp

enum class PasswordStrength(val label: String, val progress: Float, val color: Color) {
    VeryWeak("非常弱", 0.1f, Color(0xFFD32F2F)),
    Weak("弱", 0.3f, Color(0xFFF57C00)),
    Medium("一般", 0.5f, Color(0xFFFBC02D)),
    Strong("强", 0.75f, Color(0xFF388E3C)),
    VeryStrong("非常强", 1f, Color(0xFF1B6B4F))
}

fun calculatePasswordStrength(password: String): PasswordStrength {
    val score = buildList {
        add(password.length >= 8)
        add(password.any { it.isUpperCase() })
        add(password.any { it.isLowerCase() })
        add(password.any { it.isDigit() })
        add(password.any { !it.isLetterOrDigit() })
    }.count { it }

    return when {
        score <= 1 -> PasswordStrength.VeryWeak
        score == 2 -> PasswordStrength.Weak
        score == 3 -> PasswordStrength.Medium
        score == 4 -> PasswordStrength.Strong
        else -> PasswordStrength.VeryStrong
    }
}

@Composable
fun PasswordStrengthBar(password: String, modifier: Modifier = Modifier) {
    val strength = calculatePasswordStrength(password)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "密码强度", style = MaterialTheme.typography.bodyMedium)
            Text(text = strength.label, style = MaterialTheme.typography.bodyMedium, color = strength.color)
        }
        LinearProgressIndicator(
            progress = { strength.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = strength.color
        )
    }
}
