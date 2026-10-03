package com.example.konstanz.ui.plan

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import com.example.konstanz.data.RoutePreference
import com.example.konstanz.data.transit.Minutes
import java.util.Calendar
import java.util.Locale

/** "Now" / "Depart at" / "Arrive by" (artboard 17). */
enum class TimeMode(@param:androidx.annotation.StringRes private val text: Int) {
    Now(R.string.time_now), DepartAt(R.string.time_depart_at), ArriveBy(R.string.time_arrive_by);

    val label: String get() = Texts.get(text)
}

/** Everything the planner asks for besides From / To. */
data class PlanOptions(
    val timeMode: TimeMode = TimeMode.Now,
    /** Days from today (0 = today). */
    val dayOffset: Int = 0,
    val time: Minutes,
    val preference: RoutePreference = RoutePreference.Fastest,
) {
    /** "Now", "Depart 14:30", "Arrive Thu 1 15:00". */
    fun timeLabel(): String = when (timeMode) {
        TimeMode.Now -> Texts.get(R.string.time_now)
        TimeMode.DepartAt -> Texts.get(R.string.time_depart, "${dayPrefix()}${time.format()}")
        TimeMode.ArriveBy -> Texts.get(R.string.time_arrive, "${dayPrefix()}${time.format()}")
    }

    /** "Now · Fastest" under the route bar. */
    fun summary(): String = "${timeLabel()} · ${preference.label}"

    /** "Today 14:30" on the Find routes button ("Now" → the current time). */
    fun whenLabel(now: Minutes): String =
        "${dayLabel(if (timeMode == TimeMode.Now) 0 else dayOffset)} ${(if (timeMode == TimeMode.Now) now else time).format()}"

    private fun dayPrefix() = if (dayOffset == 0) "" else "${dayLabel(dayOffset)} "
}

/** "Today", else "Thu 1" (short weekday + day of month), relative to today. */
fun dayLabel(offset: Int): String {
    if (offset == 0) return Texts.get(R.string.today)
    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
    val weekday = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
    return "$weekday ${c.get(Calendar.DAY_OF_MONTH)}"
}
