package com.securevault.autofill.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

class AutofillAuthActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertDialog.Builder(this)
            .setTitle("保险库已锁定")
            .setMessage("请先打开 SecureVault 解锁后再进行自动填充。")
            .setNegativeButton("取消") { _, _ -> finish() }
            .setPositiveButton("打开保险库") { _, _ ->
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
