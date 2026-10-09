package com.example.konstanz.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tokens from the "00 Design system" artboard. Names follow the design, not Material.
// Every token but [White] follows the phone's light/dark setting: they read [ActivePalette], which
// KonstanzTheme switches. Reads are snapshot state, so composables and draw lambdas update on a switch.

/** The palette in use; set by [KonstanzTheme] from the system dark mode. */
internal object ActivePalette {
    var colors by mutableStateOf(LightColors)
}

private val c get() = ActivePalette.colors

/** Always white: text and icons on brand, status and [Inverse] fills, marker rings. Not a surface — use [Surface]. */
val White = Color(0xFFFFFFFF)

/** True in the dark (OLED black) palette. */
val IsDark get() = c.isDark

/** The logo's deep mint: the mark on its white tile, the same in light and dark. */
val BrandDeep = Color(0xFF17784A)

// Brand
val Brand get() = c.brand                     // mint brand fill: main buttons, line badges, splash
val BrandPressed get() = c.brandPressed
val OnBrand get() = c.onBrand                 // text/icons on Brand
val Primary get() = c.primary                 // brand colour that reads on surfaces: lines, icons, markers
val OnPrimary get() = c.onPrimary             // text/icons on Primary fills
val PrimaryPressed get() = c.primaryPressed   // hover / pressed
val PrimaryTint get() = c.primaryTint         // tonal surfaces
val OnPrimaryTint get() = c.onPrimaryTint     // text/icons on PrimaryTint
val Surface get() = c.surface                 // screens, sheets, cards
val Inverse get() = c.inverse                 // dark fills under white content (offline card, hints)

// Neutrals
val Ink get() = c.ink                         // text
val Ink2 get() = c.ink2                       // secondary text
val Ink3 get() = c.ink3                       // captions (≥ 4.5:1 on Surface)
val Ink4 get() = c.ink4                       // chevrons, decorative icons only
val Line get() = c.line                       // dividers, outlines
val Background get() = c.background           // grouped surfaces
val DotInactive get() = c.dotInactive         // pager dots
val SwitchTrackOff get() = c.switchTrackOff
val SkeletonBase get() = c.skeletonBase
val SkeletonHighlight get() = c.skeletonHighlight
val SkeletonBar get() = c.skeletonBar

// Accent & status — always paired with an icon and a word, never colour alone
val Location get() = c.location               // GPS, syncing
val Live get() = c.live                       // realtime on time — the only green times
val LiveTint get() = c.liveTint
val Delayed get() = c.delayed                 // delays, stale data, warnings
val DelayedTint get() = c.delayedTint
val Detour get() = c.detour                   // service changes
val DetourTint get() = c.detourTint
val Cancelled get() = c.inverse               // inverse chip + struck-through time

// Map
val MapLand get() = c.mapLand
val MapWater get() = c.mapWater               // Bodensee, Rhein
val MapPark get() = c.mapPark
val MapBuilding get() = c.mapBuilding         // Altstadt blocks
val MapRoad get() = c.mapRoad
val MapRoadCasing get() = c.mapRoadCasing
val MapDistrictLabel get() = c.mapDistrictLabel // ALTSTADT, PETERSHAUSEN …
val MapWaterLabel get() = c.mapWaterLabel       // Bodensee, Konstanzer Trichter

/** Every design token, including the ones Material has no slot for (status, map). */
@Immutable
data class KonstanzColors(
    val isDark: Boolean,
    val brand: Color,
    val brandPressed: Color,
    val onBrand: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryPressed: Color,
    val primaryTint: Color,
    val onPrimaryTint: Color,
    val surface: Color,
    val inverse: Color,
    val background: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val ink4: Color,
    val line: Color,
    val dotInactive: Color,
    val switchTrackOff: Color,
    val skeletonBase: Color,
    val skeletonHighlight: Color,
    val skeletonBar: Color,
    val location: Color,
    val live: Color,
    val liveTint: Color,
    val delayed: Color,
    val delayedTint: Color,
    val detour: Color,
    val detourTint: Color,
    val mapLand: Color,
    val mapWater: Color,
    val mapPark: Color,
    val mapBuilding: Color,
    val mapRoad: Color,
    val mapRoadCasing: Color,
    val mapDistrictLabel: Color,
    val mapWaterLabel: Color,
) {
    val cancelled: Color get() = inverse
}

val LightColors = KonstanzColors(
    isDark = false,
    // Mint #CCF9D3 is the brand. Too light for lines and icons on white, so those use its deep green (5.5:1).
    brand = Color(0xFFCCF9D3),
    brandPressed = Color(0xFFB2EEBD),
    onBrand = Color(0xFF0B3B22),
    primary = Color(0xFF17784A),
    onPrimary = Color(0xFFFFFFFF),
    primaryPressed = Color(0xFF11613B),
    primaryTint = Color(0xFFE6F8EA),
    onPrimaryTint = Color(0xFF17784A),
    surface = Color(0xFFFFFFFF),
    inverse = Color(0xFF16181D),
    background = Color(0xFFF4F5F7),
    ink = Color(0xFF16181D),
    ink2 = Color(0xFF474C55),
    ink3 = Color(0xFF646973),
    ink4 = Color(0xFFA3A7AF),
    line = Color(0xFFE7E8EB),
    dotInactive = Color(0xFFD9DBE0),
    switchTrackOff = Color(0xFFC9CCD2),
    skeletonBase = Color(0xFFEDEEF1),
    skeletonHighlight = Color(0xFFF7F8FA),
    skeletonBar = Color(0xFFD3D5DA),
    location = Color(0xFF1F6FEB),
    live = Color(0xFF0A7A43),
    liveTint = Color(0xFFE3F4EA),
    delayed = Color(0xFF9A4A00),
    delayedTint = Color(0xFFFFF0DC),
    detour = Color(0xFF6B2FB3),
    detourTint = Color(0xFFF1EAFB),
    mapLand = Color(0xFFF1EEE9),
    mapWater = Color(0xFFC9DFEC),
    mapPark = Color(0xFFDDE9D3),
    mapBuilding = Color(0xFFE5E0D8),
    mapRoad = Color(0xFFFFFFFF),
    mapRoadCasing = Color(0xFFE2DDD4),
    mapDistrictLabel = Color(0xFF8A8E96),
    mapWaterLabel = Color(0xFF5C88A3),
)

// Dark: true black for OLED screens (pixels off), near-black cards and sheets just above it.
// Same roles, contrast kept (Ink3 ≥ 4.5:1 on Surface). Mint reads on black, so it is Primary too.
val DarkColors = KonstanzColors(
    isDark = true,
    brand = Color(0xFFCCF9D3),
    brandPressed = Color(0xFFB2EEBD),
    onBrand = Color(0xFF0B3B22),
    primary = Color(0xFFCCF9D3),
    onPrimary = Color(0xFF0B3B22),
    primaryPressed = Color(0xFFB2EEBD),
    primaryTint = Color(0xFF0F2418),
    onPrimaryTint = Color(0xFFCCF9D3),
    surface = Color(0xFF0E0F11),
    inverse = Color(0xFF2A2C31),
    background = Color(0xFF000000),
    ink = Color(0xFFECEDEF),
    ink2 = Color(0xFFC2C5CC),
    ink3 = Color(0xFF9DA2AB),
    ink4 = Color(0xFF6B7079),
    line = Color(0xFF1F2125),
    dotInactive = Color(0xFF2E3035),
    switchTrackOff = Color(0xFF3A3D44),
    skeletonBase = Color(0xFF17181B),
    skeletonHighlight = Color(0xFF212327),
    skeletonBar = Color(0xFF2E3035),
    location = Color(0xFF5B9BFF),
    live = Color(0xFF3DC27F),
    liveTint = Color(0xFF0E2417),
    delayed = Color(0xFFF2A54E),
    delayedTint = Color(0xFF2A1D0E),
    detour = Color(0xFFB98DF2),
    detourTint = Color(0xFF1E1630),
    mapLand = Color(0xFF050505),
    mapWater = Color(0xFF0A1724),
    mapPark = Color(0xFF0B160F),
    mapBuilding = Color(0xFF141518),
    mapRoad = Color(0xFF2A2C31),
    mapRoadCasing = Color(0xFF141518),
    mapDistrictLabel = Color(0xFF8A8E96),
    mapWaterLabel = Color(0xFF7FA9C6),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ColorsPreview() {
    val groups = listOf(
        "Brand" to listOf("Brand" to Brand, "Primary" to Primary, "Pressed" to PrimaryPressed, "Tint" to PrimaryTint, "OnTint" to OnPrimaryTint, "Surface" to Surface),
        "Neutrals" to listOf("Ink" to Ink, "Ink2" to Ink2, "Ink3" to Ink3, "Ink4" to Ink4, "Line" to Line, "Bg" to Background),
        "Status" to listOf("Location" to Location, "Live" to Live, "LiveTint" to LiveTint, "Delayed" to Delayed, "DelayTint" to DelayedTint, "Detour" to Detour),
        "Map" to listOf("Land" to MapLand, "Water" to MapWater, "Park" to MapPark, "Building" to MapBuilding, "Casing" to MapRoadCasing),
    )
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        groups.forEach { (title, swatches) ->
            Text(title.uppercase(), fontFamily = Figtree, fontSize = 13.sp, color = Ink3)
            swatches.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (name, color) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(width = 110.dp, height = 44.dp)
                                    .background(color, RoundedCornerShape(12.dp))
                                    .border(1.dp, Line, RoundedCornerShape(12.dp))
                            )
                            Text(name, fontFamily = Figtree, fontSize = 12.sp, color = Ink2)
                        }
                    }
                }
            }
        }
    }
}
