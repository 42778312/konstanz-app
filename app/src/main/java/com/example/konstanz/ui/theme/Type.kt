package com.example.konstanz.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Text styles from the "00 Design system" artboard.
 * Times always use [Time] (tabular numerals) so departure columns line up.
 * [Label] is shown in caps: call `.uppercase()` on the text, Compose styles can't transform case.
 */
object KonstanzType {
    /** Bricolage Grotesque 800 / 40 — app name, splash. */
    val Display = TextStyle(
        fontFamily = BricolageGrotesque, fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp, lineHeight = 1.0.em, letterSpacing = (-0.02).em,
    )

    /** Bricolage Grotesque 800 / 32 — onboarding and empty-state headlines. */
    val Headline = TextStyle(
        fontFamily = BricolageGrotesque, fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp, lineHeight = 1.08.em, letterSpacing = (-0.02).em,
    )

    /** Figtree 800 / 28 — stop and place names on detail screens. */
    val TitleL = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 1.15.em, letterSpacing = (-0.02).em,
    )

    /** Figtree 800 / 22 — sheet and section titles. */
    val Title = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp, lineHeight = 1.2.em,
    )

    /** Figtree 800 / 20 — top app bar titles. */
    val BarTitle = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp, lineHeight = 1.2.em, letterSpacing = (-0.01).em,
    )

    /** Figtree 600 / 17 — primary body text. */
    val Body = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 1.45.em,
    )

    /** Figtree 700 / 16 — list row titles. */
    val RowTitle = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 1.3.em,
    )

    /** Figtree 600 / 15 — minimum body size. */
    val BodySmall = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 1.4.em,
    )

    /** Figtree 600 / 13 — row subtitles, hints, footnotes. */
    val Caption = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 1.45.em,
    )

    /** Figtree 800 / 20, tabular numerals — departure and arrival times. */
    val Time = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp, lineHeight = 1.2.em, fontFeatureSettings = "tnum",
    )

    /** Figtree 800 / 13, tracked caps — section labels ("NEXT DEPARTURES"). */
    val Label = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp, lineHeight = 1.3.em, letterSpacing = 0.08.em,
    )

    /** Figtree 700 / 17 — button text. */
    val Button = TextStyle(
        fontFamily = Figtree, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 1.2.em,
    )
}

/** Material slots mapped to the design styles, so stock M3 components pick up the right fonts. */
val Typography = Typography(
    displayLarge = KonstanzType.Display,
    headlineMedium = KonstanzType.Headline,
    titleLarge = KonstanzType.TitleL,
    titleMedium = KonstanzType.Title,
    titleSmall = KonstanzType.RowTitle,
    bodyLarge = KonstanzType.Body,
    bodyMedium = KonstanzType.BodySmall,
    bodySmall = KonstanzType.Caption,
    labelLarge = KonstanzType.Button,
    labelMedium = KonstanzType.Caption,
    labelSmall = KonstanzType.Label,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun TypePreview() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Konstanz Transit", style = KonstanzType.Display, color = Ink)
        Text("Find buses quickly", style = KonstanzType.Headline, color = Ink)
        Text("Konstanz Bahnhof", style = KonstanzType.TitleL, color = Ink)
        Text("Plan your journey", style = KonstanzType.Title, color = Ink)
        Text("Walk 4 min to Konstanz Bahnhof, then take Bus 12.", style = KonstanzType.Body, color = Ink2)
        Text("Universität Konstanz", style = KonstanzType.RowTitle, color = Ink)
        Text("Minimum body size, 15 sp.", style = KonstanzType.BodySmall, color = Ink2)
        Text("© OpenStreetMap contributors · ODbL", style = KonstanzType.Caption, color = Ink3)
        Text("14:32 – 14:48 · 16 min", style = KonstanzType.Time, color = Ink)
        Text("11:11\n18:08\n20:40", style = KonstanzType.Time, color = Live)
        Text("Next departures".uppercase(), style = KonstanzType.Label, color = Ink3)
        Text("Find routes", style = KonstanzType.Button, color = Primary)
    }
}
