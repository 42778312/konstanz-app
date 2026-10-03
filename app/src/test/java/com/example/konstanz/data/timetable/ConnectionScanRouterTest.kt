package com.example.konstanz.data.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionScanRouterTest {
    // Stations 1–5. Trip 10: 1 → 2 → 3. Trip 20: 3 → 4 (a change at 3). Trip 30: 1 → 4, slow.
    // Trip 40 leaves from 5, a 2-minute walk from 2.
    private val connections = listOf(
        Connection(10, 1, 2, departure = 600, arrival = 605),
        Connection(30, 1, 4, departure = 601, arrival = 640),
        Connection(10, 2, 3, departure = 605, arrival = 610),
        Connection(40, 5, 4, departure = 608, arrival = 620),
        Connection(20, 3, 4, departure = 610, arrival = 615), // too tight: 1-minute change
        Connection(21, 3, 4, departure = 611, arrival = 616),
    ).sortedBy { it.departure }
    private val footpaths = mapOf(2 to listOf(Access(5, 2)), 5 to listOf(Access(2, 2)))
    private val router = ConnectionScanRouter(connections, footpaths, minChange = 1)

    @Test
    fun staysSeatedAndChangesWithBuffer() {
        val r = router.route(listOf(Access(1, 0)), listOf(Access(4, 0)), departAt = 590)!!
        // 1 → 3 on trip 10, change (1 min), 3 → 4 on trip 21: arrives 616; the walk via 5 arrives 620.
        assertEquals(616, r.arrival)
        assertEquals(
            listOf(RoutePart.Ride(10, 1, 3, 600, 610), RoutePart.Ride(21, 3, 4, 611, 616)),
            r.parts,
        )
    }

    @Test
    fun usesFootpathTransfer() {
        val noChangeAt3 = ConnectionScanRouter(connections.filter { it.trip != 20 && it.trip != 21 }, footpaths)
        val r = noChangeAt3.route(listOf(Access(1, 0)), listOf(Access(4, 0)), departAt = 590)!!
        assertEquals(620, r.arrival)
        assertEquals(RoutePart.Transfer(2, 5, 2), r.parts[1])
    }

    @Test
    fun accessAndEgressWalksCount() {
        // Leaving at 598 with a 3-minute walk misses trip 10 (600) and catches trip 30 (601).
        val r = router.route(listOf(Access(1, 3)), listOf(Access(4, 4)), departAt = 598)!!
        assertEquals(RoutePart.Ride(30, 1, 4, 601, 640), r.parts.single())
        assertEquals(644, r.arrival)
        assertEquals(598, r.leaveAt)
    }

    @Test
    fun unreachableReturnsNull() {
        assertNull(router.route(listOf(Access(1, 0)), listOf(Access(4, 0)), departAt = 700))
    }
}
