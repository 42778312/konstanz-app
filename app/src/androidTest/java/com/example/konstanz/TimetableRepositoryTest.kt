package com.example.konstanz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.konstanz.data.RoutePreference
import com.example.konstanz.data.timetable.DatabaseTransitRepository
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.Leg
import com.example.konstanz.data.transit.Minutes
import com.example.konstanz.data.transit.SearchFilter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** Runs against the shipped assets/timetable.db (a Thursday morning inside the feed's validity). */
@RunWith(AndroidJUnit4::class)
class TimetableRepositoryTest {
    private val repo = DatabaseTransitRepository(
        ApplicationProvider.getApplicationContext(),
        clock = { LocalDateTime.of(2026, 10, 1, 8, 0) },
    )

    @Test
    fun shippedDatabaseOpensWithRealStops() = runBlocking {
        val stops = repo.stops()
        assertTrue("stations: ${stops.size}", stops.size > 100)
        val bahnhof = requireNotNull(repo.stop("bahnhof"))
        assertEquals("Konstanz Bahnhof", bahnhof.name)
        assertTrue(bahnhof.lines.toString(), "5" in bahnhof.lines && "S6" in bahnhof.lines)
        assertNotNull(repo.stop("universitaet"))
    }

    @Test
    fun departuresAreSortedAndUpcoming() = runBlocking {
        val deps = repo.departures("bahnhof", from = Minutes.of(8, 0), limit = 10)
        assertEquals(10, deps.size)
        assertTrue(deps.all { it.scheduled >= Minutes.of(8, 0) })
        assertEquals(deps.sortedBy { it.scheduled }, deps)
        val line5 = repo.departures("bahnhof", from = Minutes.of(8, 0), limit = 3, line = "5")
        assertTrue(line5.isNotEmpty() && line5.all { it.line == "5" })
    }

    @Test
    fun tripShowsItsStops() = runBlocking {
        val dep = repo.departures("bahnhof", from = Minutes.of(8, 0), limit = 1).single()
        val trip = requireNotNull(repo.trip(dep.tripId))
        assertTrue(trip.stops.size >= 2)
        assertTrue(trip.stops.any { it.stopId == "bahnhof" })
    }

    @Test
    fun routesBahnhofToUniversitaet() = runBlocking {
        val options = repo.journeys("Konstanz Bahnhof", "Universität", RoutePreference.Fastest, departAt = Minutes.of(8, 0))
        assertTrue("no journeys", options.isNotEmpty())
        val best = options.first()
        assertTrue(best.rides.isNotEmpty())
        assertTrue(best.start >= Minutes.of(8, 0))
        assertTrue("takes ${best.minutes} min", best.minutes in 5..45)
        // Legs are continuous in time.
        best.legs.zipWithNext().forEach { (a, b) -> assertTrue("$a → $b", b.start >= a.end) }
        assertEquals(best, repo.journey(best.id))
        // Rides follow the bus's street path, not a straight stop-to-stop line.
        best.rides.forEach { ride ->
            val points = ride.path!!.count { it == 'M' || it == 'L' }
            assertTrue("${ride.line}: $points points for ${ride.stops.size} stops", points > ride.stops.size + 2)
        }
    }

    @Test
    fun searchFindsRealStops() = runBlocking {
        val results = repo.search("univers", SearchFilter.Stops)
        assertTrue(results.stops.map { it.first.name }.toString(), results.stops.any { it.first.id == "universitaet" })
    }

    @Test
    fun realPlacesAndDesignIds() = runBlocking {
        val uni = requireNotNull(repo.place("uni")) // the design's id, used by the seeded saved place
        assertEquals("Universität Konstanz", uni.name)
        val (lat, lon) = Geo.latLon(uni.point)
        assertEquals(47.690, lat, 0.003)
        assertEquals(9.188, lon, 0.003)
        assertEquals(uni, repo.placeByName("Universität Konstanz"))
    }

    @Test
    fun searchFindsPlacesAndStreets() = runBlocking {
        val results = repo.search("therme", SearchFilter.All)
        assertTrue(results.places.map { it.name }.toString(), results.places.any { it.name == "Bodensee-Therme Konstanz" })
        assertTrue(results.addresses.map { it.name }.toString(), results.addresses.any { it.name == "Zur Therme" })
        assertTrue(results.stops.any { it.first.name == "Bodensee-Therme" })
    }

    @Test
    fun whatsHereNamesStreetAndTown() = runBlocking {
        val info = repo.locationAt(Geo.mapPoint(47.68830, 9.18260)) // on Universitätsstraße
        assertTrue(info.name, info.name.startsWith("Universitätsstraße"))
        assertEquals("Konstanz, Germany", info.region)
        val swiss = repo.locationAt(Geo.mapPoint(47.6480, 9.1750))
        assertTrue(swiss.region, swiss.region.endsWith("Switzerland"))
    }

    @Test
    fun cityStopRegisterAddsDirectionsAndFullNames() = runBlocking {
        // "Sternenpl./Spanierstr." in the timetable, spelled out by the city's register.
        assertNotNull(repo.stopByName("Sternenplatz/Spanierstraße"))
        val deps = repo.departures("sternenplatz", from = Minutes.of(8, 0), limit = 10)
        assertTrue(deps.map { it.side }.toString(), deps.any { it.side == "Out of town" || it.side?.startsWith("Towards") == true })
    }

    @Test
    fun arriveByKeepsTheDeadline() = runBlocking {
        val deadline = Minutes.of(9, 0)
        val options = repo.journeys("Konstanz Bahnhof", "Universität", RoutePreference.Fastest, departAt = deadline, arriveBy = true)
        assertTrue("no journeys", options.isNotEmpty())
        assertTrue(options.map { it.end.format() }.toString(), options.all { it.end <= deadline })
        // The first one leaves as late as possible, but not absurdly early.
        assertTrue(options.first().start >= Minutes.of(8, 0))
    }

    @Test
    fun shortTripsOfferWalking() = runBlocking {
        // Bahnhof → Sternenplatz is about 1 km through the old town.
        val options = repo.journeys("Konstanz Bahnhof", "Sternenplatz", RoutePreference.Fastest, departAt = Minutes.of(8, 0))
        val walk = options.firstOrNull { it.rides.isEmpty() }
        assertNotNull(options.map { it.legs.size }.toString(), walk)
        assertTrue(walk!!.minutes in 5..25)
    }

    @Test
    fun rideThisBusFollowsTheTrip() = runBlocking {
        val dep = repo.departures("bahnhof", from = Minutes.of(8, 0), limit = 1).single()
        val ride = requireNotNull(repo.rideJourney(dep.tripId, "bahnhof"))
        assertEquals(1, ride.legs.size)
        val leg = ride.rides.single()
        assertEquals(dep.line, leg.line)
        assertEquals(dep.tripId, leg.tripId)
        assertTrue(leg.path!!.count { it == 'L' } > leg.stops.size)
    }

    @Test
    fun tomorrowUsesTomorrowsTimetable() = runBlocking {
        // 1 Oct 2026 is a Thursday; +2 days is Saturday with its own timetable.
        val saturday = repo.journeys("Konstanz Bahnhof", "Universität", RoutePreference.Fastest, departAt = Minutes.of(8, 0), dayOffset = 2)
        assertTrue(saturday.isNotEmpty())
    }
}
