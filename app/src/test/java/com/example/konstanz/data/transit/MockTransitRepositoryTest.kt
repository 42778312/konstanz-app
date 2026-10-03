package com.example.konstanz.data.transit

import com.example.konstanz.data.RoutePreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The mock must return what the design artboards show, so screens built on it match the design. */
class MockTransitRepositoryTest {

    private val repo = MockTransitRepository(realtimeCheckMs = 0)

    @Test fun minutesFormatWithTwoDigits() {
        assertEquals("14:05", Minutes.of(14, 5).format())
        assertEquals("00:10", Minutes.of(24, 10).format()) // after-midnight trip
    }

    @Test fun bahnhofNextDeparturesMatchStopSheet() = runBlocking {
        // Artboard 12 leaves the cancelled 14:50 out of "Next departures".
        val next = repo.departures("bahnhof", limit = 3, includeCancelled = false)
        assertEquals(listOf("14:40", "14:44", "14:55"), next.map { it.scheduled.format() })
        assertEquals(listOf("12", "5", "8"), next.map { it.line })
        // 14:44 +3 → expected 14:47, as on artboard 12.
        assertEquals("14:47", next[1].expected.format())
    }

    @Test fun bahnhofShowsEveryRealtimeState() = runBlocking {
        val states = repo.departures("bahnhof", limit = 20).map { it.realtime::class }.toSet()
        listOf(Realtime.OnTime::class, Realtime.Delayed::class, Realtime.Cancelled::class, Realtime.Detour::class,
            Realtime.LastKnown::class, Realtime.Stale::class, Realtime.Scheduled::class)
            .forEach { assertTrue("missing $it", it in states) }
    }

    @Test fun lineFilter() = runBlocking {
        val only12 = repo.departures("bahnhof", limit = 20, line = "12")
        assertTrue(only12.isNotEmpty())
        assertTrue(only12.all { it.line == "12" })
    }

    @Test fun everyStopHasDepartures() = runBlocking {
        repo.stops().forEach { stop ->
            assertTrue("no departures at ${stop.name}", repo.departures(stop.id).isNotEmpty())
        }
    }

    @Test fun tripOfLine12MatchesDepartureDetails() = runBlocking {
        val trip = repo.trip("t12-1440")
        assertNotNull(trip)
        assertEquals(
            listOf("Konstanz Bahnhof", "Marktstätte", "Laube", "Benediktinerplatz", "Zähringerplatz", "Fürstenberg", "Wollmatingen"),
            trip!!.stops.map { it.name },
        )
        assertEquals("15:00", trip.stops.last().scheduled.format())
    }

    @Test fun generatedTripStartsAtItsStop() = runBlocking {
        val dep = repo.departures("universitaet-sued").first()
        val trip = repo.trip(dep.tripId)!!
        assertEquals("Universität Süd", trip.stops.first().name)
    }

    @Test fun searchUnivMatchesSearchResultsArtboard() = runBlocking {
        val r = repo.search("Univ")
        assertEquals(listOf("Universität Konstanz", "Universitätsbibliothek"), r.places.map { it.name })
        assertEquals(listOf("Universität", "Universität Süd"), r.stops.map { it.first.name })
        assertEquals(listOf("Universitätsstraße"), r.addresses.map { it.name })
    }

    @Test fun searchFilterAndEmptyQuery() = runBlocking {
        assertTrue(repo.search("Univ", SearchFilter.Stops).places.isEmpty())
        assertTrue(repo.search("  ").isEmpty)
    }

    @Test fun journeysMatchRouteResults() = runBlocking {
        val options = repo.journeys("My location", "Universität Konstanz")
        assertEquals(listOf(16, 23, 22), options.map { it.minutes })
        val fastest = options.first()
        assertEquals(RoutePreference.Fastest.label, fastest.badge)
        assertEquals("14:32", fastest.start.format())
        assertEquals(0, fastest.transfers)
        assertEquals(7, fastest.walkingMinutes)
        assertEquals(1, options[1].transfers)
        assertEquals(14, options[2].walkingMinutes)
    }

    @Test fun leastWalkingReorders() = runBlocking {
        val options = repo.journeys("My location", "Universität Konstanz", RoutePreference.LeastWalking)
        assertEquals(6, options.first().walkingMinutes)
        assertEquals(RoutePreference.LeastWalking.label, options.first().badge)
    }

    @Test fun bahnhofIsFourMinutesWalk() {
        // Artboard 12: "Bus stop · Platforms A–D · 4 min walk".
        val bahnhof = MockTransitData.stops.first { it.id == "bahnhof" }
        assertEquals(4, repo.walkMinutes(repo.myLocation(), bahnhof.point))
    }

    @Test fun pickedPointNamesDistrictAndNearestStop() = runBlocking {
        val info = repo.locationAt(MapPoint(470f, 820f))
        assertEquals("Paradies, Konstanz", info.name)
        assertEquals("Paradies", info.nearestStop?.stop?.name)
    }

    @Test fun nightDepartureHasNoRoute() = runBlocking {
        // Artboard 30: "No route … at 00:40".
        assertTrue(repo.journeys("My location", "Universität Konstanz", departAt = Minutes.of(0, 40)).isEmpty())
        assertTrue(repo.journeys("My location", "Universität Konstanz", departAt = Minutes.of(14, 30)).isNotEmpty())
    }

    @Test fun everyLegHasAShape() = runBlocking {
        repo.journeys("My location", "Universität Konstanz").flatMap { it.legs }.forEach { assertNotNull(it.path) }
    }

    @Test fun unreachableDestinationHasNoRoute() = runBlocking {
        assertTrue(repo.journeys("My location", "Kreuzlingen Hafen").isEmpty())
    }
}
