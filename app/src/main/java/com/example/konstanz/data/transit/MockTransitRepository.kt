package com.example.konstanz.data.transit

import com.example.konstanz.data.RoutePreference
import kotlinx.coroutines.delay
import kotlin.math.hypot

/**
 * [TransitRepository] backed by [MockTransitData]. Konstanz Bahnhof returns the design's exact
 * departures; every other stop gets a regular timetable generated from the line patterns, so
 * any stop the user taps shows believable data.
 *
 * [latencyMs] simulates a slow query to test loading states (artboards 33 / 33b); default 0.
 */
class MockTransitRepository(
    private val latencyMs: Long = 0,
    /** How long "checking live times" takes, to show artboard 33b. */
    private val realtimeCheckMs: Long = 1200,
) : TransitRepository {

    private val data = MockTransitData
    private val stopsById = data.stops.associateBy { it.id }
    private val linesById = data.lines.associateBy { it.id }

    override fun now(): Minutes = data.NOW

    override suspend fun lines() = data.lines
    override suspend fun line(id: String) = linesById[id]

    override suspend fun stops() = data.stops
    override suspend fun stop(id: String) = stopsById[id]
    override suspend fun stopByName(name: String) =
        data.stops.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: data.stops.firstOrNull { it.name.endsWith(name, ignoreCase = true) }

    override suspend fun departures(
        stopId: String,
        from: Minutes,
        limit: Int,
        line: String?,
        includeCancelled: Boolean,
        dayOffset: Int,
    ): List<StopDeparture> {
        // The sample network runs the same every day.
        simulateLatency()
        val all = if (stopId == "bahnhof") data.bahnhofDepartures else generatedDepartures(stopId)
        return all
            .filter { it.scheduled >= from && (line == null || it.line == line) }
            .filter { includeCancelled || it.realtime != Realtime.Cancelled }
            .sortedBy { it.scheduled }
            .take(limit)
    }

    /** Every 10 min per line, offset by the stop's position in the line pattern. */
    private fun generatedDepartures(stopId: String): List<StopDeparture> {
        val stop = stopsById[stopId] ?: return emptyList()
        return stop.lines.flatMap { lineId ->
            val line = linesById.getValue(lineId)
            val offset = data.patterns[lineId]?.firstOrNull { it.first == stopId }?.second ?: (stopId.length % 7)
            (0 until 8).map { k ->
                val base = data.NOW + (2 + k * 10 + offset % 10)
                StopDeparture(
                    tripId = generatedTripId(lineId, stopId, k),
                    line = lineId,
                    destination = line.terminusB,
                    scheduled = base,
                    // First trip live and on time, one delayed, the rest from the timetable.
                    realtime = when (k) { 0 -> Realtime.OnTime; 1 -> Realtime.Delayed(2); else -> Realtime.Scheduled },
                )
            }
        }
    }

    override suspend fun trip(tripId: String): Trip? {
        simulateLatency()
        val departure = data.bahnhofDepartures.firstOrNull { it.tripId == tripId }
            ?: data.stops.asSequence().flatMap { generatedDepartures(it.id) }.firstOrNull { it.tripId == tripId }
            ?: return null
        val pattern = data.patterns[departure.line].orEmpty()
        // Start the pattern at the departure stop (Bahnhof trips start at the first stop).
        val startStopId = parseGeneratedTripStop(tripId) ?: pattern.first().first
        val startOffset = pattern.firstOrNull { it.first == startStopId }?.second ?: 0
        val delay = departure.realtime.delayMinutes
        val stops = pattern
            .filter { it.second >= startOffset }
            .map { (stopId, offset) ->
                val scheduled = departure.scheduled + (offset - startOffset)
                TripStop(stopId, stopsById.getValue(stopId).name, scheduled, scheduled + delay)
            }
        return Trip(departure.tripId, departure.line, departure.destination, departure.platform, departure.realtime, stops)
    }

    // Generated trips encode where they start: "gen:<line>:<stopId>:<n>".
    private fun generatedTripId(lineId: String, stopId: String, n: Int) = "gen:$lineId:$stopId:$n"
    private fun parseGeneratedTripStop(tripId: String): String? =
        tripId.split(":").takeIf { it.size == 4 && it[0] == "gen" }?.get(2)

    override suspend fun linesAt(stopId: String): List<Line> =
        stopsById[stopId]?.lines.orEmpty().mapNotNull { linesById[it] }

    override suspend fun alertsAt(stopId: String): List<ServiceAlert> {
        val lines = stopsById[stopId]?.lines.orEmpty()
        return data.alerts.filter { alert -> alert.lines.any { it in lines } }
    }

    override suspend fun realtimeInfo(stopId: String): RealtimeInfo {
        if (realtimeCheckMs > 0) delay(realtimeCheckMs)
        return RealtimeInfo(available = true, updatedSecondsAgo = 30)
    }

    override fun myLocation(): MapPoint = data.myLocation

    // One map unit ≈ 6 m (see MockTransitData.latLon); walking 80 m per minute.
    override fun walkMinutes(from: MapPoint, to: MapPoint): Int {
        val units = hypot((to.x - from.x).toDouble(), (to.y - from.y).toDouble())
        return (units * 6 / 80).toInt().coerceAtLeast(1)
    }

    override suspend fun places() = data.places
    override suspend fun place(id: String) = (data.places + data.addresses).firstOrNull { it.id == id }
    override suspend fun placeByName(name: String) = (data.places + data.addresses).firstOrNull { it.name.equals(name, ignoreCase = true) }

    override suspend fun nearbyStops(point: MapPoint, limit: Int): List<NearbyStop> =
        data.stops
            .map { it to hypot((it.point.x - point.x).toDouble(), (it.point.y - point.y).toDouble()) }
            .sortedBy { it.second }
            .take(limit)
            .map { (stop, _) -> NearbyStop(stop, walkMinutes = walkMinutes(point, stop.point)) }

    override suspend fun locationAt(point: MapPoint): LocationInfo {
        val district = data.districts.minBy { (_, p) -> hypot((p.x - point.x).toDouble(), (p.y - point.y).toDouble()) }.first
        val (lat, lon) = data.latLon(point)
        val swiss = district == "Kreuzlingen"
        return LocationInfo(
            point = point,
            name = if (swiss) "Kreuzlingen" else "$district, Konstanz",
            latitude = lat,
            longitude = lon,
            region = if (swiss) "Kreuzlingen, Switzerland" else "Konstanz, Germany",
            nearestStop = nearbyStops(point, limit = 1).firstOrNull(),
        )
    }

    override suspend fun search(query: String, filter: SearchFilter): SearchResults {
        simulateLatency()
        val q = query.trim()
        if (q.isEmpty()) return SearchResults(emptyList(), emptyList(), emptyList())
        fun matches(name: String) = name.contains(q, ignoreCase = true)
        // Names that start with the query first, then by distance.
        val places = data.places.filter { matches(it.name) }.sortedWith(compareBy({ !it.name.startsWith(q, true) }, { it.distanceKm }))
        val stops = data.stops.filter { matches(it.name) }
            .map { it to (data.stopDistanceKm[it.id] ?: distanceKm(it.point)) }
            .sortedBy { it.second }
        val addresses = data.addresses.filter { matches(it.name) }.sortedBy { it.distanceKm }
        return SearchResults(
            places = if (filter == SearchFilter.All || filter == SearchFilter.Places) places else emptyList(),
            stops = if (filter == SearchFilter.All || filter == SearchFilter.Stops) stops else emptyList(),
            addresses = if (filter == SearchFilter.All || filter == SearchFilter.Addresses) addresses else emptyList(),
        )
    }

    private fun distanceKm(point: MapPoint): Double {
        val me = data.myLocation
        val units = hypot((point.x - me.x).toDouble(), (point.y - me.y).toDouble())
        return Math.round(units * 10 / 100.0) / 10.0 // 1 unit ≈ 10 m, one decimal
    }

    // The design sample has fixed journeys: arrive-by and other days show the same options.
    override suspend fun journeys(
        from: String, to: String, preference: RoutePreference, departAt: Minutes?, arriveBy: Boolean, dayOffset: Int,
    ): List<Journey> {
        simulateLatency()
        if (data.unreachable.any { to.contains(it, ignoreCase = true) }) return emptyList()
        if (departAt != null && data.isNight(departAt)) return emptyList()
        // The mock only knows the design's journeys; other places reuse them under their own name.
        val options = data.journeysToUniversity(to).map { it.copy(from = from) }
        return when (preference) {
            // The design lists the fastest first, then the alternatives in departure order.
            RoutePreference.Fastest -> options
            RoutePreference.LeastWalking -> options.sortedBy { it.walkingMinutes }
            RoutePreference.FewestTransfers -> options.sortedWith(compareBy({ it.transfers }, { it.minutes }))
        }.mapIndexed { i, j -> j.copy(badge = if (i == 0) preference.label else null) }
    }

    override suspend fun journey(id: String): Journey? =
        data.journeysToUniversity("Universität Konstanz").firstOrNull { it.id == id }

    override suspend fun dataStatus() = data.dataStatus

    private suspend fun simulateLatency() {
        if (latencyMs > 0) delay(latencyMs)
    }
}
