package com.example.konstanz.ui.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.example.konstanz.data.transit.Journey
import com.example.konstanz.data.transit.Leg
import com.example.konstanz.ui.map.MapOverlayScope
import com.example.konstanz.ui.theme.Location
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.IsDark
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White

/** Unselected alternatives (design system → Route lines: "Grey, unselected; tap to select"). */
private val AlternativeGrey get() = if (IsDark) Color(0xFF4A4E57) else Color(0xFFA9ADB5)

/** Thin edge around lines in the map's own tone: separates the line from the street without hiding it. */
private val Casing get() = if (IsDark) Color.Black else White

/**
 * Draws journeys on the map with constant on-screen widths (design system → Route lines):
 * bus = solid 5 dp on an 8 dp casing (so the street stays visible); walking = 4 dp dots every 9 dp;
 * alternatives = grey 4 dp. The selected journey is drawn last, on top.
 * With a [focus] leg (one walk shown on the map), the rest of the journey is faded and the focus drawn darker.
 */
@Composable
fun MapOverlayScope.RouteLayer(journeys: List<Journey>, selected: Journey?, focus: Leg? = null) {
    val parsed = remember(journeys) {
        journeys.associate { j -> j.id to j.legs.mapNotNull { leg -> leg.path?.let { leg to PathParser().parsePathString(it).toPath() } } }
    }
    Canvas(Modifier.fillMaxSize()) {
        val matrix = Matrix().apply {
            translate(projection.origin().x, projection.origin().y)
            scale(projection.scale, projection.scale)
        }
        fun screen(path: Path) = Path().apply { addPath(path); transform(matrix) }

        journeys.filter { it.id != selected?.id }.forEach { j ->
            parsed[j.id].orEmpty().forEach { (_, p) ->
                val sp = screen(p)
                drawPath(sp, Casing, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(sp, AlternativeGrey, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        if (selected != null) {
            val legs = parsed[selected.id].orEmpty()
            legs.filter { focus == null || it.first != focus }.forEach { (leg, p) -> drawLeg(leg, screen(p), if (focus == null) 1f else 0.3f) }
            legs.filter { focus != null && it.first == focus }.forEach { (leg, p) -> drawLeg(leg, screen(p), focused = true) }
        }
    }
}

private fun DrawScope.drawLeg(leg: Leg, path: Path, alpha: Float = 1f, focused: Boolean = false) {
    when (leg) {
        is Leg.Ride -> {
            drawPath(path, Casing, alpha, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, Primary, alpha, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        is Leg.Walk -> if (focused) drawFocusedWalk(path) else {
            // Round caps on zero-length dashes = dots.
            val dots = PathEffect.dashPathEffect(floatArrayOf(0f, 9.dp.toPx()))
            drawPath(path, Casing, alpha, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, pathEffect = dots))
            drawPath(path, Ink3, alpha, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, pathEffect = dots))
        }
    }
}

/** The walk being looked at: a solid blue line on a white casing, with white arrows showing which way to go. */
private fun DrawScope.drawFocusedWalk(path: Path) {
    val round = Stroke(13.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path, Color(0x33000000), style = Stroke(15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, White, style = round)
    drawPath(path, Location, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    val measure = PathMeasure().apply { setPath(path, false) }
    val gap = 44.dp.toPx()
    val arm = 3.dp.toPx()
    var d = gap / 2
    while (d < measure.length - 8.dp.toPx()) {
        val p = measure.getPosition(d)
        val t = measure.getTangent(d)
        // Chevron ">" pointing along the tangent t, with normal n.
        val n = Offset(-t.y, t.x)
        val tip = p + t * arm
        val back = p - t * arm
        val chevron = Path().apply {
            moveTo(back.x + n.x * arm * 1.3f, back.y + n.y * arm * 1.3f)
            lineTo(tip.x, tip.y)
            lineTo(back.x - n.x * arm * 1.3f, back.y - n.y * arm * 1.3f)
        }
        drawPath(chevron, White, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        d += gap
    }
}
