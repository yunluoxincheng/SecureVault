package com.securevault.autofill.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.securevault.security.KeyManager
import org.koin.core.context.GlobalContext

class AutofillAuthActivity : Activity() {
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertDialog.Builder(this)
            .setTitle("保险库已锁定")
            .setMessage("请先打开 SecureVault 解锁后再进行自动填充。")
            .setNegativeButton("取消") { _, _ -> finish() }
            .setPositiveButton("打开保险库") { _, _ ->
                // Keep immediate-lock mode from re-locking during the short autofill app roundtrip.
                keyManager.allowImmediateBackgroundLockBypass(durationMs = 90_000L)
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }
}
