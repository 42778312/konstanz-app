package com.example.konstanz.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow as TextShadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Figtree
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Location
import com.example.konstanz.ui.theme.MapDistrictLabel
import com.example.konstanz.ui.theme.MapWaterLabel
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White

// Marker glyphs from the design system's "Map markers" row. Sizes are the drawn glyph;
// tappable versions (Part E) wrap them in a 44 dp hit area.

private val ShadowInk = Color(0xFF16181D)

internal fun Modifier.markerShadow(shape: androidx.compose.ui.graphics.Shape, radius: Dp = 4.dp, alpha: Float = 0.3f) =
    dropShadow(shape, Shadow(radius = radius, offset = DpOffset(0.dp, 1.dp), color = ShadowInk, alpha = alpha))

/** Bus stop at zoom ≥ 15: red tile with a bus glyph. */
@Composable
fun StopTileMarker(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .size(26.dp)
            .markerShadow(shape)
            .background(Primary, shape)
            .border(2.dp, White, shape),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 15.dp, tint = White, strokeWidth = 2.4f)
    }
}

/** Bus stop at zoom 13–15: small ring. */
@Composable
fun StopDotMarker(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .background(White, CircleShape)
            .border(3.dp, Primary, CircleShape)
    )
}

/** Route start: white dot with a thick ink ring. */
@Composable
fun StartMarker(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(20.dp)
            .markerShadow(CircleShape)
            .background(White, CircleShape)
            .border(5.dp, Ink, CircleShape)
    )
}

/** Route destination: ink diamond with a white edge. */
@Composable
fun DestinationMarker(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier
            .size(20.dp)
            .rotate(45f)
            .markerShadow(shape, radius = 5.dp, alpha = 0.35f)
            .background(Ink, shape)
            .border(3.dp, White, shape)
    )
}

/** My location: accuracy circle ([accuracy] dp across), heading cone and blue dot. */
@Composable
fun LocationPuck(modifier: Modifier = Modifier, accuracy: Dp = 140.dp, showHeading: Boolean = true) {
    Box(modifier.size(accuracy), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(accuracy)) {
            val c = center
            drawCircle(Location.copy(alpha = 0.12f), radius = size.minDimension / 2f)
            drawCircle(Location.copy(alpha = 0.30f), radius = size.minDimension / 2f - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            if (!showHeading) return@Canvas
            // Heading cone: 44 dp wide, 44 dp tall, pointing up.
            val half = 22.dp.toPx()
            val cone = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x - half, c.y - 2 * half)
                arcTo(Rect(Offset(c.x, c.y - 2 * half), half), 180f, 180f, false)
                close()
            }
            drawPath(cone, Location.copy(alpha = 0.22f))
        }
        Box(
            Modifier
                .size(22.dp)
                .markerShadow(CircleShape, radius = 6.dp, alpha = 0.35f)
                .background(White, CircleShape)
                .padding(3.5.dp)
                .background(Location, CircleShape)
        )
    }
}

/** Expanding ring behind the location puck (the design's kt-pulse: 2.4 s, scale .6→1.8, fade out). */
@Composable
fun LocationPulse(modifier: Modifier = Modifier, diameter: Dp = 220.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse-t",
    )
    Box(
        modifier
            .size(diameter)
            .graphicsLayer {
                val s = 0.6f + 1.2f * t
                scaleX = s
                scaleY = s
                alpha = 0.55f * (1f - t)
            }
            .border(2.dp, Location.copy(alpha = 0.35f), CircleShape)
    )
}

private val labelHalo = TextShadow(color = White, offset = Offset.Zero, blurRadius = 6f)

/** District name, e.g. ALTSTADT. */
@Composable
fun DistrictLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier,
        style = TextStyle(
            fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            letterSpacing = 0.12.em, color = MapDistrictLabel, shadow = labelHalo,
        ),
        maxLines = 1,
        softWrap = false,
    )
}

/** Water name, e.g. Bodensee. */
@Composable
fun WaterLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = TextStyle(
            fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic,
            fontSize = 13.sp, color = MapWaterLabel, shadow = labelHalo,
        ),
        maxLines = 1,
        softWrap = false,
    )
}

/** Several stops too close to show apart: red disc with the count and a soft halo (design: "Cluster"). */
@Composable
fun ClusterMarker(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .dropShadow(CircleShape, Shadow(radius = 6.dp, offset = DpOffset(0.dp, 2.dp), color = ShadowInk, alpha = 0.25f))
            .dropShadow(CircleShape, Shadow(radius = 0.dp, spread = 5.dp, color = Primary, alpha = 0.18f))
            .background(Primary, CircleShape)
            .border(3.dp, White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$count",
            style = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = White),
        )
    }
}

/** Name of a major stop printed on the map (13 sp bold ink with a white halo). */
@Composable
fun StopNameLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = TextStyle(
            fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            color = Ink, shadow = labelHalo,
        ),
        maxLines = 1,
        softWrap = false,
    )
}

/**
 * Dropped pin (artboard 10): red teardrop with a white edge and centre dot, pulsing halo at its tip.
 * The box is 104 dp tall with the tip exactly in its centre, so `Modifier.at(x, y)` puts the tip on the point.
 */
@Composable
fun DroppedPin(modifier: Modifier = Modifier, pulse: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "pin-pulse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pin-pulse-t",
    )
    Box(modifier.size(width = 56.dp, height = 104.dp)) {
        // 56 dp halo centred on the tip (the box centre).
        if (pulse) Box(
            Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .graphicsLayer {
                    val s = 0.6f + 1.2f * t
                    scaleX = s
                    scaleY = s
                    alpha = 0.55f * (1f - t)
                }
                .background(Primary.copy(alpha = 0.25f), CircleShape)
        )
        // Pin drawn in the top half; its tip (y = 50 of 52) touches the centre.
        Canvas(Modifier.align(Alignment.TopCenter).padding(top = 2.dp).size(width = 40.dp, height = 52.dp)) {
            val sx = size.width / 40f
            val sy = size.height / 52f
            val pin = Path().apply {
                moveTo(20f * sx, 50f * sy)
                cubicTo(20f * sx, 50f * sy, 4f * sx, 32f * sy, 4f * sx, 19f * sy)
                arcTo(Rect(4f * sx, 3f * sy, 36f * sx, 35f * sy), 180f, 180f, false)
                cubicTo(36f * sx, 32f * sy, 20f * sx, 50f * sy, 20f * sx, 50f * sy)
                close()
            }
            drawPath(pin, Primary)
            drawPath(pin, White, style = Stroke(width = 3f * sx))
            drawCircle(White, radius = 6f * sx, center = Offset(20f * sx, 19f * sy))
        }
    }
}

/**
 * The stop whose sheet is open (artboard 12): a bigger 44 dp tile with a white edge, red glow and a
 * pointer underneath. Place with `Modifier.at(x, y, anchorBottom = true)`: the pointer tip marks the stop.
 */
@Composable
fun SelectedStopMarker(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(13.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .dropShadow(shape, Shadow(radius = 14.dp, offset = DpOffset(0.dp, 4.dp), color = Primary, alpha = 0.45f))
                .background(Primary, shape)
                .border(3.dp, White, shape),
            contentAlignment = Alignment.Center,
        ) {
            KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 22.dp, tint = White, strokeWidth = 2.4f)
        }
        Canvas(Modifier.size(width = 14.dp, height = 8.dp)) {
            val tri = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(tri, White)
        }
    }
}
