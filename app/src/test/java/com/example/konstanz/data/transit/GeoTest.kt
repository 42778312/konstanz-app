package com.example.konstanz.data.transit

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {
    @Test
    fun anchorIsKonstanzBahnhof() {
        val (lat, lon) = Geo.latLon(MapPoint(660f, 868f))
        assertEquals(47.6588, lat, 1e-6)
        assertEquals(9.1771, lon, 1e-6)
    }

    @Test
    fun roundTrips() {
        val p = Geo.mapPoint(47.6896, 9.1860) // Universität
        val (lat, lon) = Geo.latLon(p)
        assertEquals(47.6896, lat, 1e-5)
        assertEquals(9.1860, lon, 1e-5)
    }

    @Test
    fun oneUnitIsAboutSixMetres() {
        val (lat1, lon1) = Geo.latLon(MapPoint(660f, 868f))
        val (lat2, lon2) = Geo.latLon(MapPoint(760f, 868f))
        val (lat3, lon3) = Geo.latLon(MapPoint(660f, 968f))
        assertEquals(587.0, Geo.meters(lat1, lon1, lat2, lon2), 3.0)
        assertEquals(587.0, Geo.meters(lat1, lon1, lat3, lon3), 3.0)
    }

    @Test
    fun zoomMatchesScale() {
        // One zoom step doubles the pixels per unit.
        assertEquals(1.0, Geo.mapLibreZoom(4f, 2f) - Geo.mapLibreZoom(2f, 2f), 1e-9)
        // Design framing (487.5 units ≈ 2.9 km across a 1080 px phone, density 2.625) ≈ zoom 12.9.
        assertEquals(12.9, Geo.mapLibreZoom(1080f / 487.5f, 2.625f), 0.1)
    }
}
