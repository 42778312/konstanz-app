package com.example.konstanz.ui.onboarding

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.theme.DotInactive
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White

/**
 * Shared layout of onboarding artboards 02–06:
 * pink illustration card on top, step dots, Bricolage headline, body, actions at the bottom.
 *
 * On the 390×844 design the card is 400 px and the text + actions block below it 354 px.
 * Here the lower block gets at least that share of the height and grows for large text;
 * the card takes whatever is left, so nothing is ever clipped.
 */
@Composable
fun OnboardingPage(
    step: Int,
    stepCount: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    illustration: @Composable BoxScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(White).systemBarsPadding()) {
        val lowerMin = maxHeight * (354f / 784f)
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(Radius.Hero)
                    .background(PrimaryTint),
            ) {
                illustration()
                if (onSkip != null) SkipButton(onSkip, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp))
            }
            Column(Modifier.fillMaxWidth().heightIn(min = lowerMin), verticalArrangement = Arrangement.SpaceBetween) {
                Column(
                    Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StepDots(step, stepCount)
                    Text(
                        title,
                        modifier = Modifier.semantics { heading() },
                        style = KonstanzType.Headline.copy(lineBreak = LineBreak.Heading),
                        color = Ink,
                    )
                    Text(body, style = KonstanzType.Body.copy(fontWeight = FontWeight.Normal, lineBreak = LineBreak.Paragraph), color = Ink2)
                }
                Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 30.dp, top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = actions,
                )
            }
        }
    }
}

/** Page indicator: the current step is a 22 dp red bar, the others 8 dp grey dots. */
@Composable
fun StepDots(step: Int, stepCount: Int, modifier: Modifier = Modifier) {
    val stepLabel = stringResource(R.string.step_x_of_y, step + 1, stepCount)
    Row(
        modifier.semantics { contentDescription = stepLabel },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(stepCount) { i ->
            val active = i == step
            val width by animateDpAsState(if (active) 22.dp else 8.dp, label = "dot-width")
            val color by animateColorAsState(if (active) Primary else DotInactive, label = "dot-color")
            Box(Modifier.size(width = width, height = 8.dp).background(color, Radius.Pill))
        }
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(44.dp)
            .clip(Radius.Button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.skip), style = KonstanzType.RowTitle, color = Ink2)
    }
}

/** Quiet secondary action under the main button ("Not now"): 48 dp, ink-2 text. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(Radius.Button)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = KonstanzType.RowTitle, color = Ink2)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingPagePreview() {
    KonstanzTheme {
        OnboardingPage(
            step = 2, stepCount = 5,
            title = "Find buses quickly",
            body = "See bus stops, schedules, departures and routes — with live times whenever they're available.",
            onSkip = {},
            illustration = {},
            actions = { KtButton("Continue", {}, Modifier.fillMaxWidth()) },
        )
    }
}
