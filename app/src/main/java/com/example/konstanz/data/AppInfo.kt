package com.example.konstanz.data

import android.content.Context
import android.content.pm.ApplicationInfo

/** Facts about this installation, shown in Settings and About. */
object AppInfo {
    /** Shown in the privacy policy and used for "Send feedback"; null hides both until it is set. */
    val SUPPORT_EMAIL: String? = "konstant@werkflow.cc"

    /** Change when the privacy policy text changes (ui/about/PrivacyScreen.kt). */
    const val PRIVACY_POLICY_DATE = "11 October 2026"

    /**
     * The original sources of the timetable, stops and map, linked in the privacy policy. Google Play
     * requires links to the official source of public-sector data (Misleading Claims policy).
     */
    const val SOURCE_TIMETABLE = "https://www.nvbw.de/open-data/fahrplandaten/fahrplandaten-mit-liniennetz"
    const val SOURCE_STOPS = "https://offenedaten-konstanz.de"
    const val SOURCE_MAP = "https://www.openstreetmap.org/copyright"
    const val SOURCE_OFFICIAL_BUS = "https://www.stadtwerke-konstanz.de/mobilitaet/bus"

    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"

    fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
