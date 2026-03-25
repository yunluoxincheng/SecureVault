package com.securevault.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportExportErrorMapperTest {

    @Test
    fun passwordExport_whenLocked_returnsFriendlyMessage() {
        val message = ImportExportErrorMapper.passwordExport(
            IllegalStateException("会话已锁定，请先解锁后再导出")
        )

        assertEquals("保险库已锁定，请先解锁后再导出", message)
    }

    @Test
    fun passwordImport_whenUnsupportedFormat_returnsFriendlyMessage() {
        val message = ImportExportErrorMapper.passwordImport(
            IllegalArgumentException("不支持的导出类型：abc")
        )

        assertEquals("导入失败：文件版本或格式不受支持，请确认选择的是 SecureVault 导出文件", message)
    }

    @Test
    fun userDataSelect_whenCancelled_returnsFriendlyMessage() {
        val message = ImportExportErrorMapper.userDataSelect(
            IllegalStateException("未选择导入文件")
        )

        assertEquals("已取消导入：未选择用户数据文件", message)
    }

    @Test
    fun userDataImport_whenWrongPassword_returnsFriendlyMessage() {
        val message = ImportExportErrorMapper.userDataImport(
            IllegalStateException("用户数据解密失败，请确认主密码正确")
        )

        assertEquals("导入失败：主密码不正确，请确认后重试", message)
    }

    @Test
    fun userDataExport_whenUnknownError_returnsGenericFriendlyMessage() {
        val message = ImportExportErrorMapper.userDataExport(
            RuntimeException("unexpected")
        )

        assertTrue(message.contains("用户数据导出失败"))
    }
}
