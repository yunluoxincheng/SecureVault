# 可复用加密算法设计参考

> 从 SafeVault 提取的经过验证的加密算法设计，用 Kotlin 伪代码描述。
> 实际实现需要适配 KMP 的 expect/actual 模式。

---

## 一、Argon2id 密钥派生

### 1.1 核心参数

| 参数 | 标准值 | 最低值 | 说明 |
|------|-------|-------|------|
| `OUTPUT_LENGTH` | 32 bytes | 32 bytes | 输出 AES-256 密钥 |
| `SALT_LENGTH` | 16 bytes | 16 bytes | 128 位盐值 |
| `MEMORY_KB` | 131,072 (128 MB) | 65,536 (64 MB) | 内存消耗 |
| `ITERATIONS` | 3 | 2 | 时间成本 |
| `PARALLELISM` | 4 | 2 | 并行度 |

### 1.2 自适应配置算法

从 SafeVault 的 `AdaptiveArgon2Config` 提取，根据设备性能自动调整参数：

```kotlin
object AdaptiveArgon2Config {
    const val MIN_MEMORY_KB = 65_536       // 64 MB
    const val MIN_ITERATIONS = 2
    const val MIN_PARALLELISM = 2
    const val STANDARD_MEMORY_KB = 131_072 // 128 MB
    const val STANDARD_ITERATIONS = 3
    const val STANDARD_PARALLELISM = 4
    const val MEMORY_USAGE_RATIO = 0.25f   // 使用可用内存的 25%

    data class Argon2Parameters(
        val memoryKB: Int,
        val iterations: Int,
        val parallelism: Int
    )

    fun getOptimalParameters(): Argon2Parameters {
        val availableMemoryKB = getAvailableMemoryKB()
        val cpuCores = getAvailableProcessors()

        val memoryKB = (availableMemoryKB * MEMORY_USAGE_RATIO).toInt()
            .coerceIn(MIN_MEMORY_KB, STANDARD_MEMORY_KB)

        val parallelism = cpuCores.coerceIn(MIN_PARALLELISM, STANDARD_PARALLELISM)

        val iterations = if (memoryKB >= STANDARD_MEMORY_KB) {
            STANDARD_ITERATIONS
        } else {
            MIN_ITERATIONS
        }

        return Argon2Parameters(memoryKB, iterations, parallelism)
    }

    fun isUsingDegradedParameters(params: Argon2Parameters): Boolean {
        return params.memoryKB < STANDARD_MEMORY_KB
            || params.iterations < STANDARD_ITERATIONS
    }
}
```

**设计要点（来自 SafeVault 经验）：**
- 参数在首次运行时计算并持久化，避免每次解锁时重新计算
- 低端设备（< 128MB 可用内存）使用降级参数，但仍保证安全底线
- 密码使用 `CharArray`（非 `String`），用完立即擦除

### 1.3 密钥派生接口

```kotlin
// expect 声明
expect class Argon2Kdf() {
    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        params: AdaptiveArgon2Config.Argon2Parameters =
            AdaptiveArgon2Config.getOptimalParameters(),
        outputLength: Int = 32
    ): ByteArray

    fun generateSalt(length: Int = 16): ByteArray
}

// Android actual (argon2kt)
actual class Argon2Kdf {
    private val argon2Kt = Argon2Kt()

    actual fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        params: AdaptiveArgon2Config.Argon2Parameters,
        outputLength: Int
    ): ByteArray {
        // char[] → UTF-16BE bytes（SafeVault 验证过的编码方式）
        val passwordBytes = charArrayToUtf16BE(password)
        try {
            val result = argon2Kt.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passwordBytes,
                salt = salt,
                tCostInIterations = params.iterations,
                mCostInKibibyte = params.memoryKB,
                parallelism = params.parallelism,
                hashLengthInBytes = outputLength
            )
            return result.rawHashAsByteArray()
        } finally {
            MemorySanitizer.secureWipe(passwordBytes)
        }
    }
}

// iOS/Desktop actual (libsodium)
actual class Argon2Kdf {
    actual fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        params: AdaptiveArgon2Config.Argon2Parameters,
        outputLength: Int
    ): ByteArray {
        val passwordBytes = charArrayToUtf16BE(password)
        try {
            return LibsodiumPwHash.pwhash(
                outputLength = outputLength,
                password = passwordBytes,
                salt = salt,
                opsLimit = params.iterations.toULong(),
                memLimit = (params.memoryKB * 1024).toULong(),
                algorithm = PwHash.Algorithm.ARGON2ID13
            )
        } finally {
            MemorySanitizer.secureWipe(passwordBytes)
        }
    }
}
```

---

## 二、AES-256-GCM 加密

### 2.1 常量

| 常量 | 值 | 说明 |
|------|-----|------|
| `ALGORITHM` | `AES/GCM/NoPadding` | 算法标识 |
| `KEY_SIZE` | 256 bits | 密钥长度 |
| `IV_SIZE` | 12 bytes (96 bits) | 初始化向量 |
| `TAG_SIZE` | 128 bits | 认证标签长度 |

### 2.2 加密数据格式

```kotlin
data class EncryptedData(
    val iv: ByteArray,          // 12 bytes
    val ciphertext: ByteArray   // 密文 + 128-bit GCM tag
) {
    // 存储格式: "v2:base64(iv):base64(ciphertext)"
    fun toStorageFormat(): String {
        return "v2:${Base64.encode(iv)}:${Base64.encode(ciphertext)}"
    }

    companion object {
        fun fromStorageFormat(format: String): EncryptedData {
            val parts = format.split(":")
            require(parts.size == 3 && parts[0] == "v2")
            return EncryptedData(
                iv = Base64.decode(parts[1]),
                ciphertext = Base64.decode(parts[2])
            )
        }
    }
}
```

### 2.3 加密/解密实现

```kotlin
class AesGcmCipher {

    fun encrypt(plaintext: ByteArray, key: ByteArray): EncryptedData {
        require(key.size == 32) { "AES-256 key must be 32 bytes" }
        val iv = CryptoUtils.generateSecureRandom(12)

        // 平台实现差异通过 expect/actual 处理
        val ciphertext = platformAesGcmEncrypt(plaintext, key, iv)

        return EncryptedData(iv, ciphertext)
    }

    fun decrypt(encrypted: EncryptedData, key: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 key must be 32 bytes" }
        return platformAesGcmDecrypt(encrypted.ciphertext, key, encrypted.iv)
    }

    // 字符串级别的便捷方法（用于字段加密）
    fun encryptField(plaintext: String, key: ByteArray): String {
        val padded = SecurePadding.pad(plaintext.encodeToByteArray())
        val encrypted = encrypt(padded, key)
        return encrypted.toStorageFormat()
    }

    fun decryptField(storedFormat: String, key: ByteArray): String {
        val encrypted = EncryptedData.fromStorageFormat(storedFormat)
        val padded = decrypt(encrypted, key)
        val unpadded = SecurePadding.unpad(padded)
        return unpadded.decodeToString()
    }
}
```

**设计要点（来自 SafeVault 经验）：**
- 每次加密都生成新的随机 IV，绝不复用
- 存储格式包含版本号前缀，方便未来升级
- 字段级加密：每个字段（title, username, password, url, notes）独立加密
- SafeVault v1 无 padding，v2 增加了安全填充（`SecurePadding`）

---

## 三、安全随机填充 (SecurePadding)

### 3.1 设计目的

防止基于密文长度的信息泄露。例如：短密码 "123456" 和长密码 "MyC0mplexP@ssword!" 加密后的密文长度不同，攻击者可以推断密码的大致长度。

### 3.2 算法

| 参数 | 值 | 说明 |
|------|-----|------|
| `BLOCK_SIZE` | 256 bytes | 填充块大小 |
| 填充内容 | 随机字节 | 非零填充，增加熵 |
| 长度编码 | 最后 1 字节 | 填充长度（1-256） |

```kotlin
object SecurePadding {
    const val BLOCK_SIZE = 256

    fun pad(data: ByteArray): ByteArray {
        // 填充到 BLOCK_SIZE 的整数倍
        val paddingLength = BLOCK_SIZE - (data.size % BLOCK_SIZE)
        // paddingLength 范围: 1..256 (如果 data.size % 256 == 0, 则 paddingLength = 256)

        val padded = ByteArray(data.size + paddingLength)

        // 复制原始数据
        data.copyInto(padded)

        // 用随机字节填充（不用零，增加熵）
        val randomPadding = CryptoUtils.generateSecureRandom(paddingLength - 1)
        randomPadding.copyInto(padded, destinationOffset = data.size)

        // 最后一个字节存储填充长度
        // 如果 paddingLength == 256，存储 0（特殊值）
        padded[padded.size - 1] = (paddingLength % 256).toByte()

        return padded
    }

    fun unpad(padded: ByteArray): ByteArray {
        require(padded.isNotEmpty()) { "Padded data cannot be empty" }
        require(padded.size % BLOCK_SIZE == 0) { "Invalid padded data length" }

        val lastByte = padded[padded.size - 1].toInt() and 0xFF
        val paddingLength = if (lastByte == 0) BLOCK_SIZE else lastByte

        require(paddingLength in 1..BLOCK_SIZE) { "Invalid padding length" }
        require(padded.size >= paddingLength) { "Padding length exceeds data" }

        return padded.copyOfRange(0, padded.size - paddingLength)
    }

    // 便捷方法
    fun padString(text: String): ByteArray = pad(text.encodeToByteArray())
    fun unpadToString(padded: ByteArray): String = unpad(padded).decodeToString()
}
```

**设计要点（来自 SafeVault 经验）：**
- 256 字节块大小对密码字段来说足够大
- 随机填充而非零填充，避免填充模式泄露信息
- 最后一字节编码方案简单高效，0 表示 256 字节填充

---

## 四、敏感数据包装器 (SensitiveData)

### 4.1 设计目的

确保敏感数据（密钥、密码）在使用完毕后被安全擦除，防止内存残留。

```kotlin
class SensitiveData<T>(private var data: T?) {

    private var closed = false

    fun get(): T {
        check(!closed) { "SensitiveData has been closed" }
        return data ?: throw IllegalStateException("Data is null")
    }

    fun isClosed(): Boolean = closed

    fun close() {
        if (!closed) {
            when (val d = data) {
                is ByteArray -> MemorySanitizer.secureWipe(d)
                is CharArray -> MemorySanitizer.secureWipe(d)
            }
            data = null
            closed = true
        }
    }

    fun <R> use(block: (T) -> R): R {
        try {
            return block(get())
        } finally {
            close()
        }
    }
}
```

---

## 五、内存安全擦除 (MemorySanitizer)

### 5.1 算法

多轮覆写，确保数据不可恢复：

```kotlin
object MemorySanitizer {
    const val DEFAULT_WIPE_PASSES = 3

    fun secureWipe(data: ByteArray, passes: Int = DEFAULT_WIPE_PASSES) {
        val random = SecureRandom()
        // 前 (passes-1) 轮: 随机数据覆写
        repeat(passes - 1) {
            random.nextBytes(data)
        }
        // 最后一轮: 全零覆写
        data.fill(0)
    }

    fun secureWipe(data: CharArray, passes: Int = DEFAULT_WIPE_PASSES) {
        val random = SecureRandom()
        repeat(passes - 1) {
            for (i in data.indices) {
                data[i] = random.nextInt(Char.MAX_VALUE.code).toChar()
            }
        }
        data.fill('\u0000')
    }
}
```

**设计要点（来自 SafeVault 经验）：**
- 默认 3 轮覆写（2 轮随机 + 1 轮零填充）
- 零填充放在最后，确保最终状态可验证
- `CharArray` 和 `ByteArray` 分别处理
- 在 KMP 中，JVM 平台的 GC 可能导致数据残留，建议使用直接内存（`DirectByteBuffer`）

---

## 六、密码强度计算器

### 6.1 评分规则

从 SafeVault 的 `PasswordStrengthCalculator` 提取：

```kotlin
object PasswordStrengthCalculator {

    enum class Strength(val score: Int, val label: String) {
        VERY_WEAK(0, "非常弱"),
        WEAK(1, "弱"),
        FAIR(2, "一般"),
        STRONG(3, "强"),
        VERY_STRONG(4, "非常强")
    }

    fun calculate(password: String): Strength {
        if (password.isEmpty()) return Strength.VERY_WEAK

        var score = 0

        // 长度评分
        score += when {
            password.length >= 16 -> 3
            password.length >= 12 -> 2
            password.length >= 8 -> 1
            else -> 0
        }

        // 字符多样性
        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        val diversity = listOf(hasLower, hasUpper, hasDigit, hasSymbol).count { it }
        score += when {
            diversity >= 4 -> 3
            diversity >= 3 -> 2
            diversity >= 2 -> 1
            else -> 0
        }

        // 熵估计（不重复字符比例）
        val uniqueRatio = password.toSet().size.toFloat() / password.length
        score += when {
            uniqueRatio >= 0.8f -> 2
            uniqueRatio >= 0.6f -> 1
            else -> 0
        }

        // 常见模式扣分
        if (isCommonPattern(password)) score -= 2
        if (hasRepeatingChars(password, 3)) score -= 1

        return when {
            score >= 7 -> Strength.VERY_STRONG
            score >= 5 -> Strength.STRONG
            score >= 3 -> Strength.FAIR
            score >= 1 -> Strength.WEAK
            else -> Strength.VERY_WEAK
        }
    }

    private fun isCommonPattern(password: String): Boolean {
        val lower = password.lowercase()
        val common = listOf(
            "password", "123456", "qwerty", "abc123",
            "letmein", "admin", "welcome", "monkey"
        )
        return common.any { lower.contains(it) }
    }

    private fun hasRepeatingChars(password: String, threshold: Int): Boolean {
        if (password.length < threshold) return false
        for (i in 0..password.length - threshold) {
            if (password.substring(i, i + threshold).toSet().size == 1) return true
        }
        return false
    }
}
```

---

## 七、密码生成器

```kotlin
class PasswordGenerator {

    data class Config(
        val length: Int = 16,
        val includeUppercase: Boolean = true,
        val includeLowercase: Boolean = true,
        val includeDigits: Boolean = true,
        val includeSymbols: Boolean = true,
        val excludeAmbiguous: Boolean = false  // 排除 O/0/l/1/I 等
    )

    object Presets {
        val PIN = Config(length = 6, includeUppercase = false,
            includeLowercase = false, includeDigits = true, includeSymbols = false)
        val STRONG = Config(length = 20, includeUppercase = true,
            includeLowercase = true, includeDigits = true, includeSymbols = true)
        val MEMORABLE = Config(length = 12, includeUppercase = true,
            includeLowercase = true, includeDigits = true, includeSymbols = false)
    }

    private val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val lowercase = "abcdefghijklmnopqrstuvwxyz"
    private val digits = "0123456789"
    private val symbols = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    private val ambiguous = "O0lI1"

    fun generate(config: Config = Presets.STRONG): String {
        var charset = ""
        if (config.includeUppercase) charset += uppercase
        if (config.includeLowercase) charset += lowercase
        if (config.includeDigits) charset += digits
        if (config.includeSymbols) charset += symbols
        if (config.excludeAmbiguous) {
            charset = charset.filter { it !in ambiguous }
        }

        require(charset.isNotEmpty()) { "At least one character type must be selected" }

        val random = SecureRandom()
        val password = CharArray(config.length) {
            charset[random.nextInt(charset.length)]
        }

        // 确保每种启用的字符类型至少出现一次
        val result = ensureCharacterDiversity(password, config, charset, random)
        return String(result)
    }

    private fun ensureCharacterDiversity(
        password: CharArray,
        config: Config,
        charset: String,
        random: SecureRandom
    ): CharArray {
        val required = mutableListOf<String>()
        if (config.includeUppercase) required.add(uppercase)
        if (config.includeLowercase) required.add(lowercase)
        if (config.includeDigits) required.add(digits)
        if (config.includeSymbols) required.add(symbols)

        // 对每个必需的字符类型，检查密码中是否包含
        for ((index, charSet) in required.withIndex()) {
            if (password.none { it in charSet }) {
                // 在随机位置插入一个该类型的字符
                val pos = random.nextInt(password.size)
                val filteredSet = if (config.excludeAmbiguous) {
                    charSet.filter { it !in ambiguous }
                } else charSet
                password[pos] = filteredSet[random.nextInt(filteredSet.length)]
            }
        }
        return password
    }
}
```

---

## 八、CryptoUtils 工具函数

```kotlin
object CryptoUtils {

    fun generateSecureRandom(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    // 常量时间比较，防止时序攻击
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    fun base64Encode(data: ByteArray): String {
        // 使用平台 Base64 实现
        return platformBase64Encode(data)
    }

    fun base64Decode(encoded: String): ByteArray {
        return platformBase64Decode(encoded)
    }

    // 安全地将 CharArray 转为 UTF-16BE 字节
    // 这是 SafeVault 验证过的编码方式，确保跨平台一致性
    fun charArrayToUtf16BE(chars: CharArray): ByteArray {
        val bytes = ByteArray(chars.size * 2)
        for (i in chars.indices) {
            bytes[i * 2] = (chars[i].code shr 8).toByte()
            bytes[i * 2 + 1] = (chars[i].code and 0xFF).toByte()
        }
        return bytes
    }
}
```
