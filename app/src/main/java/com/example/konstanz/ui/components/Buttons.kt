package com.example.konstanz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.OnPrimaryTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryPressed
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.Spacing
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow
import com.example.konstanz.ui.theme.outlined

/** The six button looks from the design system's "Buttons" row. */
enum class ButtonVariant(
    private val colors: () -> Triple<Color, Color, Color>,
    val outlined: Boolean = false,
) {
    /** "Find routes" — one per screen. */
    Primary({ Triple(com.example.konstanz.ui.theme.Primary, PrimaryPressed, White) }),
    /** "Route from here" — secondary action next to a primary one. */
    Tonal({ Triple(PrimaryTint, lerp(PrimaryTint, com.example.konstanz.ui.theme.Primary, 0.12f), OnPrimaryTint) }),
    /** "All departures" — neutral action on white surfaces. */
    Neutral({ Triple(Background, Line, Ink) }),
    /** "Add a place" — white with outline, on grey surfaces. */
    Outline({ Triple(Surface, Background, Ink) }, outlined = true),
    /** "Clear all" — text only, destructive-ish. */
    Text({ Triple(Color.Transparent, PrimaryTint, OnPrimaryTint) });

    // Read from the active palette each time, so the variants follow light/dark mode.
    val container: Color get() = colors().first
    val pressed: Color get() = colors().second
    val content: Color get() = colors().third
}

/** 52 dp button, 14 dp radius, Figtree 700/17. Disabled = 40 % opacity, as in "Update data". */
@Composable
fun KtButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: KtIcon? = null,
    /** Two buttons side by side (design: 16 sp text, tighter sides) so labels like "Route from here" fit. */
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(Spacing.ButtonHeight)
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (variant.outlined) Modifier.outlined(Radius.Button) else Modifier)
            .clip(Radius.Button)
            .background(if (pressed) variant.pressed else variant.container)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) Spacing.s else Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            KonstanzIcon(leadingIcon, contentDescription = null, size = 20.dp, tint = variant.content)
        }
        Text(
            text,
            style = if (compact) KonstanzType.Button.copy(fontSize = 16.sp) else KonstanzType.Button,
            color = variant.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 48 dp square floating control over the map (layers, zoom). */
@Composable
fun FloatingIconButton(
    icon: KtIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Spacing.TouchTarget)
            .floatingShadow(Radius.Button)
            .clip(Radius.Button)
            .background(Surface)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzIcon(icon, contentDescription, tint = Ink, size = 22.dp)
    }
}

/** 56 dp round floating control — "my location". */
@Composable
fun FloatingRoundButton(
    icon: KtIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .floatingShadow(CircleShape)
            .clip(CircleShape)
            .background(Surface)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzIcon(icon, contentDescription, tint = tint, size = 24.dp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F5F7, widthDp = 390)
@Composable
private fun ButtonsPreview() {
    KonstanzTheme {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            KtButton("Find routes", {}, Modifier.fillMaxWidth())
            KtButton("Route from here", {}, Modifier.fillMaxWidth(), ButtonVariant.Tonal, leadingIcon = KtIcons.Nav)
            KtButton("All departures", {}, Modifier.fillMaxWidth(), ButtonVariant.Neutral)
            KtButton("Add a place", {}, Modifier.fillMaxWidth(), ButtonVariant.Outline, leadingIcon = KtIcons.Plus)
            KtButton("Clear all", {}, Modifier.fillMaxWidth(), ButtonVariant.Text)
            KtButton("Update data", {}, Modifier.fillMaxWidth(), enabled = false)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                FloatingIconButton(KtIcons.Layers, "Map layers", {})
                FloatingRoundButton(KtIcons.Locate, "My location", {}, tint = Primary)
            }
        }
    }
}
