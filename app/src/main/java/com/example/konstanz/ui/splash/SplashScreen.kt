package com.example.konstanz.ui.splash

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.components.KonstanzLogoTile
import com.example.konstanz.ui.components.SystemBarIcons
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White
import kotlinx.coroutines.async

/**
 * 01 Splash. Shows the loading bar while start-up work runs, then calls [onFinished] with
 * whether onboarding was already completed. The bar runs for at least 1.2 s so the brand
 * moment never flashes; real data loading (Part D) will drive it later.
 */
@Composable
fun SplashRoute(
    isOnboardingCompleted: suspend () -> Boolean,
    onFinished: (onboardingDone: Boolean) -> Unit,
) {
    val finish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val done = async { isOnboardingCompleted() }
        progress.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
        finish(done.await())
    }
    SplashScreen(progress = progress.value)
}

@Composable
fun SplashScreen(progress: Float, modifier: Modifier = Modifier) {
    SystemBarIcons(darkIcons = false)
    BoxWithConstraints(modifier.fillMaxSize().background(Primary)) {
        RouteDecoration(Modifier.fillMaxSize())

        // Design: content block starts at 270 of 844 px.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = maxHeight * (270f / 844f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            KonstanzLogoTile()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Konstanz Transit", style = KonstanzType.Display.copy(fontSize = 38.sp), color = White)
                Text(
                    stringResource(R.string.splash_tagline),
                    style = KonstanzType.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 1.35.em),
                    color = White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 60.dp, end = 60.dp, bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoadingBar(progress)
            Text(
                stringResource(R.string.splash_loading),
                style = KonstanzType.Caption.copy(fontSize = 14.sp),
                color = White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadingBar(progress: Float) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(White.copy(alpha = 0.25f))
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
    ) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(White, shape))
    }
}

// The three faint route lines from the design, on its 390×844 grid.
private val decorationPaths = listOf(
    "M-20 640 C 80 560 160 700 250 600 S 380 470 420 520",
    "M-20 700 C 90 640 180 760 280 660 S 390 560 430 600",
    "M-20 200 C 60 260 180 120 260 190 S 380 300 420 240",
).map { PathParser().parsePathString(it).toPath() }

@Composable
private fun RouteDecoration(modifier: Modifier) {
    Canvas(modifier) {
        val color = Color.White.copy(alpha = 0.14f)
        scale(size.width / 390f, size.height / 844f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(decorationPaths[0], color, style = Stroke(width = 3f))
            drawPath(
                decorationPaths[1], color,
                style = Stroke(width = 3f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 10f))),
            )
            drawPath(decorationPaths[2], color, style = Stroke(width = 3f))
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SplashPreview() {
    KonstanzTheme { SplashScreen(progress = 0.62f) }
}
