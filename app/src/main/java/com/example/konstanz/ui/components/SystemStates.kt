package com.example.konstanz.ui.components

import androidx.compose.ui.res.pluralStringResource
import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.AppStatus
import com.example.konstanz.data.DataHealth
import com.example.konstanz.data.OfflineData
import com.example.konstanz.ui.offline.shownHealth
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Inverse
import com.example.konstanz.ui.theme.Delayed
import com.example.konstanz.ui.theme.DelayedTint
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow

// Design system → System states: "Status lives in a small floating pill, never a blocking dialog."

/** The floating data-status pill, from [OfflineData]. Tapping it opens Offline data (25). */
@Composable
fun DataStatusPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    when (OfflineData.info.shownHealth) {
        DataHealth.Damaged -> StatusPill(stringResource(R.string.data_needs_repair), modifier, icon = KtIcons.Alert, iconTint = Delayed, onClick = onClick)
        DataHealth.Expired -> StatusPill(stringResource(R.string.timetable_expired), modifier, icon = KtIcons.Alert, iconTint = Delayed, onClick = onClick)
        DataHealth.ExpiresSoon -> StatusPill(stringResource(R.string.timetable_ends_soon), modifier, icon = KtIcons.Calendar, iconTint = Delayed, onClick = onClick)
        else -> StatusPill(stringResource(R.string.up_to_date), modifier, icon = KtIcons.Check, iconTint = Live, onClick = onClick)
    }
}

/** Orange banner in stop sheets: "Realtime unavailable · Showing scheduled departures …" (artboard 32). */
@Composable
fun RealtimeUnavailableBanner(minutesAgo: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(DelayedTint, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KonstanzIcon(KtIcons.LiveOff, contentDescription = null, size = 20.dp, tint = Delayed)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.realtime_unavailable), style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
            Text(
                stringResource(R.string.showing_scheduled) + (minutesAgo?.let { "\n" + pluralStringResource(R.plurals.last_realtime_update, it, it) } ?: ""),
                style = KonstanzType.BodySmall.copy(fontSize = 14.sp, lineHeight = 1.35.em),
                color = Ink2,
            )
        }
    }
}

/**
 * One error from the "Error states" board (34): what happened, what still works, one clear next step.
 * [tint] colours the icon tile; the primary action is red, the secondary grey.
 */
@Composable
fun ErrorCard(
    icon: KtIcon,
    title: String,
    message: String,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    tint: Color = Delayed,
    tileColor: Color = DelayedTint,
    primaryIcon: KtIcon? = null,
) {
    Column(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).background(tileColor, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                KonstanzIcon(icon, contentDescription = null, size = 24.dp, tint = tint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = KonstanzType.Title, color = Ink)
                Text(message, style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Normal, lineHeight = 1.45.em), color = Ink2)
            }
        }
        Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            KtButton(primary, onPrimary, Modifier.fillMaxWidth(), leadingIcon = primaryIcon)
            if (secondary != null) KtButton(secondary, onSecondary, Modifier.fillMaxWidth(), variant = ButtonVariant.Neutral)
        }
    }
}

/** A small caption under error actions ("Repair needs a connection · …"). */
@Composable
fun ErrorFootnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.fillMaxWidth().padding(top = 12.dp),
        style = KonstanzType.Caption,
        color = Ink3,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
