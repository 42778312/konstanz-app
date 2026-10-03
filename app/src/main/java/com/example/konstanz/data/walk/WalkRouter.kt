package com.example.konstanz.data.walk

import com.example.konstanz.R
import com.example.konstanz.data.Texts

import android.content.Context
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.Maneuver
import com.example.konstanz.data.transit.WalkStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

/** A walk along the streets: its shape (lat/lon), length and turn-by-turn steps. */
class WalkRoute(val points: List<Pair<Double, Double>>, val meters: Double, val steps: List<WalkStep>)

/**
 * Walking directions on the phone, over the OpenStreetMap paths and streets of Konstanz
 * (assets/walk/konstanz-walk.bin, built by tools/walk-graph). A* from the nearest street to the start
 * to the nearest street to the destination, then the route is cut into steps where the street changes.
 */
class WalkRouter(private val context: Context) {
    private var graph: WalkGraph? = null
    private val mutex = Mutex()

    private suspend fun graph(): WalkGraph? = graph ?: mutex.withLock {
        graph ?: withContext(Dispatchers.IO) {
            runCatching { context.assets.open(ASSET).use { WalkGraph.read(it) } }.getOrNull()
        }?.also { graph = it }
    }

    /** Null when there's no network here (outside the map) or no way through. */
    suspend fun route(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, destination: String): WalkRoute? {
        val g = graph() ?: return null
        return withContext(Dispatchers.Default) { g.route(fromLat, fromLon, toLat, toLon, destination) }
    }

    companion object {
        const val ASSET = "walk/konstanz-walk.bin"
    }
}

internal class WalkGraph private constructor(
    private val names: Array<String>,
    private val vLat: DoubleArray,
    private val vLon: DoubleArray,
    private val ea: IntArray,
    private val eb: IntArray,
    private val eName: IntArray,
    private val eKind: ByteArray,
    private val eFlags: ByteArray,
    private val eLen: DoubleArray,
    private val shapeStart: IntArray,
    private val shapeLat: DoubleArray,
    private val shapeLon: DoubleArray,
) {
    // Adjacency (CSR): edges at each vertex.
    private val adjStart = IntArray(vLat.size + 1)
    private val adjEdge: IntArray
    // Edges per grid cell, for snapping.
    private val cells = HashMap<Long, IntArray>()

    init {
        for (e in ea.indices) { adjStart[ea[e] + 1]++; adjStart[eb[e] + 1]++ }
        for (i in 1..vLat.size) adjStart[i] += adjStart[i - 1]
        adjEdge = IntArray(adjStart[vLat.size])
        val fill = adjStart.copyOf()
        for (e in ea.indices) { adjEdge[fill[ea[e]]++] = e; adjEdge[fill[eb[e]]++] = e }

        val lists = HashMap<Long, MutableList<Int>>()
        for (e in ea.indices) {
            val (la, lo) = points(e)
            for (i in 0 until la.size - 1) {
                val y0 = cellY(minOf(la[i], la[i + 1])); val y1 = cellY(maxOf(la[i], la[i + 1]))
                val x0 = cellX(minOf(lo[i], lo[i + 1])); val x1 = cellX(maxOf(lo[i], lo[i + 1]))
                for (y in y0..y1) for (x in x0..x1) {
                    val list = lists.getOrPut(key(y, x)) { ArrayList(4) }
                    if (list.isEmpty() || list.last() != e) list += e
                }
            }
        }
        lists.forEach { (k, v) -> cells[k] = v.toIntArray() }
    }

    /** The edge's shape from a to b, both ends included. */
    private fun points(e: Int): Pair<DoubleArray, DoubleArray> {
        val n = shapeStart[e + 1] - shapeStart[e]
        val la = DoubleArray(n + 2)
        val lo = DoubleArray(n + 2)
        la[0] = vLat[ea[e]]; lo[0] = vLon[ea[e]]
        for (i in 0 until n) { la[i + 1] = shapeLat[shapeStart[e] + i]; lo[i + 1] = shapeLon[shapeStart[e] + i] }
        la[n + 1] = vLat[eb[e]]; lo[n + 1] = vLon[eb[e]]
        return la to lo
    }

    private class Snap(val edge: Int, val seg: Int, val lat: Double, val lon: Double, val fromA: Double, val dist: Double)

    /** Nearest point on the network within ~400 m. */
    private fun snap(lat: Double, lon: Double): Snap? {
        val cy = cellY(lat); val cx = cellX(lon)
        var best: Snap? = null
        for (r in 0..4) {
            for (y in cy - r..cy + r) for (x in cx - r..cx + r) {
                if (maxOf(abs(y - cy), abs(x - cx)) != r) continue
                val edges = cells[key(y, x)] ?: continue
                for (e in edges) {
                    val (la, lo) = points(e)
                    var along = 0.0
                    for (i in 0 until la.size - 1) {
                        val segLen = Geo.meters(la[i], lo[i], la[i + 1], lo[i + 1])
                        val t = project(lat, lon, la[i], lo[i], la[i + 1], lo[i + 1])
                        val pLat = la[i] + (la[i + 1] - la[i]) * t
                        val pLon = lo[i] + (lo[i + 1] - lo[i]) * t
                        // Busy roads and stairs only when nothing else is near.
                        val d = Geo.meters(lat, lon, pLat, pLon) + if (eKind[e].toInt() == STEPS) 15.0 else 0.0
                        if (best == null || d < best.dist) best = Snap(e, i, pLat, pLon, along + segLen * t, d)
                        along += segLen
                    }
                }
            }
            // Anything found within the rings searched so far is the nearest (cells are ~110 × 75 m).
            if (best != null && best.dist < r * 75.0) break
        }
        return best
    }

    private fun cost(e: Int): Double = eLen[e] * when (eKind[e].toInt()) {
        STEPS -> 1.4
        else -> 1.0
    }

    fun route(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, destination: String): WalkRoute? {
        val s = snap(fromLat, fromLon) ?: return null
        val t = snap(toLat, toLon) ?: return null
        val n = vLat.size
        val dist = DoubleArray(n) { Double.MAX_VALUE }
        val viaEdge = IntArray(n) { -1 }
        val viaVertex = IntArray(n) { -1 }
        val done = BooleanArray(n)
        val queue = PriorityQueue<Long>(256)
        fun h(v: Int) = Geo.meters(vLat[v], vLon[v], t.lat, t.lon)
        fun push(v: Int, d: Double, edge: Int, from: Int) {
            if (d >= dist[v]) return
            dist[v] = d; viaEdge[v] = edge; viaVertex[v] = from
            // Priority (cm) in the high bits, vertex in the low 26.
            queue += ((d + h(v)) * 100).toLong().shl(26) or v.toLong()
        }
        val scale = cost(s.edge) / eLen[s.edge].coerceAtLeast(0.01)
        push(ea[s.edge], s.fromA * scale, -1, -1)
        push(eb[s.edge], (eLen[s.edge] - s.fromA) * scale, -1, -1)

        val tScale = cost(t.edge) / eLen[t.edge].coerceAtLeast(0.01)
        var best = if (s.edge == t.edge) abs(s.fromA - t.fromA) * scale else Double.MAX_VALUE
        var bestEnd = -1 // -1 = along the shared edge
        while (queue.isNotEmpty()) {
            val v = (queue.poll()!! and 0x3FFFFFF).toInt()
            if (done[v]) continue
            done[v] = true
            if (dist[v] + h(v) >= best) break
            if (v == ea[t.edge] || v == eb[t.edge]) {
                val rest = (if (v == ea[t.edge]) t.fromA else eLen[t.edge] - t.fromA) * tScale
                if (dist[v] + rest < best) { best = dist[v] + rest; bestEnd = v }
            }
            for (i in adjStart[v] until adjStart[v + 1]) {
                val e = adjEdge[i]
                val w = if (ea[e] == v) eb[e] else ea[e]
                if (!done[w]) push(w, dist[v] + cost(e), e, v)
            }
        }
        if (best == Double.MAX_VALUE) return null

        // Pieces: (edge, lat/lon points in walking order).
        val pieces = ArrayList<Pair<Int, List<Pair<Double, Double>>>>()
        if (bestEnd == -1) {
            pieces += s.edge to partial(s.edge, s.fromA, t.fromA, s, t)
        } else {
            val chain = ArrayList<Pair<Int, Int>>() // (edge, vertex we arrive at)
            var v = bestEnd
            while (viaEdge[v] != -1) { chain += viaEdge[v] to v; v = viaVertex[v] }
            chain.reverse()
            val startVertex = v
            pieces += s.edge to partial(s.edge, s.fromA, if (startVertex == ea[s.edge]) 0.0 else eLen[s.edge], s, null)
            for ((e, arrive) in chain) {
                val (la, lo) = points(e)
                val pts = la.indices.map { la[it] to lo[it] }
                pieces += e to if (eb[e] == arrive) pts else pts.reversed()
            }
            pieces += t.edge to partial(t.edge, if (bestEnd == ea[t.edge]) 0.0 else eLen[t.edge], t.fromA, null, t)
        }
        val shape = ArrayList<Pair<Double, Double>>()
        shape += fromLat to fromLon
        pieces.forEach { (_, pts) -> pts.forEach { if (shape.last() != it) shape += it } }
        if (shape.last() != toLat to toLon) shape += toLat to toLon
        val meters = (1 until shape.size).sumOf { Geo.meters(shape[it - 1].first, shape[it - 1].second, shape[it].first, shape[it].second) }
        return WalkRoute(shape, meters, Directions(pieces.filter { it.second.size > 1 }, destination).steps())
    }

    /** Part of edge [e] from [from] to [to] metres along it (either direction). */
    private fun partial(e: Int, from: Double, to: Double, s: Snap?, t: Snap?): List<Pair<Double, Double>> {
        val (la, lo) = points(e)
        val along = DoubleArray(la.size)
        for (i in 1 until la.size) along[i] = along[i - 1] + Geo.meters(la[i - 1], lo[i - 1], la[i], lo[i])
        val lo2 = minOf(from, to); val hi = maxOf(from, to)
        val out = ArrayList<Pair<Double, Double>>()
        out += pointAt(la, lo, along, lo2, if (from <= to) s else t)
        for (i in la.indices) if (along[i] > lo2 && along[i] < hi) out += la[i] to lo[i]
        out += pointAt(la, lo, along, hi, if (from <= to) t else s)
        return if (from <= to) out else out.reversed()
    }

    private fun pointAt(la: DoubleArray, lo: DoubleArray, along: DoubleArray, d: Double, snap: Snap?): Pair<Double, Double> {
        if (snap != null) return snap.lat to snap.lon
        val i = along.indexOfFirst { it >= d }.let { if (it <= 0) return la[0] to lo[0] else it }
        val f = ((d - along[i - 1]) / (along[i] - along[i - 1]).coerceAtLeast(1e-6)).coerceIn(0.0, 1.0)
        return (la[i - 1] + (la[i] - la[i - 1]) * f) to (lo[i - 1] + (lo[i] - lo[i - 1]) * f)
    }

    /** Turn-by-turn steps: a new step wherever the way (its name, or its kind when unnamed) changes. */
    private inner class Directions(private val pieces: List<Pair<Int, List<Pair<Double, Double>>>>, private val destination: String) {
        private inner class Group(val name: String?, val kind: Int, val flags: Int, val points: MutableList<Pair<Double, Double>>) {
            val meters get() = (1 until points.size).sumOf { Geo.meters(points[it - 1].first, points[it - 1].second, points[it].first, points[it].second) }
            fun sameWay(o: Group) = if (name != null || o.name != null) name == o.name else kind == o.kind
        }

        fun steps(): List<WalkStep> {
            if (pieces.isEmpty()) return emptyList()
            var groups = ArrayList<Group>()
            for ((e, pts) in pieces) {
                val g = Group(eName[e].takeIf { it >= 0 }?.let { names[it] }, eKind[e].toInt(), eFlags[e].toInt(), pts.toMutableList())
                val last = groups.lastOrNull()
                if (last != null && (last.sameWay(g) && g.kind != CROSSING || last.kind == CROSSING && g.kind == CROSSING)) last.points += pts.drop(1) else groups += g
            }
            // Short unnamed connectors (a few metres of path between two streets) aren't worth a step.
            groups = ArrayList(groups.filterIndexed { i, g ->
                val keep = i == 0 || i == groups.lastIndex || g.name != null || g.kind == CROSSING || g.kind == STEPS || g.meters >= 15
                if (!keep) groups[i + 1].points.addAll(0, g.points.dropLast(1))
                keep
            })
            // Merge again after dropping (Schottenstraße · 5 m path · Schottenstraße).
            val merged = ArrayList<Group>()
            for (g in groups) {
                val last = merged.lastOrNull()
                if (last != null && (last.sameWay(g) && g.kind != CROSSING || last.kind == CROSSING && g.kind == CROSSING)) last.points += g.points.drop(1) else merged += g
            }
            // A few metres at either end (stepping onto the street) aren't steps of their own.
            var lead = 0.0
            while (merged.size > 1 && lead + merged[0].meters < 25) {
                lead += merged[0].meters
                merged[1].points.addAll(0, merged.removeAt(0).points.dropLast(1))
            }
            while (merged.size > 1 && merged.last().meters < 10 && merged.last().kind != CROSSING) {
                val end = merged.removeAt(merged.lastIndex)
                merged.last().points += end.points.drop(1)
            }

            val steps = ArrayList<WalkStep>()
            merged.forEachIndexed { i, g ->
                val at = g.points.first()
                val point = Geo.mapPoint(at.first, at.second)
                val meters = roundMeters(g.meters)
                if (i == 0) {
                    val dir = compass(bearingFrom(g.points))
                    val where = g.label()
                    steps += WalkStep(if (where != null) Texts.get(R.string.walk_head_on, dir, where) else Texts.get(R.string.walk_head, dir), meters, g.name, Maneuver.Depart, point)
                } else {
                    val turn = turnAngle(merged[i - 1].points, g.points)
                    val maneuver = when {
                        g.kind == STEPS -> Maneuver.Stairs
                        g.kind == CROSSING -> Maneuver.Cross
                        else -> maneuverFor(turn)
                    }
                    steps += WalkStep(instruction(maneuver, turn, g, merged.getOrNull(i + 1)), meters, g.name, maneuver, point)
                }
            }
            val end = merged.last().points.last()
            steps += WalkStep(Texts.get(R.string.walk_arrive, destination), 0, null, Maneuver.Arrive, Geo.mapPoint(end.first, end.second))
            return steps
        }

        private fun Group.label(): String? = name ?: when {
            flags and FLAG_BRIDGE != 0 -> Texts.get(R.string.walk_the_bridge)
            kind == PEDESTRIAN -> Texts.get(R.string.walk_the_pedestrian_zone)
            kind == FOOTPATH -> Texts.get(R.string.walk_the_footpath)
            kind == STEPS -> Texts.get(R.string.walk_the_stairs)
            else -> null
        }

        private fun instruction(m: Maneuver, turn: Double, g: Group, next: Group?): String {
            val target = g.label()
            fun turn(plain: Int, onto: Int) = if (target != null) Texts.get(onto, target) else Texts.get(plain)
            return when (m) {
                Maneuver.Stairs -> when (maneuverFor(turn)) {
                    Maneuver.Left, Maneuver.SharpLeft, Maneuver.SlightLeft -> Texts.get(R.string.walk_left_stairs)
                    Maneuver.Right, Maneuver.SharpRight, Maneuver.SlightRight -> Texts.get(R.string.walk_right_stairs)
                    else -> Texts.get(R.string.walk_take_stairs)
                }
                Maneuver.Cross -> next?.name?.takeIf { it != g.name }?.let { Texts.get(R.string.walk_cross_to, it) } ?: Texts.get(R.string.walk_cross)
                Maneuver.Straight -> if (g.name != null) Texts.get(R.string.walk_continue_onto, g.name) else turn(R.string.walk_straight, R.string.walk_straight_onto)
                Maneuver.SlightLeft -> turn(R.string.walk_bear_left, R.string.walk_bear_left_onto)
                Maneuver.SlightRight -> turn(R.string.walk_bear_right, R.string.walk_bear_right_onto)
                Maneuver.Left -> turn(R.string.walk_left, R.string.walk_left_onto)
                Maneuver.Right -> turn(R.string.walk_right, R.string.walk_right_onto)
                Maneuver.SharpLeft -> turn(R.string.walk_sharp_left, R.string.walk_sharp_left_onto)
                Maneuver.SharpRight -> turn(R.string.walk_sharp_right, R.string.walk_sharp_right_onto)
                Maneuver.UTurn -> turn(R.string.walk_u_turn, R.string.walk_u_turn_onto)
                else -> turn(R.string.walk_continue, R.string.walk_continue_onto)
            }
        }
    }

    companion object {
        private const val CELL = 0.001
        private val COMPASS = intArrayOf(
            R.string.compass_n, R.string.compass_ne, R.string.compass_e, R.string.compass_se,
            R.string.compass_s, R.string.compass_sw, R.string.compass_w, R.string.compass_nw,
        )
        private const val STEPS = 2
        private const val CROSSING = 4
        private const val PEDESTRIAN = 3
        private const val FOOTPATH = 1
        private const val FLAG_BRIDGE = 1

        private fun cellY(lat: Double) = floor(lat / CELL).toInt()
        private fun cellX(lon: Double) = floor(lon / CELL).toInt()
        private fun key(y: Int, x: Int) = y.toLong().shl(32) or (x.toLong() and 0xFFFFFFFFL)

        /** 0…1 position of the point nearest to (lat, lon) on segment a–b. */
        private fun project(lat: Double, lon: Double, aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
            val k = cos(Math.toRadians(lat))
            val bx = (bLon - aLon) * k; val by = bLat - aLat
            val px = (lon - aLon) * k; val py = lat - aLat
            val l2 = bx * bx + by * by
            return if (l2 == 0.0) 0.0 else ((px * bx + py * by) / l2).coerceIn(0.0, 1.0)
        }

        private fun bearing(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
            val k = cos(Math.toRadians(a.first))
            return (Math.toDegrees(atan2((b.second - a.second) * k, b.first - a.first)) + 360) % 360
        }

        /** Direction of the first ~20 m. */
        private fun bearingFrom(pts: List<Pair<Double, Double>>): Double {
            var d = 0.0
            for (i in 1 until pts.size) {
                d += Geo.meters(pts[i - 1].first, pts[i - 1].second, pts[i].first, pts[i].second)
                if (d >= 20 || i == pts.lastIndex) return bearing(pts[0], pts[i])
            }
            return 0.0
        }

        /** Direction of the last ~20 m. */
        private fun bearingInto(pts: List<Pair<Double, Double>>): Double {
            var d = 0.0
            for (i in pts.lastIndex - 1 downTo 0) {
                d += Geo.meters(pts[i].first, pts[i].second, pts[i + 1].first, pts[i + 1].second)
                if (d >= 20 || i == 0) return bearing(pts[i], pts.last())
            }
            return 0.0
        }

        /** Signed turn in degrees: + right, − left. */
        private fun turnAngle(before: List<Pair<Double, Double>>, after: List<Pair<Double, Double>>): Double =
            ((bearingFrom(after) - bearingInto(before) + 540) % 360) - 180

        private fun maneuverFor(turn: Double): Maneuver = when {
            abs(turn) < 25 -> Maneuver.Straight
            abs(turn) > 165 -> Maneuver.UTurn
            turn in 25.0..60.0 -> Maneuver.SlightRight
            turn in 60.0..135.0 -> Maneuver.Right
            turn > 0 -> Maneuver.SharpRight
            turn in -60.0..-25.0 -> Maneuver.SlightLeft
            turn in -135.0..-60.0 -> Maneuver.Left
            else -> Maneuver.SharpLeft
        }

        private fun compass(b: Double) =
            Texts.get(COMPASS[((b + 22.5) / 45).toInt() % 8])

        private fun roundMeters(m: Double): Int = when {
            m < 100 -> ((m / 5).roundToInt() * 5).coerceAtLeast(5)
            else -> (m / 10).roundToInt() * 10
        }

        fun read(input: java.io.InputStream): WalkGraph {
            val d = DataInputStream(BufferedInputStream(input, 1 shl 16))
            val magic = ByteArray(4).also { d.readFully(it) }
            require(String(magic) == "KWG1") { "not a walk graph" }
            val names = Array(d.readInt()) { d.readUTF() }
            val nv = d.readInt()
            val vLat = DoubleArray(nv); val vLon = DoubleArray(nv)
            for (i in 0 until nv) { vLat[i] = d.readInt() / 1e7; vLon[i] = d.readInt() / 1e7 }
            val ne = d.readInt()
            val ea = IntArray(ne); val eb = IntArray(ne); val name = IntArray(ne)
            val kind = ByteArray(ne); val flags = ByteArray(ne); val len = DoubleArray(ne)
            val shapeStart = IntArray(ne + 1)
            var sLat = DoubleArray(ne * 2); var sLon = DoubleArray(ne * 2)
            var count = 0
            for (e in 0 until ne) {
                ea[e] = d.readInt(); eb[e] = d.readInt(); name[e] = d.readInt()
                kind[e] = d.readByte(); flags[e] = d.readByte(); len[e] = d.readInt() / 100.0
                val n = d.readUnsignedShort()
                if (count + n > sLat.size) {
                    sLat = sLat.copyOf(maxOf(sLat.size * 2, count + n)); sLon = sLon.copyOf(sLat.size)
                }
                for (i in 0 until n) { sLat[count] = d.readInt() / 1e7; sLon[count] = d.readInt() / 1e7; count++ }
                shapeStart[e + 1] = count
            }
            return WalkGraph(names, vLat, vLon, ea, eb, name, kind, flags, len, shapeStart, sLat, sLon)
        }
    }
}
