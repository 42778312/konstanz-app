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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tokens from the "00 Design system" artboard. Names follow the design, not Material.

// Brand
val Primary = Color(0xFFD4003B)          // actions, transit
val PrimaryPressed = Color(0xFFA8002F)   // hover / pressed
val PrimaryTint = Color(0xFFFCE8EE)      // tonal surfaces
val OnPrimaryTint = Color(0xFFB00032)    // text/icons on PrimaryTint
val White = Color(0xFFFFFFFF)            // surfaces, sheets

// Neutrals
val Ink = Color(0xFF16181D)              // text
val Ink2 = Color(0xFF474C55)             // secondary text
val Ink3 = Color(0xFF646973)             // captions (≥ 4.5:1 on white)
val Ink4 = Color(0xFFA3A7AF)             // chevrons, decorative icons only
val Line = Color(0xFFE7E8EB)             // dividers, outlines
val Background = Color(0xFFF4F5F7)       // grouped surfaces
val DotInactive = Color(0xFFD9DBE0)      // pager dots
val SwitchTrackOff = Color(0xFFC9CCD2)
val SkeletonBase = Color(0xFFEDEEF1)
val SkeletonHighlight = Color(0xFFF7F8FA)
val SkeletonBar = Color(0xFFD3D5DA)

// Accent & status — always paired with an icon and a word, never colour alone
val Location = Color(0xFF1F6FEB)         // GPS, syncing
val Live = Color(0xFF0A7A43)             // realtime on time — the only green times
val LiveTint = Color(0xFFE3F4EA)
val Delayed = Color(0xFF9A4A00)          // delays, stale data, warnings
val DelayedTint = Color(0xFFFFF0DC)
val Detour = Color(0xFF6B2FB3)           // service changes
val DetourTint = Color(0xFFF1EAFB)
val Cancelled = Ink                      // inverse chip + struck-through time

// Map
val MapLand = Color(0xFFF1EEE9)
val MapWater = Color(0xFFC9DFEC)         // Bodensee, Rhein
val MapPark = Color(0xFFDDE9D3)
val MapBuilding = Color(0xFFE5E0D8)      // Altstadt blocks
val MapRoad = White
val MapRoadCasing = Color(0xFFE2DDD4)
val MapDistrictLabel = Color(0xFF8A8E96) // ALTSTADT, PETERSHAUSEN …
val MapWaterLabel = Color(0xFF5C88A3)    // Bodensee, Konstanzer Trichter

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ColorsPreview() {
    val groups = listOf(
        "Brand" to listOf("Primary" to Primary, "Pressed" to PrimaryPressed, "Tint" to PrimaryTint, "OnTint" to OnPrimaryTint, "White" to White),
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
