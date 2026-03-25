package com.securevault.security

object ClipboardSafetyNotice {
    @Volatile
    private var shown = false

    fun withPasswordCopyHint(message: String): String {
        if (shown) {
            return message
        }

        synchronized(this) {
            if (shown) {
                return message
            }
            shown = true
            return "$message。提示：部分输入法可能保留剪贴板历史，请勿在不受信环境粘贴密码。"
        }
    }
}