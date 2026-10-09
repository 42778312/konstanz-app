package com.example.konstanz.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SeasonalCalendarTest {
    private fun on(y: Int, m: Int, d: Int, seasons: Boolean = false) =
        SeasonalCalendar.momentOn(LocalDate.of(y, m, d), includeSeasons = seasons)

    private fun today(event: SeasonalEvent) = SeasonalMoment(event)
    private fun ahead(event: SeasonalEvent, days: Int) = SeasonalMoment(event, days)

    @Test
    fun easterSunday() {
        assertEquals(LocalDate.of(2025, 4, 20), SeasonalCalendar.easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), SeasonalCalendar.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), SeasonalCalendar.easterSunday(2027))
    }

    @Test
    fun eventsCountDownAWeekAhead() {
        assertNull(on(2026, 10, 23))
        assertEquals(ahead(SeasonalEvent.Halloween, 7), on(2026, 10, 24))
        assertEquals(ahead(SeasonalEvent.Halloween, 1), on(2026, 10, 30))
        assertEquals(today(SeasonalEvent.Halloween), on(2026, 10, 31))
        assertEquals(ahead(SeasonalEvent.UnityDay, 7), on(2026, 9, 26))
        assertEquals(ahead(SeasonalEvent.Seenachtfest, 3), on(2026, 8, 5))
    }

    @Test
    fun easterRunsFromGoodFridayToEasterMonday() {
        assertEquals(ahead(SeasonalEvent.Easter, 1), on(2026, 4, 2))
        assertEquals(today(SeasonalEvent.Easter), on(2026, 4, 3))
        assertEquals(today(SeasonalEvent.Easter), on(2026, 4, 6))
        assertNull(on(2026, 4, 7))
    }

    @Test
    fun fasnachtRunsFromSchmotzigerDunschtigToTuesday() {
        assertEquals(ahead(SeasonalEvent.Fasnacht, 1), on(2026, 2, 11))
        assertEquals(today(SeasonalEvent.Fasnacht), on(2026, 2, 12))
        assertEquals(today(SeasonalEvent.Fasnacht), on(2026, 2, 17))
        // Ash Wednesday: Valentine's Day is next.
        assertNull(on(2026, 2, 18))
    }

    @Test
    fun adventAndTheWeekBeforeChristmas() {
        assertEquals(LocalDate.of(2025, 11, 30), SeasonalCalendar.firstAdvent(2025))
        assertEquals(LocalDate.of(2026, 11, 29), SeasonalCalendar.firstAdvent(2026))
        assertEquals(ahead(SeasonalEvent.Advent, 1), on(2026, 11, 28))
        assertEquals(today(SeasonalEvent.Advent), on(2026, 11, 29))
        assertEquals(today(SeasonalEvent.Advent), on(2026, 12, 16))
        // In the last week the countdown to Christmas beats the long Advent season.
        assertEquals(ahead(SeasonalEvent.Christmas, 7), on(2026, 12, 17))
        assertEquals(ahead(SeasonalEvent.Christmas, 1), on(2026, 12, 23))
        assertEquals(0, SeasonalCalendar.adventCandles(LocalDate.of(2026, 11, 28)))
        assertEquals(1, SeasonalCalendar.adventCandles(LocalDate.of(2026, 12, 5)))
        assertEquals(2, SeasonalCalendar.adventCandles(LocalDate.of(2026, 12, 6)))
        assertEquals(4, SeasonalCalendar.adventCandles(LocalDate.of(2026, 12, 20)))
    }

    @Test
    fun christmasThenNewYear() {
        assertEquals(today(SeasonalEvent.Christmas), on(2026, 12, 24))
        assertEquals(today(SeasonalEvent.Christmas), on(2026, 12, 26))
        assertEquals(ahead(SeasonalEvent.NewYear, 4), on(2026, 12, 27))
        assertEquals(today(SeasonalEvent.NewYear), on(2026, 12, 31))
        assertEquals(today(SeasonalEvent.NewYear), on(2027, 1, 1))
        assertNull(on(2027, 1, 2))
    }

    @Test
    fun shorterRunningEventWins() {
        // Fasnacht 2021 ran 11–16 Feb.
        assertEquals(today(SeasonalEvent.Fasnacht), on(2021, 2, 13))
        assertEquals(today(SeasonalEvent.Valentine), on(2021, 2, 14))
        assertEquals(today(SeasonalEvent.Fasnacht), on(2021, 2, 15))
    }

    @Test
    fun seasonsFillTheRest() {
        assertEquals(today(SeasonalEvent.Autumn), on(2026, 10, 9, seasons = true))
        assertEquals(today(SeasonalEvent.Winter), on(2027, 1, 20, seasons = true))
        assertEquals(today(SeasonalEvent.Spring), on(2026, 3, 10, seasons = true))
        assertEquals(today(SeasonalEvent.Summer), on(2026, 7, 1, seasons = true))
        // Events still come first.
        assertEquals(ahead(SeasonalEvent.Halloween, 5), on(2026, 10, 26, seasons = true))
    }

    @Test
    fun seenachtfestOnlyInKnownYears() {
        assertEquals(today(SeasonalEvent.Seenachtfest), on(2026, 8, 8))
        assertNull(on(2026, 8, 9))
        assertNull(on(2030, 8, 10))
    }
}
