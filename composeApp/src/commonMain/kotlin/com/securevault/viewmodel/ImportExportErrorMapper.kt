package com.securevault.viewmodel

import kotlinx.coroutines.CancellationException

internal object ImportExportErrorMapper {

    fun passwordExport(error: Throwable): String {
        if (error is CancellationException) {
            return "导出已取消"
        }
        val message = error.message.orEmpty()
        return when {
            message.contains("会话已锁定") -> "保险库已锁定，请先解锁后再导出"
            message.contains("未选择导出文件") -> "已取消导出：未选择目标文件"
            message.contains("无法写入目标文件") -> "导出失败：无法写入目标文件，请检查存储权限或磁盘空间"
            message.contains("当前没有可用的 Android Activity") -> "导出失败：当前页面不可用，请返回后重试"
            else -> "导出失败，请重试；若问题持续，请更换保存位置或检查文件权限"
        }
    }

    fun passwordImport(error: Throwable): String {
        if (error is CancellationException) {
            return "导入已取消"
        }
        val message = error.message.orEmpty()
        return when {
            message.contains("会话已锁定") -> "保险库已锁定，请先解锁后再导入"
            message.contains("未选择导入文件") -> "已取消导入：未选择源文件"
            message.contains("无法读取导入文件") -> "导入失败：无法读取文件，请检查文件权限或文件是否存在"
            message.contains("不支持的导出版本") || message.contains("不支持的导出类型") ->
                "导入失败：文件版本或格式不受支持，请确认选择的是 SecureVault 导出文件"
            message.contains("不匹配") -> "导入失败：该密码库与当前用户数据不匹配，请先导入对应的用户数据"
            message.contains("缺少") -> "导入失败：文件内容不完整或已损坏，请重新选择备份文件"
            message.contains("当前没有可用的 Android Activity") -> "导入失败：当前页面不可用，请返回后重试"
            else -> "导入失败，请确认文件内容完整且主密码相关配置正确后重试"
        }
    }

    fun userDataExport(error: Throwable): String {
        if (error is CancellationException) {
            return "用户数据导出已取消"
        }
        val message = error.message.orEmpty()
        return when {
            message.contains("未选择导出文件") -> "已取消导出：未选择目标文件"
            message.contains("无法写入目标文件") -> "用户数据导出失败：无法写入目标文件，请检查存储权限或磁盘空间"
            message.contains("当前尚未配置保险库") || message.contains("缺少保险库密钥数据") ->
                "用户数据导出失败：当前设备尚未完成初始化或数据不完整"
            message.contains("主密码") || message.contains("解密") -> "用户数据导出失败：主密码验证未通过，请确认后重试"
            message.contains("当前没有可用的 Android Activity") -> "用户数据导出失败：当前页面不可用，请返回后重试"
            else -> "用户数据导出失败，请稍后重试"
        }
    }

    fun userDataSelect(error: Throwable): String {
        if (error is CancellationException) {
            return "已取消用户数据导入"
        }
        val message = error.message.orEmpty()
        return when {
            message.contains("未选择导入文件") -> "已取消导入：未选择用户数据文件"
            message.contains("无法读取导入文件") -> "读取用户数据文件失败，请检查文件权限或文件是否存在"
            message.contains("当前没有可用的 Android Activity") -> "当前页面不可用，请返回后重试"
            else -> "选择用户数据文件失败，请重试"
        }
    }

    fun userDataImport(error: Throwable): String {
        if (error is CancellationException) {
            return "导入已取消"
        }
        val message = error.message.orEmpty()
        return when {
            message.contains("不支持的用户数据版本") || message.contains("不支持的用户数据类型") ->
                "导入失败：用户数据文件版本或格式不受支持"
            message.contains("用户数据解密失败") || message.contains("主密码验证失败") ->
                "导入失败：主密码不正确，请确认后重试"
            message.contains("导入，但主密码验证失败") ->
                "用户数据已导入，但当前主密码校验失败，请重新输入主密码解锁"
            message.contains("无法读取导入文件") -> "导入失败：无法读取用户数据文件，请检查文件权限"
            message.contains("当前没有可用的 Android Activity") -> "导入失败：当前页面不可用，请返回后重试"
            else -> "导入用户数据失败，请确认备份文件和主密码后重试"
        }
    }
}