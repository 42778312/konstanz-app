package com.example.konstanz.data.walk

import com.example.konstanz.data.transit.Maneuver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Walking directions on the real Konstanz network (assets/walk, built by tools/walk-graph). */
class WalkGraphTest {
    private val file = File("src/main/assets/${WalkRouter.ASSET}")
    private val graph by lazy { file.inputStream().use { WalkGraph.read(it) } }

    @Test
    fun schottenplatzToHtwgFollowsTheStreets() {
        assumeTrue("walk graph not built", file.exists())
        // Schottenplatz bus stop → HTWG main building.
        val route = requireNotNull(graph.route(47.66395, 9.16904, 47.66813, 9.16900, "HTWG"))
        val straight = com.example.konstanz.data.transit.Geo.meters(47.66395, 9.16904, 47.66813, 9.16900)
        println("%.0f m (straight %.0f m), %d points".format(route.meters, straight, route.points.size))
        route.steps.forEach { println("  ${it.maneuver}  ${it.instruction}  · ${it.meters} m") }
        assertTrue("follows streets, not a straight line", route.points.size > 4)
        assertTrue(route.meters >= straight && route.meters < straight * 2)
        assertEquals(Maneuver.Depart, route.steps.first().maneuver)
        assertEquals(Maneuver.Arrive, route.steps.last().maneuver)
    }

    @Test
    fun bahnhofToMuensterHasNamedSteps() {
        assumeTrue("walk graph not built", file.exists())
        val route = requireNotNull(graph.route(47.65866, 9.17783, 47.66336, 9.17567, "Münster"))
        route.steps.forEach { println("  ${it.maneuver}  ${it.instruction}  · ${it.meters} m") }
        assertTrue(route.steps.any { it.street != null })
    }
}
