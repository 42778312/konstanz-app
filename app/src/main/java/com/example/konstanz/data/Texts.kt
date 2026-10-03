package com.example.konstanz.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Words made outside the UI (route steps, summaries, statuses) in the phone's language
 * (res/values = English, values-de = German). Screens use stringResource instead.
 */
object Texts {
    // The application context only (never an activity), so nothing leaks.
    @SuppressLint("StaticFieldLeak")
    private lateinit var app: Context

    fun init(context: Context) { app = context.applicationContext }

    /** Before [init] (unit tests): the resource name, so nothing crashes. */
    fun get(@StringRes id: Int, vararg args: Any): String =
        if (::app.isInitialized) app.getString(id, *args) else "#$id"

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        if (::app.isInitialized) app.resources.getQuantityString(id, count, *args) else "#$id"
}
