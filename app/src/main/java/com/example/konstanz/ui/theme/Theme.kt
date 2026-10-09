package com.example.konstanz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalKonstanzColors = staticCompositionLocalOf { LightColors }

// Brand-coloured in both modes, so no dynamic colour.
private fun materialScheme(k: KonstanzColors) = (if (k.isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = k.primary,
    onPrimary = k.onPrimary,
    primaryContainer = k.primaryTint,
    onPrimaryContainer = k.onPrimaryTint,
    secondary = k.ink2,
    onSecondary = k.surface,
    background = k.background,
    onBackground = k.ink,
    surface = k.surface,
    onSurface = k.ink,
    surfaceVariant = k.background,
    onSurfaceVariant = k.ink3,
    surfaceContainerLowest = k.surface,
    surfaceContainerLow = k.surface,
    surfaceContainer = k.surface,
    surfaceContainerHigh = k.surface,
    surfaceContainerHighest = k.background,
    outline = k.line,
    outlineVariant = k.line,
    scrim = Color.Black,
)

private val LightColorScheme = materialScheme(LightColors)
private val DarkColorScheme = materialScheme(DarkColors)

private val KonstanzShapes = Shapes(
    extraSmall = Radius.Badge,
    small = Radius.Small,
    medium = Radius.Card,
    large = Radius.Sheet,
    extraLarge = Radius.Hero,
)

/** Follows the phone's light/dark setting unless [darkTheme] says otherwise. */
@Composable
fun KonstanzTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    // The top-level tokens (Ink, Surface …) read this; set before the content composes so the first frame is right.
    if (ActivePalette.colors != colors) ActivePalette.colors = colors
    CompositionLocalProvider(LocalKonstanzColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
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
