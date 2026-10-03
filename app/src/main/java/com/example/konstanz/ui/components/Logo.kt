package com.example.konstanz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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

/** The route mark: start ring, curved route, destination diamond. Drawn on a 56×56 grid. */
@Composable
fun KonstanzMark(modifier: Modifier = Modifier, color: Color = Primary, background: Color = White) {
    Canvas(modifier.size(56.dp)) {
        scale(size.width / 56f, size.height / 56f, pivot = Offset.Zero) {
            val route = Path().apply {
                moveTo(14f, 42f)
                cubicTo(14f, 30f, 42f, 34f, 42f, 20f)
            }
            drawPath(route, color, style = Stroke(width = 6f, cap = StrokeCap.Round))
            drawCircle(background, radius = 7f, center = Offset(14f, 42f))
            drawCircle(color, radius = 7f, center = Offset(14f, 42f), style = Stroke(width = 4.5f))
            rotate(45f, pivot = Offset(42f, 14f)) {
                drawRoundRect(color, topLeft = Offset(35f, 7f), size = Size(14f, 14f), cornerRadius = CornerRadius(2.5f))
            }
        }
    }
}

/** White app tile with the mark — Splash uses 104 dp, About 80 dp. Radius scales with size (31/104). */
@Composable
fun KonstanzLogoTile(modifier: Modifier = Modifier, size: Dp = 104.dp) {
    val shape = RoundedCornerShape(size * (31f / 104f))
    Box(
        modifier
            .size(size)
            .dropShadow(shape, Shadow(radius = 30.dp, offset = DpOffset(0.dp, 10.dp), color = Color(0xFF500014), alpha = 0.25f))
            .background(White, shape),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzMark(Modifier.size(size * (56f / 104f)))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFD4003B)
@Composable
private fun LogoPreview() {
    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) { KonstanzLogoTile() }
}
