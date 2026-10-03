package com.example.konstanz.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** Corner radii from the design. */
object Radius {
    val Badge = RoundedCornerShape(7.dp)        // line badges
    val Small = RoundedCornerShape(12.dp)       // icon tiles, small cards
    val Button = RoundedCornerShape(14.dp)      // 52 dp buttons, floating controls
    val Card = RoundedCornerShape(16.dp)        // cards, inputs
    val Group = RoundedCornerShape(18.dp)       // grouped settings lists
    val Panel = RoundedCornerShape(20.dp)       // floating panels (search, departures card)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // bottom sheets
    val Hero = RoundedCornerShape(28.dp)        // onboarding illustration card
    val Pill = RoundedCornerShape(percent = 50) // chips, status pills
}

/** 4 dp spacing grid. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Minimum touch target / floating control size. */
    val TouchTarget = 48.dp
    val ButtonHeight = 52.dp
}

private val ShadowInk = Color(0xFF16181D)

/** "Outline" elevation: flat surface with a 1.5 dp Line border. */
fun Modifier.outlined(shape: Shape): Modifier =
    border(1.5.dp, Line, shape)

/** "Floating" elevation: map controls, search bar, cards over the map. */
fun Modifier.floatingShadow(shape: Shape): Modifier = this
    .dropShadow(shape, Shadow(radius = 20.dp, offset = DpOffset(0.dp, 6.dp), color = ShadowInk, alpha = 0.12f))
    .dropShadow(shape, Shadow(radius = 3.dp, offset = DpOffset(0.dp, 1.dp), color = ShadowInk, alpha = 0.10f))

/** "Sheet" elevation: bottom sheets, shadow cast upward. */
fun Modifier.sheetShadow(shape: Shape): Modifier = this
    .dropShadow(shape, Shadow(radius = 32.dp, offset = DpOffset(0.dp, (-10).dp), color = ShadowInk, alpha = 0.14f))
    .dropShadow(shape, Shadow(radius = 6.dp, offset = DpOffset(0.dp, (-2).dp), color = ShadowInk, alpha = 0.06f))

@Preview(showBackground = true, backgroundColor = 0xFFF4F5F7, widthDp = 390, heightDp = 560)
@Composable
private fun DimensPreview() {
    Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text("ELEVATION", style = KonstanzType.Label, color = Ink3)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            listOf<Pair<String, Modifier>>(
                "Outline" to Modifier.outlined(Radius.Card),
                "Floating" to Modifier.floatingShadow(Radius.Card),
                "Sheet" to Modifier.sheetShadow(Radius.Card),
            ).forEach { (name, mod) ->
                Box(
                    mod.size(104.dp, 72.dp).background(White, Radius.Card),
                    contentAlignment = Alignment.Center,
                ) { Text(name, style = KonstanzType.RowTitle) }
            }
        }
        Text("RADIUS", style = KonstanzType.Label, color = Ink3)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf(Radius.Badge, Radius.Small, Radius.Button, Radius.Card, Radius.Panel, Radius.Hero, Radius.Pill).forEach {
                Box(Modifier.size(44.dp).background(PrimaryTint, it).border(2.dp, Primary, it))
            }
        }
        Text("SPACING", style = KonstanzType.Label, color = Ink3)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.Bottom) {
            listOf(Spacing.xxs, Spacing.xs, Spacing.s, Spacing.m, Spacing.l, Spacing.xl, Spacing.xxl, Spacing.xxxl).forEach {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(it).height(it).background(Primary))
                    Text("${it.value.toInt()}", style = KonstanzType.Caption, color = Ink3)
                }
            }
        }
    }
}

/** Height of the app's window (not the whole screen): for sheets sized as a share of it. */
@androidx.compose.runtime.Composable
fun windowHeight(): androidx.compose.ui.unit.Dp =
    with(androidx.compose.ui.platform.LocalDensity.current) {
        androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp()
    }
