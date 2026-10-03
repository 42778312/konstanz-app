package com.example.konstanz.ui.icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType

/**
 * A stroke icon from the design: 24×24 viewport, round caps and joins, no fill.
 * Geometry only — stroke width and colour are chosen where it is drawn, like the design does
 * (2 in the icon grid, 2.2 in rows, 2.4 in small chips).
 */
@Immutable
class KtIcon(val name: String, vararg val paths: String) {
    fun toImageVector(strokeWidth: Float, filled: Boolean = false): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach {
                    addPath(
                        pathData = addPathNodes(it),
                        fill = if (filled) SolidColor(Color.Black) else null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = strokeWidth,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()
}

/** Draws a [KtIcon]. Pass a [contentDescription] only when the icon stands alone (icon buttons). */
@Composable
fun KonstanzIcon(
    icon: KtIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    strokeWidth: Float = 2.2f,
    /** Also fill the shape, like the design's solid navigation arrow on "Go" buttons. */
    filled: Boolean = false,
) {
    val vector = remember(icon, strokeWidth, filled) { icon.toImageVector(strokeWidth, filled) }
    Icon(vector, contentDescription, modifier.size(size), tint = tint)
}

/** The icon set from the "00 Design system" artboard, plus the extra glyphs the screens use. */
object KtIcons {
    val Search = KtIcon("search",
        "M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0z",
        "M20 20l-3.5-3.5",
    )
    val Bus = KtIcon("bus",
        "M8 3h8a3 3 0 0 1 3 3v8a3 3 0 0 1 -3 3h-8a3 3 0 0 1 -3 -3v-8a3 3 0 0 1 3 -3z",
        "M5 10h14",
        "M8 17v3M16 17v3",
        "M8.5 13.5h.01M15.5 13.5h.01",
    )
    val Walk = KtIcon("walk",
        "M11.7 4.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0z",
        "M10 21l2-5.5 2.5 2.5V21",
        "M7.5 12l2.2-3.6a2 2 0 0 1 2.3-.9l1.5.5a2 2 0 0 1 1.2 1.2l.8 2.3 2.5 1",
        "M12 15.5l-.8-4.5",
    )
    val Pin = KtIcon("pin",
        "M12 21s-7-6.1-7-11.3A7 7 0 0 1 19 9.7C19 14.9 12 21 12 21z",
        "M9.5 9.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0z",
    )
    val Nav = KtIcon("nav",
        "M20 4L4 11l7 2 2 7z",
    )
    val Locate = KtIcon("locate",
        "M5 12a7 7 0 1 0 14 0a7 7 0 1 0 -14 0z",
        "M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0z",
        "M12 2v3M12 19v3M2 12h3M19 12h3",
    )
    val Swap = KtIcon("swap",
        "M8 4v16M8 4L4 8M8 4l4 4",
        "M16 20V4M16 20l-4-4M16 20l4-4",
    )
    val Clock = KtIcon("clock",
        "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0z",
        "M12 7.5V12l3 2",
    )
    val History = KtIcon("history",
        "M3.5 12a8.5 8.5 0 1 0 2.5-6",
        "M3 4v4h4",
        "M12 8v4l3 2",
    )
    val Star = KtIcon("star",
        "M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8L3.5 9.7l5.9-.9z",
    )
    val Home = KtIcon("home",
        "M4 11l8-7 8 7",
        "M6 9.5V20h12V9.5",
        "M10 20v-5h4v5",
    )
    val Cap = KtIcon("cap",
        "M2.5 9L12 4.5 21.5 9 12 13.5z",
        "M6.5 11v5c0 1.5 2.5 3 5.5 3s5.5-1.5 5.5-3v-5",
        "M21.5 9v5",
    )
    val Brief = KtIcon("brief",
        "M6 7h12a2.5 2.5 0 0 1 2.5 2.5v7.5a2.5 2.5 0 0 1 -2.5 2.5h-12a2.5 2.5 0 0 1 -2.5 -2.5v-7.5a2.5 2.5 0 0 1 2.5 -2.5z",
        "M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7",
        "M3.5 12.5h17",
    )
    val Layers = KtIcon("layers",
        "M12 4l9 5-9 5-9-5z",
        "M3 14l9 5 9-5",
    )
    val CloudOff = KtIcon("cloud_off",
        "M3 3l18 18",
        "M8.5 8.4A5 5 0 0 0 7 18h10.5",
        "M20.3 16.5A4 4 0 0 0 17 10h-.4A6 6 0 0 0 11 5.5c-.8 0-1.6.2-2.3.5",
    )
    val WifiOff = KtIcon("wifi_off",
        "M3 3l18 18",
        "M8.5 16.5a5 5 0 0 1 7 0",
        "M5 12.9a10 10 0 0 1 5.2-2.7M14 10.3a10 10 0 0 1 5 2.6",
        "M2 9a14.5 14.5 0 0 1 4.5-2.8M10.5 5.1A14.5 14.5 0 0 1 22 9",
        "M12 20h.01",
    )
    val Refresh = KtIcon("refresh",
        "M20 12a8 8 0 1 1-2.4-5.7",
        "M20 4v5h-5",
    )
    val Alert = KtIcon("alert",
        "M12 4l9 16H3z",
        "M12 10v4M12 17h.01",
    )
    val Live = KtIcon("live",
        "M10.2 12a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0z",
        "M8.3 8.3a5.3 5.3 0 0 0 0 7.4M15.7 8.3a5.3 5.3 0 0 1 0 7.4",
        "M5.4 5.4a9.3 9.3 0 0 0 0 13.2M18.6 5.4a9.3 9.3 0 0 1 0 13.2",
    )
    val LiveOff = KtIcon("live_off",
        "M3 3l18 18",
        "M8.3 15.7a5.3 5.3 0 0 1-.6-6.6M15.7 8.3a5.3 5.3 0 0 1 1.2 5.6",
        "M5.4 18.6a9.3 9.3 0 0 1-1.5-11M18.6 5.4a9.3 9.3 0 0 1 2.2 9.8",
    )
    val Calendar = KtIcon("calendar",
        "M6.5 5.5h11a2.5 2.5 0 0 1 2.5 2.5v9.5a2.5 2.5 0 0 1 -2.5 2.5h-11a2.5 2.5 0 0 1 -2.5 -2.5v-9.5a2.5 2.5 0 0 1 2.5 -2.5z",
        "M4 10h16M8.5 3.5v4M15.5 3.5v4",
    )
    val Detour = KtIcon("detour",
        "M6 20v-6a4 4 0 0 1 4-4h8",
        "M15 7l3 3-3 3",
        "M6 4v6",
    )
    val Cancel = KtIcon("cancel",
        "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0z",
        "M6 6l12 12",
    )
    val Access = KtIcon("access",
        "M10.2 4.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0z",
        "M5 8.5l7 1.5 7-1.5",
        "M12 10v4.5l-3 6M12 14.5l3 6",
    )
    val TurnLeft = KtIcon("turn_left",
        "M16 20v-7a3 3 0 0 0-3-3H6",
        "M10 6l-4 4 4 4",
    )
    val TurnRight = KtIcon("turn_right",
        "M8 20v-7a3 3 0 0 1 3-3h7",
        "M14 6l4 4-4 4",
    )
    val Straight = KtIcon("straight",
        "M12 20V5",
        "M7 10l5-5 5 5",
    )
    val SlightLeft = KtIcon("slight_left",
        "M15 20v-6a3 3 0 0 0-.9-2.1L8.5 6.5",
        "M8 11.5V6h5.5",
    )
    val SlightRight = KtIcon("slight_right",
        "M9 20v-6a3 3 0 0 1 .9-2.1l5.6-5.4",
        "M16 11.5V6h-5.5",
    )
    val SharpLeft = KtIcon("sharp_left",
        "M16 20V7.5a1.5 1.5 0 0 0-2.6-1L7 13.5",
        "M7 8v5.5h5.5",
    )
    val SharpRight = KtIcon("sharp_right",
        "M8 20V7.5a1.5 1.5 0 0 1 2.6-1L17 13.5",
        "M17 8v5.5h-5.5",
    )
    val UTurn = KtIcon("u_turn",
        "M16 20V9a4 4 0 0 0-8 0v7",
        "M4.5 12.5L8 16l3.5-3.5",
    )
    val Stairs = KtIcon("stairs",
        "M4 20h4v-4h4v-4h4V8h4",
    )
    val Crossing = KtIcon("crossing",
        "M7 5v14", "M12 5v14", "M17 5v14",
    )
    val Food = KtIcon("food",
        "M7 3v8a2 2 0 0 0 2 2v8", "M5 3v6a2 2 0 0 0 4 0V3",
        "M17 21V3c-2 1.5-3 4-3 7v3h3",
    )
    val Coffee = KtIcon("coffee",
        "M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5V9z",
        "M16 11h1.5a2.5 2.5 0 0 1 0 5H16", "M8 3.5v2.5", "M11.5 3.5v2.5",
    )
    val Bag = KtIcon("bag",
        "M5.5 8h13l-1 12.5H6.5L5.5 8z", "M9 10V7a3 3 0 0 1 6 0v3",
    )
    val Health = KtIcon("health",
        "M10 4h4v6h6v4h-6v6h-4v-6H4v-4h6V4z",
    )
    val Tree = KtIcon("tree",
        "M12 21v-5", "M12 3l6 8h-3l4 5H5l4-5H6l6-8z",
    )
    val Bed = KtIcon("bed",
        "M3 19V6", "M3 15h18v4", "M21 15v-3a3 3 0 0 0-3-3h-7v6",
        "M5.5 10.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z",
    )
    val Landmark = KtIcon("landmark",
        "M3 20h18", "M4 9h16", "M12 3l8 6H4l8-6z", "M6 9v9", "M10 9v9", "M14 9v9", "M18 9v9",
    )
    val Road = KtIcon("road",
        "M8 3L5 21", "M16 3l3 18", "M12 4v2.5", "M12 10.5v3", "M12 17.5V20",
    )
    val Flag = KtIcon("flag",
        "M5 21V4h11l-2 4 2 4H5",
    )
    val Database = KtIcon("database",
        "M5 6a7 2.5 0 1 0 14 0a7 2.5 0 1 0 -14 0z",
        "M5 6v12c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5V6",
        "M5 12c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5",
    )
    val Map = KtIcon("map",
        "M9 4L3.5 6v14L9 18l6 2 5.5-2V4L15 6z",
        "M9 4v14M15 6v14",
    )
    val Sliders = KtIcon("sliders",
        "M4 7h10M18 7h2M4 17h4M12 17h8",
        "M14 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
        "M8 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
    )
    val Shield = KtIcon("shield",
        "M12 3.5l7 3v5c0 4.5-3 7.7-7 9-4-1.3-7-4.5-7-9v-5z",
    )
    val ChevronRight = KtIcon("chevron_right",
        "M9 5l7 7-7 7",
    )
    val ChevronLeft = KtIcon("chevron_left",
        "M15 5l-7 7 7 7",
    )
    val ChevronDown = KtIcon("chevron_down",
        "M6 9l6 6 6-6",
    )
    val Check = KtIcon("check",
        "M5 12.5l4.5 4.5L19 7.5",
    )
    val Plus = KtIcon("plus",
        "M12 5v14M5 12h14",
    )
    val Minus = KtIcon("minus",
        "M5 12h14",
    )
    val Menu = KtIcon("menu",
        "M4 7h16M4 12h16M4 17h16",
    )
    val Close = KtIcon("close",
        "M6 6l12 12M18 6L6 18",
    )
    val Info = KtIcon("info",
        "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0z",
        "M12 11v5M12 8h.01",
    )
    val Document = KtIcon("document",
        "M7 3.5h7l4 4V20.5H7z",
        "M14 3.5v4h4",
        "M10 12h5M10 16h5",
    )
    val Globe = KtIcon("globe",
        "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0z",
        "M3.5 12h17M12 3.5c2.5 2.8 2.5 14.2 0 17M12 3.5c-2.5 2.8-2.5 14.2 0 17",
    )
    val Ruler = KtIcon("ruler",
        "M4.5 8h15a1.5 1.5 0 0 1 1.5 1.5v5a1.5 1.5 0 0 1 -1.5 1.5h-15a1.5 1.5 0 0 1 -1.5 -1.5v-5a1.5 1.5 0 0 1 1.5 -1.5z",
        "M7 8v3M11 8v4M15 8v3M19 8v3",
    )
    val Moon = KtIcon("moon",
        "M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z",
    )
    val Download = KtIcon("download",
        "M12 4v11M7 10l5 5 5-5",
        "M5 20h14",
    )
    val Stats = KtIcon("stats",
        "M5 20V10M12 20V4M19 20v-7",
    )

    val ArrowRight = KtIcon("arrow_right",
        "M5 12h14M13 6l6 6-6 6",
    )
    val Trash = KtIcon("trash",
        "M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13",
    )

    val Share = KtIcon("share",
        "M12 4v11M8 8l4-4 4 4",
        "M6 12v7.5h12V12",
    )

    val all = listOf(
        Search, Bus, Walk, Pin, Nav, Locate, Swap, Clock, History, Star, Home, Cap, Brief, Layers, CloudOff, WifiOff, Refresh, Alert, Live, LiveOff, Calendar, Detour, Cancel, Access, TurnLeft, TurnRight, Straight, SlightLeft, SlightRight, SharpLeft, SharpRight, UTurn, Stairs, Crossing, Food, Coffee, Bag, Health, Tree, Bed, Landmark, Road, Flag, Database, Map, Sliders, Shield, ChevronRight, ChevronLeft, ChevronDown, Check, Plus, Minus, Menu, Close, Info, Document, Globe, Ruler, Moon, Download, Stats, ArrowRight, Trash, Share,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun IconsPreview() {
    FlowRow(
        Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KtIcons.all.forEach { icon ->
            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                KonstanzIcon(icon, contentDescription = null, tint = Ink, strokeWidth = 2f)
                Text(icon.name, style = KonstanzType.Caption, color = Ink3, maxLines = 1)
            }
        }
    }
}
