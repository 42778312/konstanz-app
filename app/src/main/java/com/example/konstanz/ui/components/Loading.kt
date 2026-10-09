package com.example.konstanz.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.theme.Location
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.SkeletonBase
import com.example.konstanz.ui.theme.SkeletonHighlight

/** Skeleton placeholder with a moving highlight (design: kt-shimmer, 1.4 s). Give it a size. */
@Composable
fun Shimmer(modifier: Modifier = Modifier, shape: Shape = Radius.Pill) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer-x",
    )
    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(SkeletonBase, SkeletonHighlight, SkeletonBase),
                start = Offset(x, 0f),
                end = Offset(x + 600f, 0f),
            ),
            shape,
        )
    )
}

/** Small ring spinner (design: kt-spin, light-blue track, blue arc). */
@Composable
fun SmallSpinner(modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "spin-angle",
    )
    Canvas(modifier.size(size)) {
        val stroke = 2.dp.toPx()
        drawCircle(Location.copy(alpha = 0.12f), radius = this.size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
        drawArc(
            Location, startAngle = angle - 90f, sweepAngle = 90f, useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}
