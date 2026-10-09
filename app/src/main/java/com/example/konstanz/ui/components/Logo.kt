package com.example.konstanz.ui.components

import androidx.compose.foundation.Canvas
import com.example.konstanz.ui.theme.BrandDeep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.example.konstanz.ui.theme.KonstanzType
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White

/**
 * The Konstant mark: a K drawn as a trip — start ring (bottom), the steady line (stem), the curve
 * into the destination diamond. Drawn on the brand's 100×100 grid (branding/konstant-mark.svg).
 * [background] fills the ring so it stays open on any surface.
 */
@Composable
fun KonstanzMark(modifier: Modifier = Modifier, color: Color = BrandDeep, background: Color = White) {
    Canvas(modifier.size(56.dp)) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
            translate(left = -4f) {
                drawLine(color, Offset(32f, 20f), Offset(32f, 80f), strokeWidth = 12f, cap = StrokeCap.Round)
                val curve = Path().apply {
                    moveTo(32f, 62f)
                    cubicTo(32f, 46f, 70f, 52f, 70f, 35f)
                }
                drawPath(curve, color, style = Stroke(width = 10f, cap = StrokeCap.Round))
                drawLine(color, Offset(45f, 53f), Offset(63f, 71f), strokeWidth = 10f, cap = StrokeCap.Round)
                drawCircle(background, radius = 9f, center = Offset(70f, 78f))
                drawCircle(color, radius = 9f, center = Offset(70f, 78f), style = Stroke(width = 6.5f))
                rotate(45f, pivot = Offset(70f, 24f)) {
                    drawRoundRect(color, topLeft = Offset(62f, 16f), size = Size(16f, 16f), cornerRadius = CornerRadius(3f))
                }
            }
        }
    }
}

/** "konstant" in the display face, its o drawn as a stop ring (brand book → Wordmark). */
@Composable
fun KonstantWordmark(fontSize: TextUnit, color: Color, modifier: Modifier = Modifier) {
    val style = KonstanzType.Display.copy(fontSize = fontSize, letterSpacing = (-0.035).em, color = color)
    val density = LocalDensity.current
    val ring = with(density) { (fontSize * 0.55f).toDp() }
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "Konstant" }, verticalAlignment = Alignment.Bottom) {
        Text("k", style = style)
        Box(
            Modifier
                // Sits on the baseline like the x-height letters around it.
                .padding(start = ring * 0.05f, end = ring * 0.05f, bottom = with(density) { (fontSize * 0.2f).toDp() })
                .size(ring)
                .border(ring * 0.2f, color, CircleShape),
        )
        Text("nstant", style = style)
    }
}

/** White app tile with the mark — Splash uses 104 dp, About 80 dp. Radius scales with size (31/104). */
@Composable
fun KonstanzLogoTile(modifier: Modifier = Modifier, size: Dp = 104.dp) {
    val shape = RoundedCornerShape(size * (31f / 104f))
    Box(
        modifier
            .size(size)
            .dropShadow(shape, Shadow(radius = 30.dp, offset = DpOffset(0.dp, 10.dp), color = Color(0xFF0B3B22), alpha = 0.25f))
            .background(White, shape),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzMark(Modifier.size(size * (70f / 104f)))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFCCF9D3)
@Composable
private fun LogoPreview() {
    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) { KonstanzLogoTile() }
}
