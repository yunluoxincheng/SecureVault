package com.securevault.security

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

object AndroidActivityProvider {
    @Volatile
    private var activityRef: WeakReference<FragmentActivity>? = null

    fun set(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
    }

    fun clear(activity: FragmentActivity) {
        val current = activityRef?.get()
        if (current == activity) {
            activityRef = null
        }
    }

    fun get(): FragmentActivity? = activityRef?.get()
}
