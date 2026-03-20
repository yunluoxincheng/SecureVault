# 测试策略与性能基准

> 版本 1.0 — 2026-03-18

---

## 一、测试分层策略

### 1.1 测试金字塔

```
        ╱  E2E  ╲              ← 少量，验证关键用户流程
       ╱─────────╲
      ╱ 集成测试   ╲           ← 中等，验证模块协作
     ╱─────────────╲
    ╱   单元测试     ╲         ← 大量，验证独立逻辑
   ╱─────────────────╲
```

| 层级 | 占比 | 执行环境 | 频率 |
|------|------|---------|------|
| **单元测试** | ~70% | `commonTest` (JVM) | 每次提交 |
| **集成测试** | ~20% | 各平台（Android Emulator, iOS Simulator, Desktop JVM） | 每次 PR |
| **E2E 测试** | ~10% | 真机/模拟器 | 每次发版 |

### 1.2 KMP 测试工具链

| 工具 | 用途 | 平台 |
|------|------|------|
| **kotlin.test** | 断言、测试注解 | 全平台（commonTest） |
| **kotlinx-coroutines-test** | 协程测试 (`runTest`, `TestScope`) | 全平台 |
| **Turbine** | Flow 测试（值断言、超时） | 全平台 |
| **SQLDelight Test Driver** | 内存数据库测试 | 全平台 |
| **Robolectric** | Android 组件单元测试 | Android (JVM) |
| **Compose UI Test** | Compose UI 测试 | Android / Desktop |
| **Espresso** | Android 集成测试 | Android |
| **XCTest** | iOS 测试 | iOS |

---

## 二、单元测试规范

### 2.1 覆盖目标

| 模块 | 目标覆盖率 | 优先级 | 理由 |
|------|-----------|--------|------|
| `crypto/` | **≥ 95%** | P0 | 加密逻辑的任何 bug 都是安全漏洞 |
| `security/` | **≥ 90%** | P0 | 密钥管理、会话控制 |
| `usecase/` | **≥ 85%** | P1 | 业务逻辑正确性 |
| `storage/` | **≥ 80%** | P1 | 数据完整性 |
| `viewmodel/` | **≥ 70%** | P2 | 状态管理 |
| `util/` | **≥ 80%** | P2 | 工具函数 |
| `ui/` | 不强制 | P3 | 依赖 Compose UI 测试框架 |

### 2.2 加密模块测试用例

```kotlin
// crypto/ 测试
class AesGcmCipherTest {
    // 基本功能
    @Test fun encrypt_decrypt_roundtrip()
    @Test fun encrypt_produces_different_iv_each_time()
    @Test fun decrypt_with_wrong_key_throws()
    @Test fun decrypt_with_tampered_ciphertext_throws()
    @Test fun decrypt_with_tampered_iv_throws()

    // 边界条件
    @Test fun encrypt_empty_data()
    @Test fun encrypt_large_data_1MB()
    @Test fun encrypt_single_byte()

    // 格式
    @Test fun storage_format_roundtrip()
    @Test fun storage_format_v2_prefix()
    @Test fun invalid_storage_format_throws()
}

class SecurePaddingTest {
    @Test fun pad_unpad_roundtrip()
    @Test fun padded_length_is_multiple_of_256()
    @Test fun padding_is_random_not_zeros()
    @Test fun pad_empty_data()
    @Test fun pad_exactly_256_bytes()
    @Test fun pad_257_bytes()  // 跨块边界
    @Test fun unpad_invalid_length_throws()
}

class Argon2KdfTest {
    @Test fun derive_key_produces_32_bytes()
    @Test fun same_password_same_salt_same_key()  // 确定性
    @Test fun different_password_different_key()
    @Test fun different_salt_different_key()
    @Test fun generate_salt_produces_16_bytes()
    @Test fun generate_salt_is_random()
}

class CryptoUtilsTest {
    @Test fun constant_time_equals_identical()
    @Test fun constant_time_equals_different()
    @Test fun constant_time_equals_different_length()
    @Test fun char_array_to_utf16be_roundtrip()
    @Test fun base64_encode_decode_roundtrip()
}
```

### 2.3 安全模块测试用例

```kotlin
class SessionManagerTest {
    @Test fun initially_locked()
    @Test fun unlock_with_data_key()
    @Test fun get_data_key_when_locked_throws()
    @Test fun lock_clears_data_key()
    @Test fun auto_lock_immediate()
    @Test fun on_app_background_when_immediate_timeout_enabled_locks_immediately()
    @Test fun auto_lock_after_timeout()
    @Test fun auto_lock_never()
    @Test fun extend_session_resets_timer()
}

class SessionLifecycleLockTest {
    @Test fun background_then_foreground_locks_session_when_policy_matches()
}

class KeyManagerTest {
    @Test fun setup_vault_creates_salt_and_encrypted_key()
    @Test fun unlock_with_correct_password()
    @Test fun unlock_with_wrong_password_returns_error()
    @Test fun change_password_success()
    @Test fun change_password_wrong_current_fails()
    @Test fun enroll_biometric_encrypts_data_key()
    @Test fun unlock_biometric_decrypts_data_key()
}

class SensitiveDataTest {
    @Test fun get_returns_data()
    @Test fun close_wipes_byte_array()
    @Test fun close_wipes_char_array()
    @Test fun get_after_close_throws()
    @Test fun double_close_is_safe()
    @Test fun use_block_auto_closes()
}

class MemorySanitizerTest {
    @Test fun wipe_byte_array_all_zeros()
    @Test fun wipe_char_array_all_zeros()
    @Test fun wipe_with_custom_passes()
}

class BiometricStateTest {
    @Test fun initially_not_locked_out()
    @Test fun lockout_after_max_failures()
    @Test fun lockout_expires()
    @Test fun success_resets_failures()
    @Test fun debounce_prevents_rapid_auth()
}
```

### 2.4 用例层测试用例

```kotlin
class AddPasswordUseCaseTest {
    @Test fun add_password_encrypts_all_fields()
    @Test fun add_password_when_locked_returns_error()
    @Test fun add_password_with_security_mode()
    @Test fun add_password_generates_unique_iv()
}

class SearchPasswordUseCaseTest {
    @Test fun search_by_title()
    @Test fun search_by_username()
    @Test fun search_by_url()
    @Test fun search_empty_query_returns_all()
    @Test fun search_no_results()
    @Test fun filter_by_category()
    @Test fun filter_by_favorite()
}

class ExportVaultUseCaseTest {
    @Test fun export_encrypted_roundtrip()
    @Test fun export_secure_mode_roundtrip()
    @Test fun export_when_locked_returns_error()
    @Test fun import_detects_duplicates()
    @Test fun import_invalid_format_returns_error()
}
```

---

## 三、集成测试规范

### 3.1 关键集成流程

| 流程 | 覆盖范围 | 平台 |
|------|---------|------|
| 首次设置 → 添加密码 → 锁定 → 解锁 → 查看 | KeyManager → SessionManager → Repository → Cipher | 全平台 |
| 主密码解锁 → 搜索 → 复制 → 剪贴板清除 | SessionManager → Repository → Clipboard | 全平台 |
| 安全模式开启 → 添加 → 使用(复制) → 导出 → 导入 | SecurityMode → Repository → Export | 全平台 |
| 生物识别注册 → 锁定 → 生物识别解锁 | KeyManager → PlatformKeyStore → BiometricAuth | Android/iOS |
| AutofillService: 填充请求 → 匹配 → 响应 | Parser → Matcher → Builder | Android |
| AutofillService: 保存请求 → 去重 → 保存 | Parser → Detector → Repository | Android |

### 3.2 数据库集成测试

```kotlin
class PasswordRepositoryIntegrationTest {
    // 使用 SQLDelight in-memory driver
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    private val database = SecureVaultDatabase(driver)

    @Test fun insert_and_query_roundtrip()
    @Test fun update_entry()
    @Test fun delete_entry()
    @Test fun search_by_encrypted_fields()  // 注意：加密后无法搜索明文
    @Test fun category_filter()
    @Test fun favorite_filter()
    @Test fun security_mode_filter()
    @Test fun concurrent_read_write()
}
```

### 3.3 跨平台一致性测试

确保加密结果在所有平台上一致：

```kotlin
class CrossPlatformCryptoTest {
    // 使用固定的测试向量
    private val testPassword = "TestPassword123!".toCharArray()
    private val testSalt = Base64.decode("dGVzdHNhbHQxMjM0NTY3OA==")
    private val expectedKeyHex = "..." // 预计算的正确结果

    @Test fun argon2_produces_same_key_on_all_platforms() {
        val key = argon2Kdf.deriveKey(
            testPassword, testSalt,
            AdaptiveArgon2Config.getStandardParameters()
        )
        assertEquals(expectedKeyHex, key.toHexString())
    }

    @Test fun aes_gcm_produces_same_ciphertext_with_fixed_iv() {
        // 使用固定 IV 验证跨平台一致性（仅测试用，生产代码不可固定 IV）
    }

    @Test fun secure_padding_roundtrip_cross_platform() {
        val data = "Hello, SecureVault!".encodeToByteArray()
        val padded = SecurePadding.pad(data)
        val unpadded = SecurePadding.unpad(padded)
        assertContentEquals(data, unpadded)
    }
}
```

---

## 四、E2E 测试

### 4.1 关键用户流程

| 编号 | 流程 | 验证点 |
|------|------|--------|
| E2E-01 | 首次启动 → 设置主密码 → 添加第一个密码 → 查看 | 完整的新用户上手流程 |
| E2E-02 | 解锁 → 搜索密码 → 复制 → 粘贴到浏览器 | 核心使用场景 |
| E2E-03 | 设置 → 开启安全模式 → 添加安全密码 → 尝试查看（应失败） → 复制（应成功） | 安全模式的核心约束 |
| E2E-04 | 导出 → 清除数据 → 导入 → 验证所有密码存在 | 数据备份恢复 |
| E2E-05 | 锁定 → 生物识别解锁 → 操作正常 | 生物识别快速路径 |
| E2E-06 | Chrome 登录页面 → AutofillService 弹出 → 选择凭证 → 字段已填充 | Android 自动填充 |
| E2E-07 | Chrome 注册新账号 → 弹出"保存密码？" → 保存 → 密码库中可见 | 自动保存检测 |

---

## 五、性能基准

### 5.1 关键指标

| 指标 | 目标 | 测量方法 | 优先级 |
|------|------|---------|--------|
| **Argon2 解锁耗时** | < 2s (高端) / < 4s (低端) | `measureTimeMillis { argon2.deriveKey() }` | P0 |
| **XChaCha20-Poly1305 单字段加密** | < 1ms | Benchmark 工具 | P0 |
| **密码列表加载 (100条)** | < 200ms | 从解锁到列表可见 | P0 |
| **密码列表加载 (1000条)** | < 1s | 从解锁到列表可见 | P1 |
| **列表滚动帧率** | ≥ 60fps | GPU profiler | P1 |
| **应用冷启动到解锁界面** | < 1s | adb shell am start 计时 | P1 |
| **SecurePadding (1KB)** | < 0.1ms | Benchmark | P2 |
| **MemorySanitizer (1KB, 3轮)** | < 0.1ms | Benchmark | P2 |
| **AutofillService 响应** | < 300ms | 从 onFillRequest 到 callback | P0 |
| **导出 1000 条密码** | < 5s | 加密 + 写入文件 | P2 |

### 5.2 Argon2 性能基准矩阵

需要在各平台各参数下测量，确认自适应配置合理：

| 平台 | 参数组 | 设备 | 目标耗时 |
|------|-------|------|---------|
| Android | 标准 (128MB/3/4) | Pixel 7 | < 1.5s |
| Android | 标准 (128MB/3/4) | 中端 (6GB RAM) | < 2.5s |
| Android | 降级 (64MB/2/2) | 低端 (4GB RAM) | < 3s |
| iOS | 标准 (128MB/3/4) | iPhone 13+ | < 1.5s |
| iOS | 降级 (64MB/2/2) | iPhone SE | < 3s |
| Desktop | 标准 (128MB/3/4) | 8GB+ RAM | < 1s |
| Desktop | 降级 (64MB/2/2) | 4GB RAM | < 2s |

### 5.3 内存基准

| 指标 | 目标 | 说明 |
|------|------|------|
| 空闲内存占用 | < 50MB | 解锁状态，无操作 |
| 密码列表内存 (100条) | < 80MB | 列表可见 |
| 密码列表内存 (1000条) | < 150MB | 列表可见 |
| Argon2 峰值内存 | ≤ 配置值 + 20MB | 不应超出配置 |
| 加密导出内存 (1000条) | < 200MB | 不能一次性加载全部明文 |

### 5.4 数据库性能

| 操作 | 100 条 | 1,000 条 | 10,000 条 |
|------|--------|----------|-----------|
| 插入 1 条 | < 5ms | < 5ms | < 10ms |
| 查询全部 | < 20ms | < 100ms | < 500ms |
| 模糊搜索 | < 50ms | < 200ms | < 1s |
| 删除 1 条 | < 5ms | < 5ms | < 10ms |

---

## 六、性能测试实现

### 6.1 Benchmark 框架

```kotlin
// commonTest 中的简易 benchmark
fun benchmark(name: String, iterations: Int = 100, block: () -> Unit) {
    // 预热
    repeat(10) { block() }

    // 测量
    val times = LongArray(iterations)
    repeat(iterations) { i ->
        val start = measureTimeNanos { block() }
        times[i] = start
    }

    val avg = times.average() / 1_000_000.0  // 转为 ms
    val p50 = times.sorted()[iterations / 2] / 1_000_000.0
    val p99 = times.sorted()[(iterations * 0.99).toInt()] / 1_000_000.0

    println("[$name] avg=${avg}ms, p50=${p50}ms, p99=${p99}ms")
}

// 使用示例
class CryptoPerformanceTest {
    @Test fun benchmark_aes_gcm_encrypt_1kb() {
        val key = CryptoUtils.generateSecureRandom(32)
        val data = CryptoUtils.generateSecureRandom(1024)
        val cipher = AesGcmCipher()

        benchmark("XChaCha20-Poly1305 encrypt 1KB") {
            cipher.encrypt(data, key)
        }
    }

    @Test fun benchmark_secure_padding_1kb() {
        val data = CryptoUtils.generateSecureRandom(1024)

        benchmark("SecurePadding 1KB") {
            val padded = SecurePadding.pad(data)
            SecurePadding.unpad(padded)
        }
    }

    @Test fun benchmark_argon2_standard() {
        val password = "TestPassword123!".toCharArray()
        val salt = CryptoUtils.generateSecureRandom(16)

        benchmark("Argon2id standard", iterations = 5) {
            argon2Kdf.deriveKey(password, salt,
                AdaptiveArgon2Config.getStandardParameters())
        }
    }
}
```

---

## 七、CI/CD 集成

### 7.1 测试流水线

```
git push / PR
    ↓
┌──────────────────────────────────────┐
│ Stage 1: Lint + Format               │
│ - ktlint / detekt                    │
│ - 编译检查                            │
│ 约 1 分钟                             │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│ Stage 2: 单元测试                     │
│ - commonTest (JVM)                   │
│ - androidUnitTest                    │
│ - desktopTest                        │
│ 约 2 分钟                             │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│ Stage 3: 集成测试 (仅 PR/main)        │
│ - Android Emulator 测试              │
│ - Desktop 集成测试                    │
│ 约 5 分钟                             │
└──────────────┬───────────────────────┘
               ↓ (仅 release 分支)
┌──────────────────────────────────────┐
│ Stage 4: 性能基准 + E2E               │
│ - Argon2 性能回归检查                 │
│ - 关键 E2E 流程                      │
│ 约 10 分钟                            │
└──────────────────────────────────────┘
```

### 7.2 质量门禁

| 检查项 | 阈值 | 阻断级别 |
|-------|------|---------|
| 单元测试通过率 | 100% | 阻断合并 |
| crypto/ 覆盖率 | ≥ 95% | 阻断合并 |
| security/ 覆盖率 | ≥ 90% | 阻断合并 |
| 总覆盖率 | ≥ 70% | 警告 |
| Argon2 性能回归 | < 10% 退化 | 警告 |
| 无新 lint 错误 | 0 | 阻断合并 |
