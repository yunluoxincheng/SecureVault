package com.securevault.autofill

object AutofillBlocklist {
    private val blockedPackages = setOf(
        "com.securevault",
        "com.lastpass.lpandroid",
        "com.x8bit.bitwarden",
        "com.agilebits.onepassword",
        "com.dashlane",
        "com.callpod.android_apps.keeper",
        "io.enpass.app",
        "com.android.settings",
        "com.android.systemui",
        "com.android.shell",
        "com.android.adb",
    )

    fun isBlocked(packageName: String): Boolean = packageName in blockedPackages
}
