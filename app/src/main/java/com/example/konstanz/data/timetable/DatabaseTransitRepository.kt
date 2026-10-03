package com.example.konstanz.data.timetable

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import android.content.Context
import com.example.konstanz.data.AppStatus
import com.example.konstanz.data.RoutePreference
import com.example.konstanz.data.SavedStore
import com.example.konstanz.data.UserLocation
import com.example.konstanz.data.walk.WalkRouter
import com.example.konstanz.data.search.PlaceSearch
import com.example.konstanz.data.search.SearchEntry
import com.example.konstanz.data.transit.SearchCategory
import com.example.konstanz.data.transit.SearchHit
import com.example.konstanz.data.transit.DataStatus
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.Journey
import com.example.konstanz.data.transit.Leg
import com.example.konstanz.data.transit.Line
import com.example.konstanz.data.transit.LocationInfo
import com.example.konstanz.data.transit.MapPoint
import com.example.konstanz.data.transit.Minutes
import com.example.konstanz.data.transit.MockTransitRepository
import com.example.konstanz.data.transit.NearbyStop
import com.example.konstanz.data.transit.PlaceKind
import com.example.konstanz.data.transit.Place
import com.example.konstanz.data.transit.Realtime
import com.example.konstanz.data.transit.RealtimeInfo
import com.example.konstanz.data.transit.SearchFilter
import com.example.konstanz.data.transit.SearchResults
import com.example.konstanz.data.transit.ServiceAlert
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.data.transit.Trip
import com.example.konstanz.data.transit.TripStop
import com.example.konstanz.data.transit.TransitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * [TransitRepository] on the shipped NVBW timetable ([TimetableDatabase]): real stops, lines,
 * departures, trips and offline routing. Scheduled times only — realtime comes later.
 *
 * Places, addresses and the map drawing still come from the design ([MockTransitRepository]) until
 * real place data and the real map arrive (K10); real stops are placed on the drawing with [Geo].
 */
class DatabaseTransitRepository(
    context: Context,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) : TransitRepository {
    init {
        // Words made here (route steps, platform sides) need the app's resources, also without an activity.
        Texts.init(context)
    }

    private val dao = TimetableDatabase.get(context).timetableDao()
    private val dbFile: File = context.getDatabasePath(TimetableDatabase.ASSET)
    private val design = MockTransitRepository(realtimeCheckMs = 0)
    private val walker = WalkRouter(context)
    private val finder = PlaceSearch(context)

    private class Station(val row: StationRow, val stop: Stop)

    private class Network(
        val stations: List<Station>,
        val bySlug: Map<String, Station>,
        val byId: Map<Int, Station>,
        val lines: Map<String, Line>,
        val routes: Map<Int, RouteRow>,
        val footpaths: Map<Int, List<Access>>,
    )

    private val mutex = Mutex()
    private var network: Network? = null
    private var router: Pair<LocalDate, ConnectionScanRouter>? = null
    private val journeyCache = LinkedHashMap<String, Journey>()

    private suspend fun net(): Network = network ?: mutex.withLock { network ?: load().also { network = it } }

    private suspend fun load(): Network = withContext(Dispatchers.Default) {
        val routes = dao.routes()
        val routeById = routes.associateBy { it.id }
        val lines = routes.sortedWith(lineOrder).groupBy { it.shortName }.mapValues { (_, rs) -> rs.first().toLine() }
        val platforms = dao.allPlatforms().groupBy { it.stationId }
        val linesAt = dao.stationLines().groupBy({ it.stationId }, { routeById.getValue(it.routeId) })
            .mapValues { (_, rs) -> rs.sortedWith(lineOrder).map { it.shortName }.distinct() }

        val used = HashSet<String>()
        val stations = dao.stations().map { row ->
            val base = slug(row.shortName)
            val id = generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base-$it" }.first { used.add(it) }
            val codes = platforms[row.id].orEmpty().mapNotNull { it.code }.distinct().sorted()
            Station(
                row,
                Stop(
                    id = id,
                    name = displayName(row),
                    lines = linesAt[row.id].orEmpty(),
                    point = Geo.mapPoint(row.lat, row.lon),
                    platforms = if (codes.size > 1) "${codes.first()}–${codes.last()}" else null,
                ),
            )
        }
        // Transfers on foot between stations up to 400 m apart.
        val footpaths = stations.associate { a ->
            a.row.id to stations.mapNotNull { b ->
                if (a === b) return@mapNotNull null
                val m = Geo.meters(a.row.lat, a.row.lon, b.row.lat, b.row.lon)
                if (m > 400) null else Access(b.row.id, Geo.walkMinutes(m * 1.25))
            }
        }
        Network(stations, stations.associateBy { it.stop.id }, stations.associateBy { it.row.id }, lines, routeById, footpaths)
    }

    // ---------- Clock ----------

    override fun now(): Minutes = clock().let { Minutes.of(it.hour, it.minute) }
    private fun today(): LocalDate = clock().toLocalDate()

    // ---------- Lines & stops ----------

    override suspend fun lines() = net().lines.values.toList()
    override suspend fun line(id: String) = net().lines[id]
    override suspend fun stops() = net().stations.map { it.stop }
    override suspend fun stop(id: String) = net().bySlug[id]?.stop

    override suspend fun stopByName(name: String): Stop? {
        val q = fold(name)
        val all = net().stations
        return (all.firstOrNull { fold(it.stop.name) == q || fold(it.row.name) == q }
            ?: all.firstOrNull { fold(it.row.name).endsWith(q) })?.stop
    }

    override suspend fun linesAt(stopId: String): List<Line> {
        val n = net()
        return n.bySlug[stopId]?.stop?.lines.orEmpty().mapNotNull { n.lines[it] }
    }

    // ---------- Departures & trips ----------

    override suspend fun departures(
        stopId: String,
        from: Minutes,
        limit: Int,
        line: String?,
        includeCancelled: Boolean,
    ): List<StopDeparture> {
        val station = net().bySlug[stopId] ?: return emptyList()
        val fetch = if (line == null) limit else limit * 8
        val day = today()
        // After-midnight trips belong to yesterday's service day with times ≥ 24:00.
        val rows = dao.departures(station.row.id, day.int(), from.value, fetch).map { it to 0 } +
            dao.departures(station.row.id, day.minusDays(1).int(), from.value + DAY, fetch).map { it to -DAY }
        return rows
            .map { (r, shift) ->
                StopDeparture(
                    tripId = r.tripId.toString(),
                    line = r.line,
                    destination = shortPlaceName(r.headsign),
                    scheduled = Minutes(r.departure + shift),
                    realtime = Realtime.Scheduled,
                    platform = r.platformCode,
                    side = sideLabel(r.platformDirection),
                )
            }
            .filter { line == null || it.line == line }
            .sortedBy { it.scheduled }
            .take(limit)
    }

    override suspend fun trip(tripId: String): Trip? {
        val id = tripId.toIntOrNull() ?: return null
        val n = net()
        val trip = dao.trip(id) ?: return null
        val route = n.routes[trip.routeId] ?: return null
        val calls = dao.tripCalls(id)
        return Trip(
            id = tripId,
            line = route.shortName,
            destination = shortPlaceName(trip.headsign),
            platform = calls.firstOrNull()?.platformCode,
            realtime = Realtime.Scheduled,
            stops = calls.map { c ->
                val s = n.byId.getValue(c.stationId).stop
                TripStop(s.id, s.name, Minutes(c.departure), Minutes(c.departure))
            },
        )
    }

    override suspend fun alertsAt(stopId: String): List<ServiceAlert> = emptyList()

    /** No realtime feed yet: every stop shows the timetable. */
    override suspend fun realtimeInfo(stopId: String) = RealtimeInfo(available = false, updatedSecondsAgo = null)

    // ---------- Places & location (OpenStreetMap, from the offline map tiles) ----------

    /**
     * The phone's position when it is inside the offline map; anywhere else (or without a fix) the
     * design's spot near the Bahnhof, so planning still works for someone outside Konstanz.
     */
    override fun myLocation(): MapPoint {
        val fix = UserLocation.fix
        if (fix != null && !AppStatus.simulateLocationUnavailable &&
            fix.latitude in 47.63..47.74 && fix.longitude in 9.05..9.26
        ) return Geo.mapPoint(fix.latitude, fix.longitude)
        return design.myLocation()
    }

    override fun walkMinutes(from: MapPoint, to: MapPoint): Int {
        val (lat1, lon1) = Geo.latLon(from)
        val (lat2, lon2) = Geo.latLon(to)
        return Geo.walkMinutes(Geo.walkMeters(lat1, lon1, lat2, lon2))
    }

    override suspend fun places(): List<Place> = dao.landmarks(limit = 40).map { it.toPlace() }

    override suspend fun place(id: String): Place? =
        (dao.placeBySlug(id) ?: DESIGN_PLACE_NAMES[id]?.let { dao.placeByName(fold(it)) })?.toPlace()
            ?: finder.byId(id)?.let { it.toPlace(distanceKm(it.lat, it.lon)) }

    override suspend fun placeByName(name: String): Place? =
        pinnedPlace(name) ?: dao.placeByName(fold(name))?.toPlace()
            ?: finder.byName(name)?.let { it.toPlace(distanceKm(it.lat, it.lon)) }

    private fun distanceKm(lat: Double, lon: Double): Double {
        val (myLat, myLon) = Geo.latLon(myLocation())
        return (Geo.meters(myLat, myLon, lat, lon) / 100).roundToInt() / 10.0
    }

    /**
     * Points described by [locationAt] ("Fischmarkt, Altstadt"): screens pass places on by name, so a
     * dropped pin used as start or destination keeps its exact position. The newest 20 are kept.
     */
    private val pins = object : LinkedHashMap<String, Pair<MapPoint, String>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<MapPoint, String>>) = size > 20
    }

    private fun pinnedPlace(name: String): Place? {
        val (point, region) = synchronized(pins) { pins[name] } ?: return null
        val (myLat, myLon) = Geo.latLon(myLocation())
        val (lat, lon) = Geo.latLon(point)
        return Place(
            id = "pin:${point.x.roundToInt()},${point.y.roundToInt()}",
            name = name,
            kind = PlaceKind.Address,
            address = region,
            point = point,
            distanceKm = (Geo.meters(myLat, myLon, lat, lon) / 100).roundToInt() / 10.0,
            category = Texts.get(R.string.dropped_pin),
        )
    }

    override suspend fun nearbyStops(point: MapPoint, limit: Int): List<NearbyStop> {
        val (lat, lon) = Geo.latLon(point)
        return net().stations
            .map { it to Geo.walkMeters(lat, lon, it.row.lat, it.row.lon) }
            .sortedBy { it.second }
            .take(limit)
            .map { (s, m) -> NearbyStop(s.stop, Geo.walkMinutes(m)) }
    }

    /** "What's here?": the nearest street (within 120 m), its district and town, all offline. */
    override suspend fun locationAt(point: MapPoint): LocationInfo {
        val (lat, lon) = Geo.latLon(point)
        val areas = areas()
        fun nearest(kind: String, maxM: Double, minRank: Int = 0) = areas.filter { it.kind == kind && it.rank >= minRank }
            .map { it to Geo.meters(lat, lon, it.lat, it.lon) }
            .filter { it.second <= maxM }
            .minByOrNull { it.second }?.first
        // Konstanz or Kreuzlingen when within 5 km, otherwise the nearest village (same rule as the importer).
        val town = nearest("Town", 5_000.0, minRank = 1) ?: nearest("Town", 6_000.0)
        val district = nearest("District", 1_500.0)
        val d = 0.0015 // ≈ 120–170 m around the point
        val street = dao.streetPointsIn(lat - d, lat + d, lon - d, lon + d)
            .map { it to Geo.meters(lat, lon, it.lat, it.lon) }
            .filter { it.second <= 120 }
            .minByOrNull { it.second }
            ?.let { dao.placeById(it.first.placeId) }
        val name = listOfNotNull(street?.name, district?.name ?: town?.name).distinct().joinToString(", ")
            .ifEmpty { String.format(Locale.ROOT, "%.4f° N, %.4f° E", lat, lon) }
        val country = when (town?.detail) {
            "Germany" -> Texts.get(R.string.country_germany)
            "Switzerland" -> Texts.get(R.string.country_switzerland)
            else -> town?.detail
        }
        val region = listOfNotNull(town?.name, country?.takeIf { it.isNotEmpty() }).joinToString(", ")
        synchronized(pins) { pins[name] = point to region }
        return LocationInfo(
            point = point,
            name = name,
            latitude = lat,
            longitude = lon,
            region = region,
            nearestStop = nearbyStops(point, limit = 1).firstOrNull(),
        )
    }

    override suspend fun search(query: String, filter: SearchFilter): SearchResults {
        if (query.isBlank()) return SearchResults(emptyList(), emptyList(), emptyList())
        val (myLat, myLon) = Geo.latLon(myLocation())
        val groups = when (filter) {
            SearchFilter.All -> null
            SearchFilter.Stops -> setOf("Stop")
            SearchFilter.Addresses -> setOf("Address", "Street")
            SearchFilter.Places -> PLACE_GROUPS
        }
        val matches = finder.search(query, myLat, myLon, stopEntries(), groups, limit = 30)
        val hits = matches.mapNotNull { m -> hit(m.entry, m.meters) }
        return SearchResults(
            places = hits.mapNotNull { it.place }.filter { it.kind != PlaceKind.Street && it.kind != PlaceKind.Address },
            stops = hits.mapNotNull { h -> h.stop?.let { it to h.distanceKm } },
            addresses = hits.mapNotNull { it.place }.filter { it.kind == PlaceKind.Street || it.kind == PlaceKind.Address },
            top = hits,
            categories = if (filter == SearchFilter.All || filter == SearchFilter.Places) finder.categories(query) else emptyList(),
            didYouMean = matches.firstOrNull()?.takeIf { it.fuzzy }?.entry?.name,
        )
    }

    override suspend fun nearby(category: SearchCategory): List<SearchHit> {
        val (myLat, myLon) = Geo.latLon(myLocation())
        return finder.nearby(category.key, myLat, myLon).mapNotNull { hit(it.entry, it.meters) }
    }

    override fun searchShortcuts(): List<SearchCategory> = PlaceSearch.SHORTCUTS

    /** Bus stops as search entries ("Stop"), so they rank together with places. */
    private var stopEntryCache: List<SearchEntry>? = null
    private suspend fun stopEntries(): List<SearchEntry> = stopEntryCache ?: net().stations.map { s ->
        SearchEntry(
            id = s.stop.id, name = s.stop.name, group = "Stop", category = Texts.get(R.string.bus_stop), area = "",
            lat = s.row.lat, lon = s.row.lon, rank = 45,
            keywords = "bus stop haltestelle bushaltestelle " + if (s.row.name != s.stop.name) s.row.name else "",
            address = "",
        )
    }.also { stopEntryCache = it }

    private suspend fun hit(e: SearchEntry, meters: Double): SearchHit? {
        val km = (meters / 100).roundToInt() / 10.0
        if (e.group == "Stop") return net().bySlug[e.id]?.let { SearchHit(null, it.stop, km) }
        return SearchHit(e.toPlace(km), null, km)
    }

    private fun SearchEntry.toPlace(km: Double) = Place(
        id = id,
        name = name,
        kind = when (group) {
            "University" -> PlaceKind.University
            "Street" -> PlaceKind.Street
            "Address" -> PlaceKind.Address
            "Square" -> PlaceKind.Square
            "Food" -> PlaceKind.Food
            "Cafe" -> PlaceKind.Cafe
            "Shop" -> PlaceKind.Shop
            "Health" -> PlaceKind.Health
            "Education" -> if (category == "Library") PlaceKind.Library else PlaceKind.Education
            "Culture" -> PlaceKind.Culture
            "Park" -> PlaceKind.Park
            "Leisure" -> PlaceKind.Leisure
            "Hotel" -> PlaceKind.Hotel
            "Transport" -> if (category.contains("station", ignoreCase = true)) PlaceKind.Station else PlaceKind.Transport
            "District" -> PlaceKind.District
            "Town" -> PlaceKind.Town
            else -> PlaceKind.Service
        },
        // "Marktstätte 1, Altstadt, Konstanz"; streets "Street · Paradies, Konstanz"; addresses "78462 Konstanz".
        address = when (group) {
            "Street" -> listOf(Texts.get(R.string.street), area).filter { it.isNotEmpty() }.joinToString(" · ")
            else -> listOf(address, area).filter { it.isNotEmpty() }.joinToString(", ")
        },
        point = Geo.mapPoint(lat, lon),
        distanceKm = km,
        category = localCategory,
    )

    private var areaCache: List<PlaceRow>? = null
    private suspend fun areas(): List<PlaceRow> = areaCache ?: dao.areas().also { areaCache = it }

    private fun PlaceRow.toPlace(): Place {
        val (myLat, myLon) = Geo.latLon(myLocation())
        return Place(
            id = slug,
            name = name,
            kind = when (kind) {
                "University" -> PlaceKind.University
                "Library" -> PlaceKind.Library
                "Station" -> PlaceKind.Station
                "Harbour" -> PlaceKind.Harbour
                "Square" -> PlaceKind.Square
                "Street" -> PlaceKind.Street
                else -> PlaceKind.Venue
            },
            // Places: "Altstadt, Konstanz" (the category comes separately); streets: "Street · Paradies, Konstanz".
            address = if (kind == "Street") listOf(Texts.get(R.string.street), area).filter { it.isNotEmpty() }.joinToString(" · ") else area,
            point = Geo.mapPoint(lat, lon),
            distanceKm = (Geo.meters(myLat, myLon, lat, lon) / 100).roundToInt() / 10.0,
            category = detail,
        )
    }

    // ---------- Journeys ----------

    private class Endpoint(val label: String, val lat: Double, val lon: Double)

    private suspend fun resolve(name: String): Endpoint? {
        if (name.equals("My location", ignoreCase = true)) {
            val (lat, lon) = Geo.latLon(myLocation())
            return Endpoint(name, lat, lon)
        }
        stopByName(name)?.let { s -> net().bySlug[s.id]?.let { return Endpoint(name, it.row.lat, it.row.lon) } }
        placeByName(name)?.let { p -> Geo.latLon(p.point).let { (lat, lon) -> return Endpoint(name, lat, lon) } }
        // A saved place's label ("University") points at a place or stop.
        val target = SavedStore.places.firstOrNull { it.name.equals(name, ignoreCase = true) }?.targetId ?: return null
        net().bySlug[target]?.let { return Endpoint(name, it.row.lat, it.row.lon) }
        return place(target)?.let { p -> Geo.latLon(p.point).let { (lat, lon) -> Endpoint(name, lat, lon) } }
    }

    /** Stations within a 15-minute walk of [e] (at least the closest two). */
    private suspend fun accessFor(e: Endpoint): List<Access> =
        net().stations
            .map { it to Geo.walkMeters(e.lat, e.lon, it.row.lat, it.row.lon) }
            .sortedBy { it.second }
            .filterIndexed { i, (_, m) -> i < 2 || m <= 15 * Geo.WALK_M_PER_MIN }
            .take(12)
            .map { (s, m) -> Access(s.row.id, if (m < 40) 0 else Geo.walkMinutes(m)) }

    private suspend fun routerFor(day: LocalDate): ConnectionScanRouter = mutex.withLock {
        router?.takeIf { it.first == day }?.second ?: withContext(Dispatchers.Default) {
            val connections = ArrayList<Connection>()
            fun addHops(calls: List<CallRow>, tripSign: Int, shift: Int) {
                for (i in 0 until calls.size - 1) {
                    val a = calls[i]
                    val b = calls[i + 1]
                    if (a.tripId != b.tripId) continue
                    val dep = a.departure + shift
                    if (dep < 0) continue
                    connections += Connection(tripSign * a.tripId, a.stationId, b.stationId, dep, b.arrival + shift)
                }
            }
            addHops(dao.callsOn(day.int()), tripSign = 1, shift = 0)
            // Yesterday's after-midnight trips; negative ids keep them apart from today's runs.
            addHops(dao.callsOn(day.minusDays(1).int()), tripSign = -1, shift = -DAY)
            connections.sortWith(compareBy({ it.departure }, { it.arrival }))
            ConnectionScanRouter(connections, network!!.footpaths)
        }.also { router = day to it }
    }

    override suspend fun journeys(
        from: String,
        to: String,
        preference: RoutePreference,
        departAt: Minutes?,
        arriveBy: Boolean,
        dayOffset: Int,
    ): List<Journey> {
        val a = resolve(from) ?: return emptyList()
        val b = resolve(to) ?: return emptyList()
        net()
        val router = routerFor(today().plusDays(dayOffset.toLong()))
        val origins = accessFor(a)
        val destinations = accessFor(b)
        val time = (departAt ?: now()).value
        suspend fun route(t: Int) = withContext(Dispatchers.Default) { router.route(origins, destinations, t) }
        fun trips(r: RouteResult) = r.parts.filterIsInstance<RoutePart.Ride>().map { it.trip }

        // Boarding the same buses at another stop is not a new option: one per set of trips.
        val byTrips = LinkedHashMap<List<Int>, RouteResult>()
        if (!arriveBy) {
            // The best route, then the best one leaving later, and so on; per set of trips the quickest.
            var t = time
            repeat(10) {
                if (byTrips.size >= 4) return@repeat
                val r = route(t) ?: return@repeat
                val known = byTrips[trips(r)]
                if (known == null || r.arrival - r.leaveAt < known.arrival - known.leaveAt) byTrips[trips(r)] = r
                t = r.leaveAt + 1
            }
        } else {
            // Arrive by [time]: scan departures from 3 h before and keep those arriving in time. The
            // earliest arrival never gets earlier when leaving later, so the scan stops at the first late one.
            var t = (time - 180).coerceAtLeast(0)
            repeat(40) {
                val r = route(t)?.takeIf { it.arrival <= time } ?: return@repeat
                val known = byTrips[trips(r)]
                if (known == null || r.leaveAt > known.leaveAt) byTrips[trips(r)] = r
                t = r.leaveAt + 1
            }
        }
        val results = if (arriveBy) byTrips.values.sortedByDescending { it.leaveAt }.take(4) else byTrips.values.toList()
        val options = results.mapIndexed { i, r -> toJourney("rt-$time-$i-${(from + to).hashCode()}", r, a, b) }.toMutableList()

        // Walking the whole way: always for short trips (≤ 20 min, no waiting), otherwise when about as quick as the bus.
        val walkMeters = Geo.walkMeters(a.lat, a.lon, b.lat, b.lon)
        val walkMinutes = Geo.walkMinutes(walkMeters)
        val quickest = options.minOfOrNull { it.minutes }
        if (walkMinutes <= 20 || (walkMinutes <= 45 && (quickest == null || walkMinutes <= quickest + 5))) {
            val leave = if (arriveBy) time - walkMinutes else time
            options += Journey(
                id = "walk-$time-${(from + to).hashCode()}",
                from = a.label,
                to = b.label,
                legs = listOf(walkLeg(Minutes(leave), Minutes(leave + walkMinutes), a.lat, a.lon, b.lat, b.lon, b.label)),
            )
        }

        return when (preference) {
            RoutePreference.Fastest -> options.sortedWith(compareBy({ if (arriveBy) -it.start.value else it.end.value }, { it.minutes }))
            RoutePreference.LeastWalking -> options.sortedWith(compareBy({ it.walkingMinutes }, { it.end }))
            RoutePreference.FewestTransfers -> options.sortedWith(compareBy({ it.transfers }, { it.end }))
        }.mapIndexed { i, j -> j.copy(badge = if (i == 0) preference.label else null) }
            .onEach { synchronized(journeyCache) { journeyCache[it.id] = it } }
    }

    override suspend fun journey(id: String): Journey? = synchronized(journeyCache) { journeyCache[id] }

    /** One bus from [fromStopId] to its last stop, drawn along its street path. */
    override suspend fun rideJourney(tripId: String, fromStopId: String?, at: Int?): Journey? {
        val id = tripId.toIntOrNull() ?: return null
        val n = net()
        val calls = dao.tripCalls(id)
        val passes = calls.indices.filter { n.byId[calls[it].stationId]?.stop?.id == fromStopId }
        val board = passes.firstOrNull { calls[it].departure == at } ?: passes.firstOrNull() ?: 0
        if (board >= calls.lastIndex) return null
        val a = calls[board]
        val b = calls.last()
        val leg = rideLeg(RoutePart.Ride(id, a.stationId, b.stationId, a.departure, b.arrival), n)
        return Journey("ride:$tripId:${leg.fromStopId}", leg.from, leg.to, listOf(leg))
    }

    private suspend fun toJourney(id: String, r: RouteResult, a: Endpoint, b: Endpoint): Journey {
        val n = net()
        fun st(id: Int) = n.byId.getValue(id)
        val legs = ArrayList<Leg>()
        val firstStation = st(r.firstRide.from)
        if (r.access.walkMinutes > 0) {
            val dep = r.firstRide.departure
            legs += walkLeg(
                Minutes(dep - r.access.walkMinutes), Minutes(dep),
                a.lat, a.lon, firstStation.row.lat, firstStation.row.lon, firstStation.stop.name,
            )
        }
        for (part in r.parts) when (part) {
            is RoutePart.Ride -> legs += rideLeg(part, n)
            is RoutePart.Transfer -> {
                val s = st(part.from)
                val e = st(part.to)
                val t0 = legs.last().end
                legs += walkLeg(t0, t0 + part.minutes, s.row.lat, s.row.lon, e.row.lat, e.row.lon, e.stop.name)
            }
        }
        if (r.egress.walkMinutes > 0) {
            val s = st(r.egress.station)
            val t0 = legs.last().end
            legs += walkLeg(t0, t0 + r.egress.walkMinutes, s.row.lat, s.row.lon, b.lat, b.lon, b.label)
        }
        return Journey(id, a.label, b.label, legs)
    }

    /**
     * A walk along the streets (offline walking network) with turn-by-turn steps; a straight line
     * with the usual detour estimate when the network doesn't reach.
     */
    private suspend fun walkLeg(
        start: Minutes, end: Minutes, fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, to: String,
    ): Leg.Walk {
        val route = walker.route(fromLat, fromLon, toLat, toLon, to)
        return if (route != null) {
            Leg.Walk(
                start, end,
                meters = (route.meters / 10).roundToInt().coerceAtLeast(1) * 10,
                to = to,
                steps = route.steps,
                path = toPolyline(route.points.map { (lat, lon) -> Geo.mapPoint(lat, lon) }),
            )
        } else {
            Leg.Walk(
                start, end,
                meters = (Geo.walkMeters(fromLat, fromLon, toLat, toLon) / 10).roundToInt() * 10,
                to = to,
                path = toPolyline(listOf(Geo.mapPoint(fromLat, fromLon), Geo.mapPoint(toLat, toLon))),
            )
        }
    }

    /** The stop register's direction ("Towards city centre", "Out of town", "Towards Bahnhof") in the phone's language. */
    private fun sideLabel(side: String?): String? = when {
        side == null -> null
        side == "Towards city centre" -> Texts.get(R.string.side_city_centre)
        side == "Out of town" -> Texts.get(R.string.side_out_of_town)
        side.startsWith("Towards ") -> Texts.get(R.string.side_towards, side.removePrefix("Towards "))
        else -> side
    }

    private suspend fun rideLeg(part: RoutePart.Ride, n: Network): Leg.Ride {
        val tripId = kotlin.math.abs(part.trip)
        val shift = if (part.trip < 0) -DAY else 0
        val trip = dao.trip(tripId)
        val route = trip?.let { n.routes[it.routeId] }
        val calls = dao.tripCalls(tripId)
        val board = calls.indexOfFirst { it.stationId == part.from && it.departure + shift == part.departure }.coerceAtLeast(0)
        val alight = (board + 1 until calls.size).firstOrNull { calls[it].stationId == part.to && calls[it].arrival + shift == part.arrival }
            ?: calls.lastIndex
        val ridden = calls.subList(board, alight + 1)
        return Leg.Ride(
            Minutes(part.departure), Minutes(part.arrival),
            line = route?.shortName ?: "?",
            direction = trip?.let { shortPlaceName(it.headsign) } ?: "",
            from = n.byId.getValue(part.from).stop.name,
            platform = ridden.first().platformCode,
            to = n.byId.getValue(part.to).stop.name,
            realtime = Realtime.Scheduled,
            stops = ridden.drop(1).map { c ->
                val s = n.byId.getValue(c.stationId).stop
                TripStop(s.id, s.name, Minutes(c.arrival + shift), Minutes(c.arrival + shift))
            },
            path = toPolyline(streetPath(trip?.shapeId, ridden.map { n.byId.getValue(it.stationId).stop.point })),
            side = sideLabel(ridden.first().platformDirection),
            tripId = tripId.toString(),
            fromStopId = n.byId[part.from]?.stop?.id,
        )
    }

    private val shapes = HashMap<Int, List<MapPoint>>()

    /**
     * The bus's real path from the first to the last of [stops], cut out of its shape; the straight
     * stop-to-stop line when the trip has no shape or the cut fails.
     */
    private suspend fun streetPath(shapeId: Int?, stops: List<MapPoint>): List<MapPoint> {
        if (shapeId == null || stops.size < 2) return stops
        val shape = synchronized(shapes) { shapes[shapeId] }
            ?: dao.shape(shapeId)?.let { row -> decodePolyline(row.points).map { (lat, lon) -> Geo.mapPoint(lat, lon) } }
                ?.also { synchronized(shapes) { shapes[shapeId] = it } }
            ?: return stops
        fun d2(a: MapPoint, b: MapPoint) = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        // Walk the shape stop by stop, always forward: ring lines pass the same streets twice.
        var index = 0
        val matched = ArrayList<Int>(stops.size)
        for (stop in stops) {
            index = (index until shape.size).minByOrNull { d2(shape[it], stop) } ?: return stops
            matched += index
        }
        val start = matched.first()
        val end = matched.last()
        if (end <= start) return stops
        val path = listOf(stops.first()) + shape.subList(start, end + 1) + stops.last()
        // A cut far longer than the stop-to-stop line went wrong (e.g. matched the wrong pass): don't draw it.
        fun length(points: List<MapPoint>) = points.zipWithNext().sumOf { (a, b) -> kotlin.math.sqrt(d2(a, b).toDouble()) }
        return if (length(path) > 2.5 * length(stops) + 50) stops else path
    }

    // ---------- Data status ----------

    override suspend fun dataStatus(): DataStatus {
        val version = dao.meta("feed_version").orEmpty()
        val end = dao.meta("feed_end").orEmpty()
        return DataStatus(
            upToDate = (end.toIntOrNull() ?: 0) >= today().int(),
            version = version.toIntOrNull() ?: 0,
            lastSync = Texts.get(R.string.timetable_of, formatDate(version)),
            savedAt = Texts.get(R.string.valid_until_cap, formatDate(end)),
            storage = String.format(Locale.ROOT, "%.1f MB", dbFile.length() / 1e6),
        )
    }

    private companion object {
        const val DAY = 24 * 60

        /** "Places" filter: everything but stops, streets and house addresses. */
        val PLACE_GROUPS = setOf("Food", "Cafe", "Shop", "Health", "Education", "University", "Culture", "Park",
            "Leisure", "Hotel", "Transport", "Service", "Square", "District", "Town")

        /** Place ids of the design sample (saved places, links) → the real place they stand for. */
        val DESIGN_PLACE_NAMES = mapOf(
            "uni" to "Universität Konstanz",
            "uni-library" to "Universität Konstanz",
            "bahnhof-place" to "Konstanz",
            "mainau" to "Insel Mainau",
            "addr-uni-str" to "Universitätsstraße",
            "addr-bahnhofplatz" to "Bahnhofplatz",
            "addr-hafenstr" to "Hafenstraße",
        )
        const val TOWN = "Konstanz "

        /** City buses first, then by number ("2" before "12"), then trains, ferries, regional buses. */
        val lineOrder = compareBy<RouteRow>({ !it.isCity }, { it.shortName.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }, { it.shortName })

        fun RouteRow.toLine(): Line {
            val ends = longName.split(" - ").map { it.trim() }.filter { it.isNotEmpty() }
            val name = when (type) { 3 -> "Bus $shortName"; else -> shortName }
            return Line(shortName, name, ends.firstOrNull().orEmpty(), ends.lastOrNull().orEmpty())
        }

        /** "Konstanz Universität" → "Universität", but "Konstanz Bahnhof" stays (the design names it so). */
        fun displayName(row: StationRow): String = if (row.shortName in setOf("Bahnhof", "Hafen")) row.name else row.shortName

        /** Headsigns: "Konstanz, Zähringerplatz" → "Zähringerplatz", "Bahnhof (Bus)" → "Konstanz Bahnhof". */
        fun shortPlaceName(name: String): String {
            val plain = name.removeSuffix(" (Bus)")
            val rest = plain.removePrefix("Konstanz, ").removePrefix(TOWN)
            return when {
                rest in setOf("Bahnhof", "Hafen") -> "$TOWN$rest"
                else -> rest
            }
        }

        /** Stop ids like the design's: "Universität" → "universitaet". */
        fun slug(name: String): String = name.lowercase(Locale.GERMAN)
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            .let { Normalizer.normalize(it, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "") }
            .replace(Regex("[^a-z0-9]+"), "-").trim('-')

        /** For search: lowercase, no accents, "ß" → "ss". */
        fun fold(text: String): String = Normalizer.normalize(text.lowercase(Locale.GERMAN).replace("ß", "ss"), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")

        fun LocalDate.int() = year * 10_000 + monthValue * 100 + dayOfMonth

        fun formatDate(yyyymmdd: String): String = runCatching {
            LocalDate.parse(yyyymmdd, DateTimeFormatter.BASIC_ISO_DATE).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        }.getOrDefault(yyyymmdd)

        fun toPolyline(points: List<MapPoint>): String =
            points.mapIndexed { i, p -> String.format(Locale.ROOT, "%s%.1f %.1f", if (i == 0) "M" else "L", p.x, p.y) }.joinToString(" ")

        /** Google encoded polyline (1e-5 degrees) → lat/lon pairs. */
        fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
            val out = ArrayList<Pair<Double, Double>>()
            var i = 0
            var lat = 0
            var lon = 0
            fun next(): Int {
                var result = 0
                var shift = 0
                var b: Int
                do {
                    b = encoded[i++].code - 63
                    result = result or ((b and 0x1F) shl shift)
                    shift += 5
                } while (b >= 0x20)
                return if (result and 1 != 0) (result shr 1).inv() else result shr 1
            }
            while (i < encoded.length) {
                lat += next()
                lon += next()
                out += lat / 1e5 to lon / 1e5
            }
            return out
        }
    }
}
