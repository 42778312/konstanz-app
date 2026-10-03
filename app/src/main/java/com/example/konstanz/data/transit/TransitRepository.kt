package com.example.konstanz.data.transit

import com.example.konstanz.data.RoutePreference

/**
 * Everything the screens need to know about the network. Plain suspend functions, no UI types.
 * Implementations: [MockTransitRepository] now, a local-database one later (same interface).
 */
interface TransitRepository {
    /** "Now" for the timetable. The mock freezes it at the design's 14:30. */
    fun now(): Minutes

    suspend fun lines(): List<Line>
    suspend fun line(id: String): Line?

    suspend fun stops(): List<Stop>
    suspend fun stop(id: String): Stop?
    suspend fun stopByName(name: String): Stop?

    /**
     * Next departures at a stop from [from]. Summaries ("Next departures") leave cancelled trips out;
     * the full list keeps them, struck through (artboards 12–14).
     */
    suspend fun departures(
        stopId: String,
        from: Minutes = now(),
        limit: Int = 10,
        line: String? = null,
        includeCancelled: Boolean = true,
    ): List<StopDeparture>

    suspend fun trip(tripId: String): Trip?
    suspend fun linesAt(stopId: String): List<Line>
    suspend fun alertsAt(stopId: String): List<ServiceAlert>
    /** Live data status for a stop. May take a moment (network); the timetable never waits for it. */
    suspend fun realtimeInfo(stopId: String): RealtimeInfo

    /** Where the user is. */
    fun myLocation(): MapPoint

    /** Walking time between two points, whole minutes (at least 1). */
    fun walkMinutes(from: MapPoint, to: MapPoint): Int

    suspend fun places(): List<Place>
    suspend fun place(id: String): Place?
    suspend fun placeByName(name: String): Place?
    suspend fun nearbyStops(point: MapPoint, limit: Int = 3): List<NearbyStop>
    /** Describe a point the user picked on the map (reverse geocoding, offline). */
    suspend fun locationAt(point: MapPoint): LocationInfo

    suspend fun search(query: String, filter: SearchFilter = SearchFilter.All): SearchResults

    /** Places of one kind by distance from the user ("Cafés nearby"). */
    suspend fun nearby(category: SearchCategory): List<SearchHit> = emptyList()

    /** Category shortcuts for the empty search screen. */
    fun searchShortcuts(): List<SearchCategory> = emptyList()

    /** Route options, best first. Empty = no route (artboard 30). */
    suspend fun journeys(
        from: String,
        to: String,
        preference: RoutePreference = RoutePreference.Fastest,
        /** Departure time (or arrival time with [arriveBy]); null = now. */
        departAt: Minutes? = null,
        /** [departAt] is the latest arrival ("Arrive by"). */
        arriveBy: Boolean = false,
        /** 0 = today, 1 = tomorrow … */
        dayOffset: Int = 0,
    ): List<Journey>
    suspend fun journey(id: String): Journey?

    /**
     * One bus trip as a journey ("Show on map"): [tripId] from [fromStopId] (null = its first stop)
     * to its last stop, as a journey with a single ride. Id "ride:<trip>:<stop>".
     */
    suspend fun rideJourney(tripId: String, fromStopId: String? = null, at: Int? = null): Journey? {
        val trip = trip(tripId) ?: return null
        val passes = trip.stops.indices.filter { trip.stops[it].stopId == fromStopId }
        val start = passes.firstOrNull { trip.stops[it].scheduled.value == at } ?: passes.firstOrNull() ?: 0
        val ridden = trip.stops.drop(start)
        if (ridden.size < 2) return null
        val here = ridden.first()
        val last = ridden.last()
        val points = ridden.mapNotNull { stop(it.stopId)?.point }
        val ride = Leg.Ride(
            here.expected, last.expected, trip.line, trip.destination, here.name, trip.platform, last.name,
            trip.realtime, ridden.drop(1),
            path = points.takeIf { it.size >= 2 }?.mapIndexed { i, p -> "${if (i == 0) "M" else "L"}${p.x} ${p.y}" }?.joinToString(" "),
            tripId = tripId, fromStopId = here.stopId,
        )
        return Journey("ride:$tripId:${here.stopId}", here.name, last.name, listOf(ride))
    }

    suspend fun dataStatus(): DataStatus
}
