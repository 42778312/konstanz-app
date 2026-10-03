package com.example.konstanz.ui.about

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.AppInfo
import com.example.konstanz.data.OfflineData
import com.example.konstanz.data.display
import com.example.konstanz.ui.components.KonstanzLogoTile
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.components.SettingsGroup
import com.example.konstanz.ui.components.SettingsRow
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Spacing

/**
 * Where About's legal rows lead. Null = not available yet (no chevron, not tappable).
 * Privacy policy and feedback need a real URL / address from the project owner.
 */
data class AboutActions(
    val onBack: () -> Unit,
    val onOpenLicenses: (() -> Unit)? = null,
    val onOpenPrivacyPolicy: (() -> Unit)? = null,
    val onSendFeedback: (() -> Unit)? = null,
)

/** 35 About / data sources. */
@Composable
fun AboutRoute(actions: AboutActions) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { OfflineData.refresh(context) }
    val timetable = OfflineData.info?.timetableDate?.display() ?: "—"
    AboutScreen(version = remember { AppInfo.versionName(context) }, timetableDate = timetable, actions = actions)
}

@Composable
fun AboutScreen(version: String, timetableDate: String, actions: AboutActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(Background)) {
        KtTopBar(stringResource(R.string.about), onBack = actions.onBack, background = Background)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KonstanzLogoTile(size = 80.dp)
                Text("Konstanz Transit", style = KonstanzType.Display.copy(fontSize = 26.sp, letterSpacing = 0.sp), color = Ink)
                Text(
                    stringResource(R.string.version_line, version, timetableDate),
                    style = KonstanzType.Caption.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = Ink3,
                )
            }

            SettingsGroup(stringResource(R.string.data_sources)) {
                SettingsRow(stringResource(R.string.map_places_streets), subtitle = "© OpenStreetMap contributors · ODbL", icon = KtIcons.Map)
                SettingsRow(stringResource(R.string.timetable), subtitle = "Datensatz der NVBW GmbH · Datenlizenz Deutschland 2.0 · GTFS", icon = KtIcons.Calendar)
                SettingsRow(stringResource(R.string.bus_stops), subtitle = "Stadt Konstanz · Bushaltestellen", icon = KtIcons.Bus)
                SettingsRow(stringResource(R.string.routing), subtitle = stringResource(R.string.runs_on_device), icon = KtIcons.Nav, showDivider = false)
            }

            SettingsGroup(stringResource(R.string.legal)) {
                LegalRow(stringResource(R.string.oss_licenses), KtIcons.Document, actions.onOpenLicenses)
                LegalRow(stringResource(R.string.privacy_policy), KtIcons.Shield, actions.onOpenPrivacyPolicy, last = actions.onSendFeedback == null)
                actions.onSendFeedback?.let { LegalRow(stringResource(R.string.send_feedback), KtIcons.Info, it, last = true) }
            }

            Text(
                stringResource(R.string.times_disclaimer),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = KonstanzType.Caption,
                color = Ink3,
            )
        }
    }
}

@Composable
private fun LegalRow(title: String, icon: com.example.konstanz.ui.icons.KtIcon, onClick: (() -> Unit)?, last: Boolean = false) {
    SettingsRow(title, icon = icon, showChevron = onClick != null, showDivider = !last, onClick = onClick)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AboutPreview() {
    KonstanzTheme {
        AboutScreen("1.0.0", "20 Sep 2026", AboutActions(onBack = {}, onOpenLicenses = {}, onOpenPrivacyPolicy = {}, onSendFeedback = {}))
    }
}
