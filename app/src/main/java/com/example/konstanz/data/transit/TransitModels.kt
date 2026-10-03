package com.example.konstanz.data.transit

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import java.util.Locale

// Transit domain models the screens work with. They don't depend on where the data comes from:
// today MockTransitRepository fills them from the design, later the local timetable database.
// No Android or Compose imports here (Architecture artboard: data layer has no UI imports).

/** A position on the map, in the design's map units (see ui/map). Real coordinates come with the database. */
data class MapPoint(val x: Float, val y: Float)

/** Clock time as minutes after midnight; values ≥ 24:00 are after-midnight trips of the previous service day. */
@JvmInline
value class Minutes(val value: Int) : Comparable<Minutes> {
    override fun compareTo(other: Minutes) = value.compareTo(other.value)
    operator fun plus(minutes: Int) = Minutes(value + minutes)
    operator fun minus(other: Minutes) = value - other.value

    /** "14:05" — always two-digit hours, tabular in the UI. */
    fun format(): String {
        val m = ((value % (24 * 60)) + 24 * 60) % (24 * 60)
        return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)
    }

    companion object {
        fun of(hours: Int, minutes: Int) = Minutes(hours * 60 + minutes)
    }
}

/** A bus line, e.g. "12" Bahnhof ↔ Wollmatingen. */
data class Line(val id: String, val name: String, val terminusA: String, val terminusB: String)

data class Stop(
    val id: String,
    val name: String,
    /** Lines calling here, in display order. */
    val lines: List<String>,
    val point: MapPoint,
    /** "A–D" when the stop has several platforms. */
    val platforms: String? = null,
)

/**
 * Realtime knowledge about one trip (design system → "Transit badges & realtime").
 * Every state is shown with an icon and a word; only [OnTime], [Live] and [Delayed] may colour times green.
 */
sealed interface Realtime {
    /** Live feed, under 2 min old, no delay. */
    data object OnTime : Realtime
    /** Live feed, running late. */
    data class Delayed(val minutes: Int) : Realtime
    data object Cancelled : Realtime
    data object Detour : Realtime
    /** Feed 2–15 min old. */
    data class LastKnown(val minutesAgo: Int) : Realtime
    /** Feed over 15 min old — treated like scheduled. */
    data class Stale(val minutesOld: Int) : Realtime
    /** Timetable only for this trip. */
    data object Scheduled : Realtime
    /** The operator publishes no live data. */
    data object NoRealtime : Realtime

    val delayMinutes: Int get() = if (this is Delayed) minutes else 0
    val isLive: Boolean get() = this == OnTime || this is Delayed
}

/** One departure of a trip from a stop. */
data class StopDeparture(
    val tripId: String,
    val line: String,
    val destination: String,
    val scheduled: Minutes,
    val realtime: Realtime,
    val platform: String? = null,
    /** Which way buses leave from this platform, "Towards city centre". */
    val side: String? = null,
) {
    /** Expected departure (scheduled + delay). */
    val expected: Minutes get() = scheduled + realtime.delayMinutes
}

/** A stop along a trip (Departure details, Bus journey). */
data class TripStop(val stopId: String, val name: String, val scheduled: Minutes, val expected: Minutes)

/** A single run of a line with all its stops. */
data class Trip(
    val id: String,
    val line: String,
    val destination: String,
    val platform: String?,
    val realtime: Realtime,
    val stops: List<TripStop>,
)

data class ServiceAlert(val lines: List<String>, val title: String, val message: String)

/** Freshness of realtime data for a stop ("Live · updated 30 s ago"). [updatedSecondsAgo] null = never received. */
data class RealtimeInfo(val available: Boolean, val updatedSecondsAgo: Int?)

enum class PlaceKind(@param:androidx.annotation.StringRes private val text: Int) {
    University(R.string.kind_university), Library(R.string.kind_library), Station(R.string.kind_station), Harbour(R.string.kind_harbour),
    Venue(R.string.kind_venue), Square(R.string.kind_square), Street(R.string.street), Address(R.string.kind_address),
    Food(R.string.kind_food), Cafe(R.string.kind_cafe), Shop(R.string.kind_shop), Health(R.string.kind_health), Education(R.string.kind_education),
    Culture(R.string.kind_culture), Park(R.string.kind_park), Leisure(R.string.kind_leisure), Hotel(R.string.kind_hotel), Transport(R.string.kind_transport),
    Service(R.string.kind_service), District(R.string.kind_district), Town(R.string.kind_town);

    val label: String get() = Texts.get(text)
}

/** Something you can travel to that is not a stop. */
data class Place(
    val id: String,
    val name: String,
    val kind: PlaceKind,
    /** "Universitätsstraße 10, 78464 Konstanz". */
    val address: String,
    val point: MapPoint,
    /** Distance from the user, e.g. 3.1 km. */
    val distanceKm: Double,
    /** What it is in words, "Museum"; null = the kind's label. */
    val category: String? = null,
)

/** A stop near a place with the walking time to it (Location details → Nearby stops). */
data class NearbyStop(val stop: Stop, val walkMinutes: Int)

/** What is at a map point (artboard 10 → Selected location). */
data class LocationInfo(
    val point: MapPoint,
    /** "Paradies, Konstanz". */
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** "Konstanz, Germany". */
    val region: String,
    val nearestStop: NearbyStop?,
)

/** Search is grouped like the design: places, bus stops, addresses. */
data class SearchResults(
    val places: List<Place>,
    val stops: List<Pair<Stop, Double>>,
    val addresses: List<Place>,
    /** Everything in one list, best match first (what "All" shows). Empty = use the three lists. */
    val top: List<SearchHit> = emptyList(),
    /** "Cafés nearby" suggestions for what's typed ("caf"). */
    val categories: List<SearchCategory> = emptyList(),
    /** The best answer only matched with a typo: "Did you mean Bahnhofplatz?". */
    val didYouMean: String? = null,
) {
    val isEmpty get() = places.isEmpty() && stops.isEmpty() && addresses.isEmpty() && top.isEmpty()
}

/** One search answer: a place (incl. streets and addresses) or a bus stop, with its distance. */
data class SearchHit(val place: Place?, val stop: Stop?, val distanceKm: Double)

/** A kind of place to list by distance ("Pharmacies"); [key] is the word places are indexed under. */
data class SearchCategory(val key: String, val label: String, val group: String)

enum class SearchFilter { All, Places, Stops, Addresses }

// ---------- Journeys ----------

/** One turn-by-turn step while walking. */
/** What to do at the start of a walking step (the icon in the directions). */
enum class Maneuver { Depart, Straight, SlightLeft, SlightRight, Left, Right, SharpLeft, SharpRight, UTurn, Stairs, Cross, Arrive }

/** One step of walking directions: [instruction], then walk [meters]; [at] = where the step starts on the map. */
data class WalkStep(
    val instruction: String,
    val meters: Int,
    val street: String? = null,
    val maneuver: Maneuver = Maneuver.Straight,
    val at: MapPoint? = null,
)

/** Where to wait: "Platform 1 · Towards city centre", "Towards city centre", "Platform A"; null = unknown. */
fun platformLabel(platform: String?, side: String?): String? =
    listOfNotNull(platform?.let { Texts.get(R.string.platform_x, it) }, side).joinToString(" · ").ifEmpty { null }

/** Every coordinate pair of an SVG path (end and control points), for fitting the camera around it. */
fun pathPoints(path: String): List<MapPoint> =
    Regex("-?\\d+(?:\\.\\d+)?").findAll(path).map { it.value.toFloat() }.chunked(2)
        .filter { it.size == 2 }.map { MapPoint(it[0], it[1]) }.toList()

/** First and last point of an SVG path in map units ("M612 902 C… 660 868" → 612,902 / 660,868). */
fun pathEnds(path: String): Pair<MapPoint, MapPoint>? {
    val n = Regex("-?\\d+(?:\\.\\d+)?").findAll(path).map { it.value.toFloat() }.toList()
    if (n.size < 4) return null
    return MapPoint(n[0], n[1]) to MapPoint(n[n.size - 2], n[n.size - 1])
}

sealed interface Leg {
    val start: Minutes
    val end: Minutes
    val minutes: Int get() = end - start
    /** Shape on the map as SVG path data in map units (M/L/C), for drawing the route. */
    val path: String?

    data class Walk(
        override val start: Minutes,
        override val end: Minutes,
        val meters: Int,
        /** Where this walk ends, e.g. "Konstanz Bahnhof". */
        val to: String,
        val steps: List<WalkStep> = emptyList(),
        override val path: String? = null,
    ) : Leg

    data class Ride(
        override val start: Minutes,
        override val end: Minutes,
        val line: String,
        val direction: String,
        val from: String,
        val platform: String?,
        val to: String,
        val realtime: Realtime,
        /** Stops after boarding, the last one is where you get off. */
        val stops: List<TripStop>,
        override val path: String? = null,
        /** Which way buses leave from the boarding platform, "Towards city centre". */
        val side: String? = null,
        /** The trip ridden, for its details page; null in the design sample. */
        val tripId: String? = null,
        /** Stop id where you board. */
        val fromStopId: String? = null,
    ) : Leg
}

data class Journey(
    val id: String,
    val from: String,
    val to: String,
    val legs: List<Leg>,
    /** Label on the best option, e.g. "Fastest". */
    val badge: String? = null,
) {
    val start: Minutes get() = legs.first().start
    val end: Minutes get() = legs.last().end
    val minutes: Int get() = end - start
    val rides: List<Leg.Ride> get() = legs.filterIsInstance<Leg.Ride>()
    val transfers: Int get() = (rides.size - 1).coerceAtLeast(0)
    val walkingMinutes: Int get() = legs.filterIsInstance<Leg.Walk>().sumOf { it.minutes }
    val firstRide: Leg.Ride? get() = rides.firstOrNull()
}

/** Offline data bundle status (Settings, Offline data, Sync). */
data class DataStatus(
    val upToDate: Boolean,
    val version: Int,
    val lastSync: String,
    val savedAt: String,
    val storage: String,
)
