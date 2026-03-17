package com.securevault.security

import android.content.Context

object AppContextHolder {
    @Volatile
    private var context: Context? = null

    fun set(context: Context) {
        this.context = context.applicationContext
    }

    fun get(): Context? = context
}
