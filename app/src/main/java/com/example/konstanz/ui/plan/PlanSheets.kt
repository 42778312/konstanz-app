package com.example.konstanz.ui.plan

import com.example.konstanz.ui.components.placeDisplayName
import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.RoutePreference
import com.example.konstanz.data.transit.Journey
import com.example.konstanz.data.transit.Leg
import com.example.konstanz.data.transit.Minutes
import com.example.konstanz.ui.components.BadgeSize
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.LineBadge
import com.example.konstanz.ui.components.RouteCard
import com.example.konstanz.ui.components.SectionLabel
import com.example.konstanz.ui.components.Shimmer
import com.example.konstanz.ui.components.StatusChip
import com.example.konstanz.ui.components.SwitchRow
import com.example.konstanz.ui.components.toTripStatus
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.Ink4
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.SkeletonBar
import com.example.konstanz.ui.theme.SwitchTrackOff
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow
import com.example.konstanz.ui.theme.outlined
import com.example.konstanz.ui.theme.sheetShadow
import com.example.konstanz.ui.components.SmallSpinner

/** Unselected radio ring (#B9BCC3 in artboard 17). */
private val RadioOff = androidx.compose.ui.graphics.Color(0xFFB9BCC3)

// ---------- Shared pieces ----------

/** White bottom sheet with the grabber (design system → Bottom sheets). */
@Composable
fun PlanSheet(modifier: Modifier = Modifier, bottomPadding: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .sheetShadow(Radius.Sheet)
            .clip(Radius.Sheet)
            .background(White)
            .then(if (bottomPadding) Modifier.navigationBarsPadding() else Modifier)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = if (bottomPadding) 20.dp else 0.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).background(SkeletonBar, Radius.Pill))
        Spacer(Modifier.height(15.dp))
        content()
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, Modifier.semantics { heading() }, style = KonstanzType.Title.copy(letterSpacing = (-0.01).em), color = Ink)
            if (subtitle != null) Text(subtitle, style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink3)
        }
        trailing()
    }
}

@Composable
fun CircleButton(icon: KtIcon, label: String, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Background).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { KonstanzIcon(icon, contentDescription = label, size = 20.dp, tint = Ink) }
}

/** 64 dp floating bar: back, "My location → Universität Konstanz", summary, options (artboards 18, 19, 30, 33). */
@Composable
fun PlanBar(from: String, to: String, subtitle: String, onBack: () -> Unit, onEdit: () -> Unit, onOptions: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .floatingShadow(shape)
            .clip(shape)
            .background(White)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(48.dp).clip(Radius.Button).clickable(role = Role.Button, onClickLabel = stringResource(R.string.back), onClick = onBack), contentAlignment = Alignment.Center) {
            KonstanzIcon(KtIcons.ChevronLeft, contentDescription = stringResource(R.string.back), size = 22.dp, tint = Ink)
        }
        Column(Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = stringResource(R.string.change_route), onClick = onEdit), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(placeDisplayName(from), style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Ink, maxLines = 1)
                KonstanzIcon(KtIcons.ArrowRight, contentDescription = stringResource(R.string.to_word), size = 16.dp, tint = Ink3)
                Text(placeDisplayName(to), style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(subtitle, style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink3, maxLines = 1)
        }
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Background).clickable(role = Role.Button, onClickLabel = stringResource(R.string.route_options), onClick = onOptions),
            contentAlignment = Alignment.Center,
        ) { KonstanzIcon(KtIcons.Sliders, contentDescription = stringResource(R.string.route_options), size = 20.dp, tint = Ink) }
    }
}

/** "🚶4 › [12] › 🚶3" — the legs of a journey in one line. */
@Composable
fun LegsStrip(journey: Journey, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        journey.legs.forEachIndexed { i, leg ->
            if (i > 0) KonstanzIcon(KtIcons.ChevronRight, contentDescription = null, size = 14.dp, tint = Ink4, strokeWidth = 2.4f)
            when (leg) {
                is Leg.Walk -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    KonstanzIcon(KtIcons.Walk, contentDescription = stringResource(R.string.walk), size = 16.dp, tint = Ink2)
                    Text("${leg.minutes}", style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = Ink2)
                }
                is Leg.Ride -> LineBadge(leg.line, size = BadgeSize.S)
            }
        }
    }
}

/** "Direct · 7 min walking", "1 transfer · 6 min walking". */
fun Journey.summary(): String {
    if (rides.isEmpty()) return Texts.get(R.string.walk_only, walkingMinutes)
    val changes = if (transfers == 0) Texts.get(R.string.direct) else Texts.plural(R.plurals.n_transfers, transfers, transfers)
    return Texts.get(R.string.summary_walking, changes, walkingMinutes)
}

// ---------- 16 Plan your journey ----------

@Composable
fun InputSheet(
    from: String?,
    to: String?,
    options: PlanOptions,
    onFrom: () -> Unit,
    onTo: () -> Unit,
    onSwap: () -> Unit,
    onOptions: () -> Unit,
    onFind: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanSheet(modifier) {
        SheetTitle(stringResource(R.string.plan_journey)) { CircleButton(KtIcons.Close, stringResource(R.string.close), onClose) }
        Spacer(Modifier.height(14.dp))
        RouteCard(from = from, to = to, onFrom = onFrom, onTo = onTo, onSwap = onSwap)
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionButton(KtIcons.Clock, options.timeLabel(), trailingChevron = true, onClick = onOptions)
            OptionButton(KtIcons.Sliders, options.preference.label, onClick = onOptions)
        }
        KtButton(stringResource(R.string.find_routes), onFind, Modifier.fillMaxWidth().padding(top = 16.dp), leadingIcon = KtIcons.Search, enabled = from != null && to != null)
        FooterNote(KtIcons.Database, stringResource(R.string.calculated_on_phone), Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun OptionButton(icon: KtIcon, text: String, onClick: () -> Unit, trailingChevron: Boolean = false) {
    Row(
        Modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(Background).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KonstanzIcon(icon, contentDescription = null, size = 18.dp, tint = Ink)
        Text(text, style = KonstanzType.RowTitle, color = Ink, maxLines = 1)
        if (trailingChevron) KonstanzIcon(KtIcons.ChevronDown, contentDescription = null, size = 18.dp, tint = Ink)
    }
}

@Composable
fun FooterNote(icon: KtIcon, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        KonstanzIcon(icon, contentDescription = null, size = 16.dp, tint = Ink3)
        Text(text, style = KonstanzType.Caption, color = Ink3)
    }
}

// ---------- 17 When & how ----------

@Composable
fun PrefsSheet(
    options: PlanOptions,
    now: Minutes,
    onChange: (PlanOptions) -> Unit,
    onClose: () -> Unit,
    onFind: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlanSheet(modifier) {
        SheetTitle(stringResource(R.string.when_how)) { CircleButton(KtIcons.Close, stringResource(R.string.close), onClose) }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Segmented(options.timeMode, onSelect = { mode ->
                onChange(
                    if (mode == TimeMode.Now) options.copy(timeMode = mode, dayOffset = 0, time = now)
                    // The wheel has 5-minute steps: start at the next one, never just before now.
                    else options.copy(timeMode = mode, time = if (options.timeMode == TimeMode.Now) Minutes(((now.value + 4) / 5 * 5).coerceAtMost(23 * 60 + 55)) else options.time)
                )
            })
            if (options.timeMode != TimeMode.Now) TimeWheel(options, onChange)

            SectionLabel(stringResource(R.string.prefer), Modifier.padding(top = 8.dp, bottom = 4.dp))
            Column(Modifier.selectableGroup()) {
                listOf(
                    RoutePreference.Fastest to stringResource(R.string.pref_fastest_hint),
                    RoutePreference.LeastWalking to stringResource(R.string.pref_walk_hint),
                    RoutePreference.FewestTransfers to stringResource(R.string.pref_transfers_hint),
                ).forEach { (pref, hint) ->
                    PrefRadio(pref.label, hint, selected = options.preference == pref) { onChange(options.copy(preference = pref)) }
                }
            }
        }
        KtButton(stringResource(R.string.find_routes_at, options.whenLabel(now)), onFind, Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}

@Composable
private fun Segmented(selected: TimeMode, onSelect: (TimeMode) -> Unit) {
    Row(
        Modifier.padding(top = 14.dp).fillMaxWidth().background(Background, Radius.Button).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TimeMode.entries.forEach { mode ->
            val on = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(if (on) Modifier.dropShadow(RoundedCornerShape(10.dp), Shadow(radius = 3.dp, offset = DpOffset(0.dp, 1.dp), color = Ink, alpha = 0.15f)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) White else Background)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(mode) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(mode.label, style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = if (on) Ink else Ink2)
            }
        }
    }
}

/** Day · hour · minute wheel with the selected row on a grey band. */
@Composable
private fun TimeWheel(options: PlanOptions, onChange: (PlanOptions) -> Unit) {
    val days = (-2..4).toList()
    val hours = (0..23).toList()
    val minutes = (0..55 step 5).toList()
    Box(Modifier.padding(top = 8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(36.dp).background(Background, RoundedCornerShape(10.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WheelPicker(days.map { dayLabel(it) }, days.indexOf(options.dayOffset).coerceAtLeast(0), { onChange(options.copy(dayOffset = days[it])) }, stringResource(R.string.day), width = 120.dp)
            WheelPicker(hours.map { "%02d".format(it) }, options.time.value / 60 % 24, { onChange(options.copy(time = Minutes.of(hours[it], options.time.value % 60))) }, stringResource(R.string.hour))
            WheelPicker(minutes.map { "%02d".format(it) }, (options.time.value % 60) / 5, { onChange(options.copy(time = Minutes.of(options.time.value / 60 % 24, minutes[it]))) }, stringResource(R.string.minute))
        }
    }
}

@Composable
private fun PrefRadio(title: String, hint: String, selected: Boolean, onSelect: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = KonstanzType.RowTitle, color = Ink)
                Text(hint, style = KonstanzType.Caption, color = Ink3)
            }
            Box(
                Modifier.size(24.dp).border(if (selected) 7.dp else 2.dp, if (selected) Primary else RadioOff, CircleShape)
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

// ---------- 33 Finding routes ----------

/** Progress of the (mock) route calculation: 0 walking paths, 1 departures, 2 transfers. */
@Composable
fun LoadingSheet(step: Int, modifier: Modifier = Modifier) {
    PlanSheet(modifier) {
        Text(stringResource(R.string.finding_routes), Modifier.padding(bottom = 14.dp), style = KonstanzType.Title, color = Ink)
        Column(
            Modifier.fillMaxWidth().background(Background, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(stringResource(R.string.loading_walk), stringResource(R.string.loading_departures), stringResource(R.string.loading_transfers)).forEachIndexed { i, label ->
                ProgressStep(label, done = i < step, current = i == step)
            }
        }
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) { SkeletonCard() }
        }
    }
}

@Composable
private fun ProgressStep(label: String, done: Boolean, current: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            done -> Box(Modifier.size(22.dp).background(Live, CircleShape), contentAlignment = Alignment.Center) {
                KonstanzIcon(KtIcons.Check, contentDescription = null, size = 14.dp, tint = White, strokeWidth = 3f)
            }
            current -> SmallSpinner(size = 22.dp)
            else -> Box(Modifier.size(22.dp).border(2.dp, SwitchTrackOff, CircleShape))
        }
        Text(
            label,
            style = KonstanzType.RowTitle.copy(fontWeight = if (current) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = if (done || current) Ink else Ink3,
        )
    }
}

@Composable
private fun SkeletonCard() {
    Column(
        Modifier.fillMaxWidth().outlined(RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Shimmer(Modifier.size(130.dp, 18.dp)); Shimmer(Modifier.size(56.dp, 18.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shimmer(Modifier.size(40.dp, 22.dp)); Shimmer(Modifier.size(34.dp, 22.dp)); Shimmer(Modifier.size(40.dp, 22.dp))
        }
        Shimmer(Modifier.fillMaxWidth(0.7f).height(12.dp))
    }
}

// ---------- 18 Routes ----------

@Composable
fun ResultsSheet(
    journeys: List<Journey>,
    selectedIndex: Int,
    preference: RoutePreference,
    onSelect: (Int) -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep the upper half of the map (and the route) visible; the list scrolls (artboard 18).
    val maxHeight = com.example.konstanz.ui.theme.windowHeight() * 0.56f
    PlanSheet(modifier.heightIn(max = maxHeight)) {
        SheetTitle(stringResource(R.string.routes), pluralStringResource(R.plurals.n_options_offline, journeys.size, journeys.size)) {
            Row(
                Modifier.height(40.dp).clip(Radius.Pill).background(Background).clickable(role = Role.Button, onClick = onOptions).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(preference.label, style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ink)
                KonstanzIcon(KtIcons.ChevronDown, contentDescription = null, size = 18.dp, tint = Ink)
            }
        }
        Column(
            Modifier.padding(top = 12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            journeys.forEachIndexed { i, j -> JourneyCard(j, selected = i == selectedIndex) { onSelect(i) } }
        }
    }
}

/** One option: times, duration, legs, first departure + status, transfers and walking. */
@Composable
fun JourneyCard(journey: Journey, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.border(2.dp, Primary, shape) else Modifier.outlined(shape))
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val time = KonstanzType.Time
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${journey.start.format()} – ${journey.end.format()}", style = time, color = Ink)
            Text(stringResource(R.string.minutes_short, journey.minutes), style = time, color = Ink)
        }
        LegsStrip(journey)
        journey.firstRide?.let { ride ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.time_from_stop, ride.start.format(), ride.from),
                    Modifier.weight(1f),
                    style = KonstanzType.BodySmall.copy(fontSize = 14.sp),
                    color = Ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(ride.realtime.toTripStatus())
            }
        }
        Text(journey.summary(), style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink3)
    }
}

// ---------- 30 No route found ----------

@Composable
fun NoRouteSheet(time: Minutes, onChangeDestination: () -> Unit, onChangeTime: () -> Unit, modifier: Modifier = Modifier) {
    PlanSheet(modifier) {
        Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(64.dp).background(Background, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 30.dp, tint = Ink2)
            }
            Text(stringResource(R.string.no_route), Modifier.padding(top = 4.dp).semantics { heading() }, style = KonstanzType.Title.copy(fontSize = 24.sp), color = Ink)
            Text(
                stringResource(R.string.no_route_body, time.format()),
                Modifier.width(300.dp),
                style = KonstanzType.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal),
                color = Ink2,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.no_route_hint), style = KonstanzType.BodySmall, color = Ink3, textAlign = TextAlign.Center)
        }
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            KtButton(stringResource(R.string.change_destination), onChangeDestination, Modifier.fillMaxWidth())
            KtButton(stringResource(R.string.change_time), onChangeTime, Modifier.fillMaxWidth(), variant = ButtonVariant.Neutral)
        }
    }
}
