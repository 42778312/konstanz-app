package com.example.konstanz.ui.splash

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import com.example.konstanz.data.SeasonalEvent
import com.example.konstanz.ui.theme.White
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Colours of the splash screen for an event. [content] is the text / loading-bar colour on [background]. */
internal data class SeasonalLook(val background: Color, val content: Color = White, val darkIcons: Boolean = false)

internal val SeasonalEvent.look: SeasonalLook
    get() = when (this) {
        SeasonalEvent.NewYear -> SeasonalLook(Color(0xFF111A3A))
        SeasonalEvent.Fasnacht -> SeasonalLook(Color(0xFF6A2C91))
        SeasonalEvent.Valentine -> SeasonalLook(Color(0xFFD6336C))
        // Fixed dark text: the theme's Ink turns light in dark mode.
        SeasonalEvent.Easter -> SeasonalLook(Color(0xFFFFF1D6), content = Color(0xFF2B2420), darkIcons = true)
        SeasonalEvent.MayDay -> SeasonalLook(Color(0xFF2F8F5B))
        SeasonalEvent.Seenachtfest -> SeasonalLook(Color(0xFF0D2A45))
        SeasonalEvent.UnityDay -> SeasonalLook(Color(0xFF18191C))
        SeasonalEvent.Halloween -> SeasonalLook(Color(0xFF2A1540))
        SeasonalEvent.StMartin -> SeasonalLook(Color(0xFF1B2340))
        SeasonalEvent.Advent -> SeasonalLook(Color(0xFF1A2B4C))
        SeasonalEvent.Christmas -> SeasonalLook(Color(0xFF0E4A33))
        // Seasons: calmer than the events.
        SeasonalEvent.Winter -> SeasonalLook(Color(0xFF2B4A6F))
        SeasonalEvent.Spring -> SeasonalLook(Color(0xFF4E9A68))
        SeasonalEvent.Summer -> SeasonalLook(Color(0xFF0E7C9A))
        SeasonalEvent.Autumn -> SeasonalLook(Color(0xFF9A4E1C))
    }

/** Time shown when animations are off (and in previews): every effect is in full swing by then. */
private const val STILL_TIME = 2.5f

/**
 * The event's animated layer, drawn behind the splash content. Everything is a function of time,
 * so there is no state to keep; with animations turned off in the system it is one still frame.
 * [candles] = Advent candles to light (1–4).
 */
@Composable
internal fun SeasonalEffect(event: SeasonalEvent, modifier: Modifier = Modifier, candles: Int = 1) {
    val context = LocalContext.current
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val time by produceState(STILL_TIME) {
        if (still) return@produceState
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = STILL_TIME + (it - start) / 1e9f }
    }
    val particles = remember(event) { Particle.many(event.ordinal) }
    Canvas(modifier) {
        when (event) {
            SeasonalEvent.NewYear -> fireworks(time, NEW_YEAR_SPARKS, top = 0.05f, bottom = 0.3f)
            SeasonalEvent.Seenachtfest -> fireworks(time, LAKE_SPARKS, top = 0.04f, bottom = 0.28f)
            SeasonalEvent.Fasnacht -> particles.forEach { confetti(it, time) }
            SeasonalEvent.Valentine -> particles.take(22).forEach { heart(it, time) }
            SeasonalEvent.Easter -> particles.take(16).forEach { egg(it, time) }
            SeasonalEvent.MayDay -> particles.take(30).forEach { petal(it, time) }
            SeasonalEvent.UnityDay -> flagRibbon(time)
            SeasonalEvent.Halloween -> { moon(); particles.take(9).forEach { bat(it, time) } }
            SeasonalEvent.StMartin -> lanterns(time)
            SeasonalEvent.Advent -> { particles.take(24).forEach { snow(it, time, 0.5f) }; adventWreath(time, candles) }
            SeasonalEvent.Christmas -> { particles.forEach { snow(it, time, 0.8f) }; star(time) }
            SeasonalEvent.Winter -> particles.take(20).forEach { snow(it, time, 0.45f) }
            SeasonalEvent.Spring -> particles.take(14).forEach { petal(it, time) }
            SeasonalEvent.Summer -> { sun(time); particles.take(18).forEach { sparkle(it, time) } }
            SeasonalEvent.Autumn -> particles.take(16).forEach { leaf(it, time) }
        }
    }
}

/** Fixed random values for one particle; positions are fractions of the screen. */
private class Particle(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float, val pick: Int) {
    companion object {
        fun many(seed: Int) = Random(seed + 7).let { r ->
            List(44) { Particle(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.6f, r.nextFloat(), r.nextFloat() * 2 * PI.toFloat(), r.nextInt(100)) }
        }
    }

    /** Falling (or rising) position that wraps around the screen, swaying sideways. */
    fun drift(scope: DrawScope, t: Float, fall: Float, sway: Float, rising: Boolean = false): Offset {
        val travel = (y + t * fall * speed) % 1.1f
        val fy = if (rising) 1.05f - travel else travel - 0.05f
        val fx = x + sway * sin(t * 1.3f + phase)
        return Offset(fx * scope.size.width, fy * scope.size.height)
    }
}

// ---------- particles ----------

private fun DrawScope.snow(p: Particle, t: Float, alpha: Float) {
    val r = (1.5f + p.size * 3f) * density
    drawCircle(White.copy(alpha = alpha * (0.5f + p.size * 0.5f)), r, p.drift(this, t, 0.08f, 0.02f))
}

private val CONFETTI = listOf(Color(0xFFFFD23F), Color(0xFF3BCEAC), Color(0xFFEE4266), Color(0xFF4CC9F0), Color.White)

private fun DrawScope.confetti(p: Particle, t: Float) {
    val at = p.drift(this, t, 0.16f, 0.03f)
    val w = (7f + p.size * 6f) * density
    rotate(t * 160f * (if (p.pick % 2 == 0) 1 else -1) + p.phase * 57f, at) {
        // Squashed by a cosine so the pieces seem to flip as they fall.
        val h = w * 0.5f * (0.3f + 0.7f * kotlin.math.abs(cos(t * 4f + p.phase)))
        drawRect(CONFETTI[p.pick % CONFETTI.size], Offset(at.x - w / 2, at.y - h / 2), Size(w, h))
    }
}

private fun DrawScope.heart(p: Particle, t: Float) {
    val at = p.drift(this, t, 0.07f, 0.025f, rising = true)
    val s = (10f + p.size * 12f) * density
    val path = Path().apply {
        moveTo(0f, s * 0.3f)
        cubicTo(0f, 0f, -s * 0.5f, 0f, -s * 0.5f, s * 0.3f)
        cubicTo(-s * 0.5f, s * 0.6f, 0f, s * 0.75f, 0f, s)
        cubicTo(0f, s * 0.75f, s * 0.5f, s * 0.6f, s * 0.5f, s * 0.3f)
        cubicTo(s * 0.5f, 0f, 0f, 0f, 0f, s * 0.3f)
        close()
    }
    translate(at.x, at.y - s / 2) { drawPath(path, (if (p.pick % 3 == 0) Color(0xFFFFB3C8) else White).copy(alpha = 0.35f + p.size * 0.4f)) }
}

private val EGGS = listOf(Color(0xFFF7A8B8), Color(0xFFA8D8F0), Color(0xFFB8E0A8), Color(0xFFF9D77E), Color(0xFFD0B8F0))

private fun DrawScope.egg(p: Particle, t: Float) {
    val at = p.drift(this, t, 0.05f, 0.015f)
    val w = (16f + p.size * 10f) * density
    val h = w * 1.3f
    rotate(sin(t * 1.5f + p.phase) * 18f, at) {
        drawOval(EGGS[p.pick % EGGS.size], Offset(at.x - w / 2, at.y - h / 2), Size(w, h))
        // A white band with a zig-zag look.
        drawRect(White.copy(alpha = 0.8f), Offset(at.x - w / 2 + w * 0.08f, at.y - h * 0.06f), Size(w * 0.84f, h * 0.12f))
    }
}

private fun DrawScope.petal(p: Particle, t: Float) {
    val at = p.drift(this, t, 0.06f, 0.05f)
    val w = (8f + p.size * 6f) * density
    rotate(t * 70f + p.phase * 57f, at) {
        drawOval(
            (if (p.pick % 3 == 0) White else Color(0xFFFFC7DA)).copy(alpha = 0.85f),
            Offset(at.x - w / 2, at.y - w / 4), Size(w, w / 2),
        )
    }
}

private fun DrawScope.bat(p: Particle, t: Float) {
    // Across the upper part of the screen, left to right, at different heights and speeds.
    val fx = ((p.x + t * 0.06f * p.speed) % 1.2f) - 0.1f
    val fy = 0.06f + p.y * 0.24f + 0.015f * sin(t * 2f + p.phase)
    val s = (12f + p.size * 10f) * density
    val flap = sin(t * 12f + p.phase) // -1 … 1
    val wingY = s * 0.35f * flap
    val path = Path().apply {
        moveTo(0f, 0f)
        quadraticTo(-s * 0.5f, -s * 0.2f + wingY, -s, wingY)
        quadraticTo(-s * 0.6f, s * 0.05f + wingY * 0.5f, -s * 0.3f, s * 0.25f)
        lineTo(0f, s * 0.15f)
        lineTo(s * 0.3f, s * 0.25f)
        quadraticTo(s * 0.6f, s * 0.05f + wingY * 0.5f, s, wingY)
        quadraticTo(s * 0.5f, -s * 0.2f + wingY, 0f, 0f)
        close()
    }
    translate(fx * size.width, fy * size.height) { drawPath(path, Color(0xFF0B0612)) }
}

private fun DrawScope.moon() {
    val c = Offset(size.width * 0.78f, size.height * 0.13f)
    val r = 38f * density
    drawCircle(Brush.radialGradient(listOf(Color(0x55FF8A1F), Color.Transparent), c, r * 2.4f), r * 2.4f, c)
    drawCircle(Color(0xFFFF9A2E), r, c)
}

private val LEAVES = listOf(Color(0xFFE8A33D), Color(0xFFD9622B), Color(0xFFB8402A), Color(0xFFF2C14E))

/** A maple-ish leaf tumbling down and swaying wide. */
private fun DrawScope.leaf(p: Particle, t: Float) {
    val at = p.drift(this, t, 0.06f, 0.06f)
    val s = (12f + p.size * 10f) * density
    val path = Path().apply {
        moveTo(0f, -s / 2)
        quadraticTo(s * 0.55f, -s * 0.2f, 0f, s / 2)
        quadraticTo(-s * 0.55f, -s * 0.2f, 0f, -s / 2)
        close()
    }
    rotate(sin(t * 1.6f + p.phase) * 50f + p.phase * 57f, at) {
        translate(at.x, at.y) {
            drawPath(path, LEAVES[p.pick % LEAVES.size].copy(alpha = 0.9f))
            drawLine(Color(0x66000000), Offset(0f, -s * 0.35f), Offset(0f, s * 0.5f), strokeWidth = 1f * density)
        }
    }
}

/** Glints on the lake: small four-point stars that fade in and out where they are. */
private fun DrawScope.sparkle(p: Particle, t: Float) {
    val at = Offset(p.x * size.width, (0.55f + p.y * 0.4f) * size.height)
    val a = (0.5f + 0.5f * sin(t * 2.4f * p.speed + p.phase)).let { it * it }
    val r = (4f + p.size * 5f) * density
    val c = White.copy(alpha = a * 0.8f)
    drawLine(c, at - Offset(r, 0f), at + Offset(r, 0f), strokeWidth = 1.5f * density)
    drawLine(c, at - Offset(0f, r), at + Offset(0f, r), strokeWidth = 1.5f * density)
}

/** A warm sun in the top corner with slowly turning rays. */
private fun DrawScope.sun(t: Float) {
    val c = Offset(size.width * 0.8f, size.height * 0.12f)
    val r = 30f * density
    drawCircle(Brush.radialGradient(listOf(Color(0x66FFE08A), Color.Transparent), c, r * 3f), r * 3f, c)
    rotate(t * 8f, c) {
        repeat(12) { k ->
            val a = k / 12f * 2 * PI.toFloat()
            val dir = Offset(cos(a), sin(a))
            drawLine(Color(0xFFFFD86B).copy(alpha = 0.7f), c + dir * (r * 1.3f), c + dir * (r * 1.75f), strokeWidth = 3f * density, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
    drawCircle(Color(0xFFFFD86B), r, c)
}

// ---------- scenes ----------

private val NEW_YEAR_SPARKS = listOf(Color(0xFFFFD23F), Color(0xFFFF5D73), Color(0xFF7CE0FF), Color.White)
private val LAKE_SPARKS = listOf(Color(0xFFFFC94D), Color(0xFFFF6B9E), Color(0xFF9BF6FF))

/** Bursts one after another in the band [top]…[bottom] of the screen, each fading as it falls. */
private fun DrawScope.fireworks(t: Float, colors: List<Color>, top: Float, bottom: Float) {
    val period = 1.6f
    repeat(4) { i ->
        val local = t + i * period / 4
        val cycle = (local / period).toInt()
        val p = (local % period) / period
        val r = Random(cycle * 31 + i)
        val center = Offset(size.width * (0.15f + r.nextFloat() * 0.7f), size.height * (top + r.nextFloat() * (bottom - top)))
        val color = colors[r.nextInt(colors.size)]
        val reach = (60f + r.nextFloat() * 40f) * density
        val ease = 1f - (1f - p) * (1f - p)
        val alpha = (1f - p).coerceIn(0f, 1f)
        val drop = p * p * 24f * density
        repeat(18) { k ->
            val a = k / 18f * 2 * PI.toFloat()
            val tip = center + Offset(cos(a), sin(a)) * reach * ease + Offset(0f, drop)
            val tail = center + Offset(cos(a), sin(a)) * reach * (ease * 0.7f) + Offset(0f, drop * 0.7f)
            drawLine(color.copy(alpha = alpha * 0.6f), tail, tip, strokeWidth = 2f * density)
            drawCircle(color.copy(alpha = alpha), 2.2f * density, tip)
        }
    }
}

private val FLAG = listOf(Color(0xFF000000), Color(0xFFDD0000), Color(0xFFFFCE00))

/** Black-red-gold ribbon waving across the lower middle of the screen. */
private fun DrawScope.flagRibbon(t: Float) {
    val band = 14f * density
    val baseY = size.height * 0.68f
    val steps = 40
    FLAG.forEachIndexed { i, color ->
        val path = Path()
        for (s in 0..steps) {
            val x = size.width * s / steps
            val y = baseY + i * band + sin(x / size.width * 2 * PI.toFloat() * 1.2f - t * 2.2f) * 18f * density
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        // A black stripe would vanish on the dark background: give it a faint edge.
        if (i == 0) drawPath(path, Color.White.copy(alpha = 0.18f), style = Stroke(band + 2f * density))
        drawPath(path, color, style = Stroke(band))
    }
}

/** Paper lanterns on strings from the top edge, swinging gently and glowing. */
private fun DrawScope.lanterns(t: Float) {
    val colors = listOf(Color(0xFFFF9A2E), Color(0xFFFFD23F), Color(0xFFFF6B4A))
    repeat(6) { i ->
        val x = size.width * (0.1f + i * 0.16f)
        val len = size.height * (0.08f + ((i * 37) % 5) * 0.025f)
        val anchor = Offset(x, 0f)
        rotate(sin(t * 1.4f + i) * 6f, anchor) {
            val c = Offset(x, len)
            val r = 16f * density
            drawLine(White.copy(alpha = 0.5f), anchor, c, strokeWidth = 1.2f * density)
            val glow = 0.8f + 0.2f * sin(t * 5f + i * 2)
            drawCircle(Brush.radialGradient(listOf(colors[i % 3].copy(alpha = 0.45f * glow), Color.Transparent), c + Offset(0f, r), r * 3f), r * 3f, c + Offset(0f, r))
            drawOval(colors[i % 3], Offset(c.x - r, c.y), Size(r * 2, r * 2.2f))
            drawOval(Color(0xFFFFF4C2).copy(alpha = glow), Offset(c.x - r * 0.45f, c.y + r * 0.5f), Size(r * 0.9f, r * 1.2f))
        }
    }
}

/** Four candles near the top; the first [lit] burn with a flickering flame. */
private fun DrawScope.adventWreath(t: Float, lit: Int) {
    val w = 14f * density
    val h = 46f * density
    val gap = 34f * density
    val baseY = size.height * 0.24f
    val startX = size.width / 2 - gap * 1.5f
    drawOval(Color(0xFF2E6B45), Offset(startX - gap * 0.9f, baseY - 8f * density), Size(gap * 4.8f, 20f * density))
    repeat(4) { i ->
        val x = startX + i * gap
        drawRoundRect(Color(0xFFC8102E), Offset(x - w / 2, baseY - h), Size(w, h), androidx.compose.ui.geometry.CornerRadius(3f * density))
        if (i < lit) {
            val flicker = 1f + 0.12f * sin(t * 13f + i * 1.7f)
            val fc = Offset(x, baseY - h - 9f * density)
            drawCircle(Brush.radialGradient(listOf(Color(0x66FFD23F), Color.Transparent), fc, 26f * density), 26f * density, fc)
            drawOval(Color(0xFFFFC94D), Offset(fc.x - 4f * density, fc.y - 8f * density * flicker), Size(8f * density, 14f * density * flicker))
        } else {
            drawLine(Color(0xFF3A3A3A), Offset(x, baseY - h), Offset(x, baseY - h - 5f * density), strokeWidth = 1.5f * density)
        }
    }
}

/** A twinkling gold star high in the middle. */
private fun DrawScope.star(t: Float) {
    val c = Offset(size.width / 2, size.height * 0.15f)
    val outer = 24f * density * (1f + 0.06f * sin(t * 3f))
    val path = Path()
    repeat(10) { k ->
        val a = -PI.toFloat() / 2 + k * PI.toFloat() / 5
        val r = if (k % 2 == 0) outer else outer * 0.45f
        val pt = c + Offset(cos(a), sin(a)) * r
        if (k == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    path.close()
    drawCircle(Brush.radialGradient(listOf(Color(0x55FFD23F), Color.Transparent), c, outer * 3), outer * 3, c)
    drawPath(path, Color(0xFFFFD23F))
}
