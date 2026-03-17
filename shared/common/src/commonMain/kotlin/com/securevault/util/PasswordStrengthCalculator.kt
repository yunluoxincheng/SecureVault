package com.securevault.util

enum class PasswordStrengthLevel {
    VeryWeak,
    Weak,
    Medium,
    Strong,
    VeryStrong
}

object PasswordStrengthCalculator {
    fun calculate(password: String): PasswordStrengthLevel {
        if (password.isBlank()) return PasswordStrengthLevel.VeryWeak

        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 1 -> PasswordStrengthLevel.VeryWeak
            score == 2 -> PasswordStrengthLevel.Weak
            score == 3 -> PasswordStrengthLevel.Medium
            score in 4..5 -> PasswordStrengthLevel.Strong
            else -> PasswordStrengthLevel.VeryStrong
        }
    }
}
