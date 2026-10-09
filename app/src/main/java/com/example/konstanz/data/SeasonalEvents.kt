package com.example.konstanz.data

import androidx.annotation.StringRes
import com.example.konstanz.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Holidays, Konstanz events and the four seasons the start screen dresses up for
 * (see ui/splash/SeasonalEffect.kt). [title] names it in the countdown ("Christmas · in 3 days").
 */
enum class SeasonalEvent(@param:StringRes val greeting: Int, @param:StringRes val title: Int, val isSeason: Boolean = false) {
    NewYear(R.string.event_new_year, R.string.event_name_new_year),
    Fasnacht(R.string.event_fasnacht, R.string.event_name_fasnacht),
    Valentine(R.string.event_valentine, R.string.event_name_valentine),
    Easter(R.string.event_easter, R.string.event_name_easter),
    MayDay(R.string.event_may_day, R.string.event_name_may_day),
    Seenachtfest(R.string.event_seenachtfest, R.string.event_name_seenachtfest),
    UnityDay(R.string.event_unity_day, R.string.event_name_unity_day),
    Halloween(R.string.event_halloween, R.string.event_name_halloween),
    StMartin(R.string.event_st_martin, R.string.event_name_st_martin),
    Advent(R.string.event_advent, R.string.event_name_advent),
    Christmas(R.string.event_christmas, R.string.event_name_christmas),
    Winter(R.string.season_winter, R.string.season_winter, isSeason = true),
    Spring(R.string.season_spring, R.string.season_spring, isSeason = true),
    Summer(R.string.season_summer, R.string.season_summer, isSeason = true),
    Autumn(R.string.season_autumn, R.string.season_autumn, isSeason = true),
}

/** What the start screen shows on a day: [event] itself ([daysToGo] = 0) or its countdown (1–7). */
data class SeasonalMoment(val event: SeasonalEvent, val daysToGo: Int = 0)

/** Works out on the phone (no internet) which event or season a day belongs to. */
object SeasonalCalendar {
    /** Events show up this many days ahead, counting down. */
    const val LEAD_DAYS = 7

    /**
     * Seenachtfest moves every year (usually the 2nd Saturday of August) and is published late.
     * Add a year once its date is announced; a missing year simply shows nothing.
     */
    private val SEENACHTFEST = mapOf(
        2025 to LocalDate.of(2025, 8, 9),
        2026 to LocalDate.of(2026, 8, 8),
    )

    private class Occurrence(val event: SeasonalEvent, val start: LocalDate, val end: LocalDate) {
        val days = ChronoUnit.DAYS.between(start, end) + 1
    }

    /**
     * The event on [date]; otherwise the next one starting within [LEAD_DAYS]; otherwise the
     * season, if [includeSeasons].
     * - Of two events on the same day the shorter wins (Valentine's Day over Fasnacht, Christmas over Advent).
     * - A running event beats a countdown, except a long season of its own (Advent): in the week
     *   before Christmas the countdown to Christmas wins.
     */
    fun momentOn(date: LocalDate, includeSeasons: Boolean = true): SeasonalMoment? {
        val all = (date.year - 1..date.year + 1).flatMap(::occurrences)
        val running = all.filter { date in it.start..it.end }.minByOrNull { it.days }
        val next = all.filter { it.start > date && ChronoUnit.DAYS.between(date, it.start) <= LEAD_DAYS }
            .minWithOrNull(compareBy<Occurrence> { it.start }.thenBy { it.days })
        return when {
            running != null && (running.days <= LEAD_DAYS || next == null) -> SeasonalMoment(running.event)
            next != null -> SeasonalMoment(next.event, ChronoUnit.DAYS.between(date, next.start).toInt())
            includeSeasons -> SeasonalMoment(seasonOf(date))
            else -> null
        }
    }

    /** Meteorological seasons: winter December–February, and so on. */
    fun seasonOf(date: LocalDate): SeasonalEvent = when (date.month) {
        Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> SeasonalEvent.Winter
        Month.MARCH, Month.APRIL, Month.MAY -> SeasonalEvent.Spring
        Month.JUNE, Month.JULY, Month.AUGUST -> SeasonalEvent.Summer
        else -> SeasonalEvent.Autumn
    }

    private fun occurrences(year: Int): List<Occurrence> {
        fun day(event: SeasonalEvent, month: Int, d: Int) = LocalDate.of(year, month, d).let { Occurrence(event, it, it) }
        val easter = easterSunday(year)
        return listOfNotNull(
            Occurrence(SeasonalEvent.NewYear, LocalDate.of(year, 12, 31), LocalDate.of(year + 1, 1, 1)),
            // Schmotziger Dunschtig (Thursday before Ash Wednesday) to Fasnachtsdienstag.
            Occurrence(SeasonalEvent.Fasnacht, easter.minusDays(52), easter.minusDays(47)),
            day(SeasonalEvent.Valentine, 2, 14),
            // Good Friday to Easter Monday.
            Occurrence(SeasonalEvent.Easter, easter.minusDays(2), easter.plusDays(1)),
            day(SeasonalEvent.MayDay, 5, 1),
            SEENACHTFEST[year]?.let { Occurrence(SeasonalEvent.Seenachtfest, it, it) },
            day(SeasonalEvent.UnityDay, 10, 3),
            day(SeasonalEvent.Halloween, 10, 31),
            day(SeasonalEvent.StMartin, 11, 11),
            // First Advent Sunday to 23 Dec: Advent and the Christmas market at the lake.
            Occurrence(SeasonalEvent.Advent, firstAdvent(year), LocalDate.of(year, 12, 23)),
            Occurrence(SeasonalEvent.Christmas, LocalDate.of(year, 12, 24), LocalDate.of(year, 12, 26)),
        )
    }

    /** Candles to light on [date]: 0 before Advent, 1 from the first Sunday, 4 from the fourth. */
    fun adventCandles(date: LocalDate): Int {
        val first = firstAdvent(date.year)
        if (date < first) return 0
        return (ChronoUnit.WEEKS.between(first, date).toInt() + 1).coerceAtMost(4)
    }

    /** The 4th Sunday before Christmas Day. */
    fun firstAdvent(year: Int): LocalDate =
        LocalDate.of(year, 12, 24).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).minusWeeks(3)

    /** Gregorian Easter Sunday (anonymous / Meeus algorithm). */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val dayOfMonth = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, dayOfMonth)
    }
}
