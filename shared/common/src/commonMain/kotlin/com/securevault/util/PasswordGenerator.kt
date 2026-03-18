package com.securevault.util

import kotlin.random.Random

data class PasswordGeneratorConfig(
    val length: Int,
    val includeUppercase: Boolean,
    val includeLowercase: Boolean,
    val includeDigits: Boolean,
    val includeSymbols: Boolean,
    val digitCount: Int = 0,
    val symbolCount: Int = 0
)

enum class PasswordPreset(val config: PasswordGeneratorConfig) {
    Strong(
        PasswordGeneratorConfig(
            length = 20,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = true,
            digitCount = 4,
            symbolCount = 2
        )
    ),
    Medium(
        PasswordGeneratorConfig(
            length = 16,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = false,
            digitCount = 3,
            symbolCount = 0
        )
    ),
    PinLike(
        PasswordGeneratorConfig(
            length = 6,
            includeUppercase = false,
            includeLowercase = false,
            includeDigits = true,
            includeSymbols = false,
            digitCount = 6,
            symbolCount = 0
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
        require(config.length in 4..128) { "密码长度需在 4..128 之间" }
        require(config.digitCount >= 0 && config.symbolCount >= 0) { "数字和符号个数不能为负数" }

        val requiredDigits = if (config.includeDigits) config.digitCount else 0
        val requiredSymbols = if (config.includeSymbols) config.symbolCount else 0
        require(requiredDigits + requiredSymbols <= config.length) { "数字和符号个数之和不能超过密码长度" }

        val result = mutableListOf<Char>()

        repeat(requiredDigits) {
            result.add(DIGITS[random.nextInt(DIGITS.length)])
        }
        repeat(requiredSymbols) {
            result.add(SYMBOLS[random.nextInt(SYMBOLS.length)])
        }

        val remainCount = config.length - result.size
        repeat(remainCount) {
            result.add(charset[random.nextInt(charset.length)])
        }

        for (index in result.indices.reversed()) {
            val swapIndex = random.nextInt(index + 1)
            val temp = result[index]
            result[index] = result[swapIndex]
            result[swapIndex] = temp
        }

        val generated = result.joinToString("")

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
