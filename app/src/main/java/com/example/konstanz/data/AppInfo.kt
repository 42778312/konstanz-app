package com.example.konstanz.data

import android.content.Context
import android.content.pm.ApplicationInfo

/** Facts about this installation, shown in Settings and About. */
object AppInfo {
    /** Shown in the privacy policy and used for "Send feedback"; null hides both until it is set. */
    val SUPPORT_EMAIL: String? = null

    /** Change when the privacy policy text changes (ui/about/PrivacyScreen.kt). */
    const val PRIVACY_POLICY_DATE = "30 September 2026"

    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"

    fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
