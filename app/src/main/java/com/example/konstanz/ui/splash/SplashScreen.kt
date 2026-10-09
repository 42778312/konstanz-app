package com.example.konstanz.ui.splash

import com.example.konstanz.data.Texts
import com.example.konstanz.ui.theme.OnBrand
import com.example.konstanz.ui.theme.Brand
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.konstanz.data.SeasonalCalendar
import com.example.konstanz.data.SeasonalEvent
import com.example.konstanz.data.SeasonalMoment
import com.example.konstanz.data.SeasonalThemes
import java.time.LocalDate
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
import com.example.konstanz.ui.components.KonstantWordmark
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
    seasonalThemes: suspend () -> SeasonalThemes = { SeasonalThemes.Off },
) {
    val finish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    var moment by remember { mutableStateOf<SeasonalMoment?>(null) }
    LaunchedEffect(Unit) {
        val done = async { isOnboardingCompleted() }
        // A quick DataStore read; the splash starts in brand red and fades to the event's look.
        moment = when (seasonalThemes()) {
            SeasonalThemes.EventsAndSeasons -> SeasonalCalendar.momentOn(LocalDate.now(), includeSeasons = true)
            SeasonalThemes.EventsOnly -> SeasonalCalendar.momentOn(LocalDate.now(), includeSeasons = false)
            SeasonalThemes.Off -> null
        }
        // Events (and their countdown week) stay a little longer so the effect can be seen; seasons don't.
        val duration = if (moment?.event?.isSeason == false) 1800 else 1200
        progress.animateTo(1f, tween(durationMillis = duration, easing = FastOutSlowInEasing))
        finish(done.await())
    }
    SplashScreen(progress = progress.value, moment = moment)
}

@Composable
fun SplashScreen(progress: Float, modifier: Modifier = Modifier, moment: SeasonalMoment? = null) {
    val event = moment?.event
    val look = event?.look
    val background by animateColorAsState(look?.background ?: Brand, tween(400), label = "splash-background")
    val content = look?.content ?: OnBrand
    // Mint is light: dark status-bar icons unless the event's look is dark.
    SystemBarIcons(darkIcons = look?.darkIcons ?: true)
    BoxWithConstraints(modifier.fillMaxSize().background(background)) {
        RouteDecoration(Modifier.fillMaxSize(), content)
        AnimatedVisibility(event != null, enter = fadeIn(tween(400)), exit = fadeOut()) {
            // Keeps the last event while fading out.
            val shown = remember { moment }
            if (shown != null) {
                // Unlit while counting down to Advent.
                val candles = remember { if (shown.daysToGo > 0) 0 else SeasonalCalendar.adventCandles(LocalDate.now()).coerceAtLeast(1) }
                SeasonalEffect(shown.event, Modifier.fillMaxSize(), candles = candles)
            }
        }

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
                KonstantWordmark(fontSize = 46.sp, color = content)
                Text(
                    when {
                        moment == null -> stringResource(R.string.splash_tagline)
                        moment.daysToGo == 0 -> stringResource(moment.event.greeting)
                        else -> pluralStringResource(R.plurals.event_countdown, moment.daysToGo, stringResource(moment.event.title), moment.daysToGo)
                    },
                    style = KonstanzType.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 1.35.em),
                    color = content.copy(alpha = 0.92f),
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
            LoadingBar(progress, content)
            Text(
                stringResource(R.string.splash_loading),
                style = KonstanzType.Caption.copy(fontSize = 14.sp),
                color = content.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadingBar(progress: Float, color: Color) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.25f))
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
    ) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(color, shape))
    }
}

// The three faint route lines from the design, on its 390×844 grid.
private val decorationPaths = listOf(
    "M-20 640 C 80 560 160 700 250 600 S 380 470 420 520",
    "M-20 700 C 90 640 180 760 280 660 S 390 560 430 600",
    "M-20 200 C 60 260 180 120 260 190 S 380 300 420 240",
).map { PathParser().parsePathString(it).toPath() }

@Composable
private fun RouteDecoration(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val color = tint.copy(alpha = 0.14f)
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

private class SeasonalEventProvider : PreviewParameterProvider<SeasonalEvent> {
    override val values = SeasonalEvent.entries.asSequence()
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SeasonalSplashPreview(@PreviewParameter(SeasonalEventProvider::class) event: SeasonalEvent) {
    KonstanzTheme { SplashScreen(progress = 0.62f, moment = SeasonalMoment(event)) }
}
