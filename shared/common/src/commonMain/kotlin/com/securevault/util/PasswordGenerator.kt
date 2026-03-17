package com.securevault.util

import kotlin.random.Random

data class PasswordGeneratorConfig(
    val length: Int,
    val includeUppercase: Boolean,
    val includeLowercase: Boolean,
    val includeDigits: Boolean,
    val includeSymbols: Boolean
)

enum class PasswordPreset(val config: PasswordGeneratorConfig) {
    Strong(
        PasswordGeneratorConfig(
            length = 20,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = true
        )
    ),
    Medium(
        PasswordGeneratorConfig(
            length = 16,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = false
        )
    ),
    PinLike(
        PasswordGeneratorConfig(
            length = 6,
            includeUppercase = false,
            includeLowercase = false,
            includeDigits = true,
            includeSymbols = false
        )
    )
}

class PasswordGenerator(
    private val random: Random = Random.Default
) {
    private val history = ArrayDeque<String>()

    fun generateFromPreset(preset: PasswordPreset): String {
        return generateCustom(preset.config)
    }

    fun generateCustom(config: PasswordGeneratorConfig): String {
        val charset = buildString {
            if (config.includeUppercase) append(UPPERCASE)
            if (config.includeLowercase) append(LOWERCASE)
            if (config.includeDigits) append(DIGITS)
            if (config.includeSymbols) append(SYMBOLS)
        }

        require(charset.isNotEmpty()) { "至少选择一种字符类型" }
        require(config.length in 4..64) { "密码长度需在 4..64 之间" }

        val generated = buildString {
            repeat(config.length) {
                append(charset[random.nextInt(charset.length)])
            }
        }

        addHistory(generated)
        return generated
    }

    fun getHistory(): List<String> = history.toList()

    private fun addHistory(password: String) {
        if (history.firstOrNull() == password) return
        history.addFirst(password)
        while (history.size > MAX_HISTORY_SIZE) {
            history.removeLast()
        }
    }

    private companion object {
        const val MAX_HISTORY_SIZE = 20
        const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        const val DIGITS = "0123456789"
        const val SYMBOLS = "!@#$%^&*()-_=+[]{}<>?"
    }
}
