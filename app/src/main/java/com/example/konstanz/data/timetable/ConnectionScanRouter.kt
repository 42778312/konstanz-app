package com.example.konstanz.data.timetable

/** A ride between two consecutive calls of a trip. Times are minutes after midnight. */
class Connection(val trip: Int, val from: Int, val to: Int, val departure: Int, val arrival: Int)

/** A station reachable on foot in [walkMinutes] (start/end of a journey, or a transfer). */
data class Access(val station: Int, val walkMinutes: Int)

sealed interface RoutePart {
    data class Ride(val trip: Int, val from: Int, val to: Int, val departure: Int, val arrival: Int) : RoutePart
    data class Transfer(val from: Int, val to: Int, val minutes: Int) : RoutePart
}

/** Walk to [access], take [parts] (always starting with a ride), walk from [egress]; arrive at [arrival]. */
data class RouteResult(val access: Access, val parts: List<RoutePart>, val egress: Access, val arrival: Int) {
    val firstRide: RoutePart.Ride get() = parts.first() as RoutePart.Ride
    /** When to leave the start point. */
    val leaveAt: Int get() = firstRide.departure - access.walkMinutes
}

/**
 * Earliest-arrival routing with the Connection Scan Algorithm (Dibbelt et al.): one pass over the
 * day's connections sorted by departure. Offline and fast enough for a city (a few ms per query).
 *
 * @param connections sorted by departure.
 * @param footpaths per station, the stations a short walk away (transfers between stops).
 * @param minChange minutes needed to change buses at the same station.
 */
class ConnectionScanRouter(
    private val connections: List<Connection>,
    private val footpaths: Map<Int, List<Access>>,
    private val minChange: Int = 1,
) {
    private sealed interface Reached {
        data class Start(val access: Access) : Reached
        data class ByRide(val enter: Int, val exit: Int) : Reached
        data class ByWalk(val from: Int, val minutes: Int) : Reached
    }

    /** Best route leaving at [departAt] or later, or null when the destinations can't be reached. */
    fun route(origins: List<Access>, destinations: List<Access>, departAt: Int): RouteResult? {
        val earliest = HashMap<Int, Int>()
        val reached = HashMap<Int, Reached>()
        val arrivedByRide = HashSet<Int>()
        val tripEntered = HashMap<Int, Int>()
        val egress = destinations.associate { it.station to it.walkMinutes }
        var best = Int.MAX_VALUE
        var bestStation = -1

        fun offer(station: Int) {
            val walk = egress[station] ?: return
            val t = earliest.getValue(station) + walk
            if (t < best) { best = t; bestStation = station }
        }

        for (o in origins) {
            val t = departAt + o.walkMinutes
            if (t < (earliest[o.station] ?: Int.MAX_VALUE)) {
                earliest[o.station] = t
                reached[o.station] = Reached.Start(o)
            }
        }

        for (i in firstDepartureAtOrAfter(departAt) until connections.size) {
            val c = connections[i]
            if (c.departure > best) break // nothing later can arrive earlier
            val entered = tripEntered[c.trip] ?: run {
                val at = earliest[c.from] ?: return@run null
                val ready = at + if (c.from in arrivedByRide) minChange else 0
                if (ready > c.departure) null else i.also { tripEntered[c.trip] = it }
            } ?: continue

            if (c.arrival < (earliest[c.to] ?: Int.MAX_VALUE)) {
                earliest[c.to] = c.arrival
                reached[c.to] = Reached.ByRide(entered, i)
                arrivedByRide += c.to
                offer(c.to)
                for (f in footpaths[c.to].orEmpty()) {
                    val t = c.arrival + f.walkMinutes
                    if (t < (earliest[f.station] ?: Int.MAX_VALUE)) {
                        earliest[f.station] = t
                        reached[f.station] = Reached.ByWalk(c.to, f.walkMinutes)
                        arrivedByRide -= f.station
                        offer(f.station)
                    }
                }
            }
        }
        if (bestStation < 0) return null

        // Walk the pointers back to the start.
        val parts = ArrayList<RoutePart>()
        var station = bestStation
        var start: Access? = null
        repeat(64) {
            when (val r = reached.getValue(station)) {
                is Reached.Start -> { start = r.access; return@repeat }
                is Reached.ByRide -> {
                    val enter = connections[r.enter]
                    val exit = connections[r.exit]
                    parts += RoutePart.Ride(enter.trip, enter.from, exit.to, enter.departure, exit.arrival)
                    station = enter.from
                }
                is Reached.ByWalk -> {
                    parts += RoutePart.Transfer(r.from, station, r.minutes)
                    station = r.from
                }
            }
        }
        val access = start ?: return null
        parts.reverse()
        if (parts.firstOrNull() !is RoutePart.Ride) return null // walking only: not a transit route
        return RouteResult(access, parts, Access(bestStation, egress.getValue(bestStation)), best)
    }

    private fun firstDepartureAtOrAfter(time: Int): Int {
        var lo = 0
        var hi = connections.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (connections[mid].departure < time) lo = mid + 1 else hi = mid
        }
        return lo
    }
}
