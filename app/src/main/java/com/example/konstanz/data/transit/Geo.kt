package com.example.konstanz.data.transit

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.tan

/**
 * WGS84 ↔ the app's map units. Map units are Web Mercator (the real map's projection) scaled so one
 * unit ≈ 5.87 m on the ground around Konstanz, anchored on Konstanz Bahnhof (47.6588 N, 9.1771 E)
 * at (660, 868) — the same framing as the design's map drawing, so both maps share one camera.
 * The drawing itself is not a projection: design positions on it are only approximately real.
 */
object Geo {
    private const val ANCHOR_LAT = 47.6588
    private const val ANCHOR_LON = 9.1771
    private const val ANCHOR_X = 660.0
    private const val ANCHOR_Y = 868.0
    private const val EARTH_RADIUS = 6_378_137.0
    private const val EARTH_CIRCUMFERENCE = 2 * PI * EARTH_RADIUS
    /** Ground metres per map unit at the anchor. */
    private const val GROUND_M_PER_UNIT = 5.87

    /** Mercator metres per map unit (ground metres grow by 1 / cos(lat) in Mercator). */
    private val MERCATOR_M_PER_UNIT = GROUND_M_PER_UNIT / cos(Math.toRadians(ANCHOR_LAT))
    private val ANCHOR_MX = mercatorX(ANCHOR_LON)
    private val ANCHOR_MY = mercatorY(ANCHOR_LAT)

    /** Walking pace, metres per minute. */
    const val WALK_M_PER_MIN = 80.0
    /** Streets are not straight lines: walking distance ≈ 1.25 × straight distance. */
    private const val DETOUR = 1.25

    private fun mercatorX(lon: Double) = EARTH_RADIUS * Math.toRadians(lon)
    private fun mercatorY(lat: Double) = EARTH_RADIUS * ln(tan(PI / 4 + Math.toRadians(lat) / 2))

    fun latLon(point: MapPoint): Pair<Double, Double> {
        val mx = ANCHOR_MX + (point.x - ANCHOR_X) * MERCATOR_M_PER_UNIT
        val my = ANCHOR_MY - (point.y - ANCHOR_Y) * MERCATOR_M_PER_UNIT
        val lat = Math.toDegrees(2 * atan(exp(my / EARTH_RADIUS)) - PI / 2)
        val lon = Math.toDegrees(mx / EARTH_RADIUS)
        return lat to lon
    }

    fun mapPoint(lat: Double, lon: Double) = MapPoint(
        x = (ANCHOR_X + (mercatorX(lon) - ANCHOR_MX) / MERCATOR_M_PER_UNIT).toFloat(),
        y = (ANCHOR_Y - (mercatorY(lat) - ANCHOR_MY) / MERCATOR_M_PER_UNIT).toFloat(),
    )

    /**
     * MapLibre zoom that shows [pxPerUnit] physical pixels per map unit on a screen of [density].
     * MapLibre: 512 · 2^zoom logical pixels span the Mercator world.
     */
    fun mapLibreZoom(pxPerUnit: Float, density: Float): Double =
        log2(pxPerUnit / density / MERCATOR_M_PER_UNIT * EARTH_CIRCUMFERENCE / 512.0)

    /** Straight-line distance in metres (equirectangular; exact enough inside a city). */
    fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat = Math.toRadians((lat1 + lat2) / 2)
        val kx = 111_320.0 * cos(lat)
        val ky = 111_132.954 - 559.822 * cos(2 * lat) // metres per degree of latitude here
        return hypot((lon2 - lon1) * kx, (lat2 - lat1) * ky)
    }

    /** Estimated walking distance in metres. */
    fun walkMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double) = meters(lat1, lon1, lat2, lon2) * DETOUR

    fun walkMinutes(meters: Double): Int = kotlin.math.ceil(meters / WALK_M_PER_MIN).toInt().coerceAtLeast(1)
}
