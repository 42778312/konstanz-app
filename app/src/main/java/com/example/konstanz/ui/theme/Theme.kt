package com.example.konstanz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Every design token, including the ones Material has no slot for (status, map). */
@Immutable
data class KonstanzColors(
    val primary: Color = Primary,
    val primaryPressed: Color = PrimaryPressed,
    val primaryTint: Color = PrimaryTint,
    val onPrimaryTint: Color = OnPrimaryTint,
    val surface: Color = White,
    val background: Color = Background,
    val ink: Color = Ink,
    val ink2: Color = Ink2,
    val ink3: Color = Ink3,
    val ink4: Color = Ink4,
    val line: Color = Line,
    val dotInactive: Color = DotInactive,
    val switchTrackOff: Color = SwitchTrackOff,
    val skeletonBase: Color = SkeletonBase,
    val skeletonHighlight: Color = SkeletonHighlight,
    val skeletonBar: Color = SkeletonBar,
    val location: Color = Location,
    val live: Color = Live,
    val liveTint: Color = LiveTint,
    val delayed: Color = Delayed,
    val delayedTint: Color = DelayedTint,
    val detour: Color = Detour,
    val detourTint: Color = DetourTint,
    val cancelled: Color = Cancelled,
    val mapLand: Color = MapLand,
    val mapWater: Color = MapWater,
    val mapPark: Color = MapPark,
    val mapBuilding: Color = MapBuilding,
    val mapRoad: Color = MapRoad,
    val mapRoadCasing: Color = MapRoadCasing,
)

private val LocalKonstanzColors = staticCompositionLocalOf { KonstanzColors() }

// The design is light-only and brand-coloured, so no dark scheme and no dynamic colour.
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = PrimaryTint,
    onPrimaryContainer = OnPrimaryTint,
    secondary = Ink2,
    onSecondary = White,
    background = Background,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Background,
    onSurfaceVariant = Ink3,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Background,
    outline = Line,
    outlineVariant = Line,
    scrim = Ink,
)

private val KonstanzShapes = Shapes(
    extraSmall = Radius.Badge,
    small = Radius.Small,
    medium = Radius.Card,
    large = Radius.Sheet,
    extraLarge = Radius.Hero,
)

@Composable
fun KonstanzTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKonstanzColors provides KonstanzColors()) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            shapes = KonstanzShapes,
            content = content
        )
    }
}

/** Access design tokens inside composables: `KonstanzTheme.colors.live`, `KonstanzTheme.type.Time`. */
object KonstanzTheme {
    val colors: KonstanzColors
        @Composable @ReadOnlyComposable get() = LocalKonstanzColors.current
    val type = KonstanzType
    val radius = Radius
    val spacing = Spacing
}
