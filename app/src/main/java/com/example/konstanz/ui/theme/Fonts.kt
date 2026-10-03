package com.example.konstanz.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.R

// Both fonts are variable TTFs bundled in res/font so the app works offline.
// One file serves every weight; the weight axis is set per Font entry (API 26+).

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight, vararg extra: FontVariation.Setting) =
    Font(
        resId = resId,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), *extra),
    )

/** Body / UI face: Figtree 400–800. */
val Figtree = FontFamily(
    variableFont(R.font.figtree, FontWeight.Normal),
    variableFont(R.font.figtree, FontWeight.Medium),
    variableFont(R.font.figtree, FontWeight.SemiBold),
    variableFont(R.font.figtree, FontWeight.Bold),
    variableFont(R.font.figtree, FontWeight.ExtraBold),
)

/** Display face: Bricolage Grotesque 600–800, optical size tuned for large headlines. */
val BricolageGrotesque = FontFamily(
    variableFont(R.font.bricolage_grotesque, FontWeight.SemiBold, FontVariation.Setting("opsz", 48f)),
    variableFont(R.font.bricolage_grotesque, FontWeight.Bold, FontVariation.Setting("opsz", 48f)),
    variableFont(R.font.bricolage_grotesque, FontWeight.ExtraBold, FontVariation.Setting("opsz", 48f)),
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun FontsPreview() {
    Column(Modifier.padding(16.dp)) {
        Text("Konstanz Transit", fontFamily = BricolageGrotesque, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).forEach {
            Text("Figtree ${it.weight} · 14:32 – 14:48", fontFamily = Figtree, fontWeight = it, fontSize = 18.sp)
        }
    }
}
