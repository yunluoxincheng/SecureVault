package com.securevault.util

enum class PasswordStrengthLevel {
    VeryWeak,
    Weak,
    Medium,
    Strong,
    VeryStrong
}

object PasswordStrengthCalculator {
    fun calculateScore(password: String): Int {
        if (password.isBlank()) return 0

        val length = password.length
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        val categoryCount = listOf(hasUpper, hasLower, hasDigit, hasSymbol).count { it }

        var score = 0

        score += when {
            length <= 5 -> 5
            length <= 7 -> 12
            length <= 9 -> 22
            length <= 11 -> 34
            length <= 15 -> 46
            length <= 24 -> 56
            else -> 60
        }

        score += when (categoryCount) {
            1 -> 0
            2 -> 10
            3 -> 22
            else -> 30
        }

        val uniqueChars = password.toSet().size
        score += (uniqueChars * 0.6f).toInt().coerceAtMost(8)

        if (password.zipWithNext().count { it.first == it.second } >= 2) score -= 8
        if (Regex("(.)\\1{2,}").containsMatchIn(password)) score -= 10
        if (Regex("^(?:\\d+|[a-zA-Z]+|[^a-zA-Z\\d]+)$").matches(password)) score -= 10
        if (Regex("1234|abcd|qwer|password|admin", RegexOption.IGNORE_CASE).containsMatchIn(password)) score -= 14

        return score.coerceIn(0, 100)
    }

    fun calculate(password: String): PasswordStrengthLevel {
        val score = calculateScore(password)

        return when {
            score < 25 -> PasswordStrengthLevel.VeryWeak
            score < 45 -> PasswordStrengthLevel.Weak
            score < 65 -> PasswordStrengthLevel.Medium
            score < 82 -> PasswordStrengthLevel.Strong
            else -> PasswordStrengthLevel.VeryStrong
        }
    }
}
