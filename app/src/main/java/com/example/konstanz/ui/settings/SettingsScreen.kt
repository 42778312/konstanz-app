package com.example.konstanz.ui.settings

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.konstanz.data.AppInfo
import com.example.konstanz.data.DataHealth
import com.example.konstanz.data.OfflineData
import com.example.konstanz.data.OfflineDataInfo
import com.example.konstanz.data.display
import com.example.konstanz.data.formatBytes
import com.example.konstanz.data.RoutePreference
import com.example.konstanz.data.SettingsRepository
import com.example.konstanz.data.UserSettings
import com.example.konstanz.ui.components.IconChip
import com.example.konstanz.ui.components.SettingsGroup
import com.example.konstanz.ui.components.SettingsRow
import com.example.konstanz.ui.components.SwitchRow
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.offline.shownHealth
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Delayed
import com.example.konstanz.ui.theme.DelayedTint
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.LiveTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.Spacing
import com.example.konstanz.ui.theme.White
import kotlinx.coroutines.launch

/** Where each row leads. */
data class SettingsActions(
    val onClose: () -> Unit,
    val onOpenOfflineData: () -> Unit = {},
    val onOpenAbout: () -> Unit = {},
    val onOpenLicenses: () -> Unit = {},
    val onOpenPrivacy: () -> Unit = {},
    /** Developer builds only: switch offline / error states on (Part J). */
    val onOpenSimulate: () -> Unit = {},
    val onOpenDesignSystem: () -> Unit = {},
)

/** 27 Settings, wired to the stored preferences and the real location permission. */
@Composable
fun SettingsRoute(actions: SettingsActions) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settings.collectAsState(initial = UserSettings())
    val scope = rememberCoroutineScope()

    // Re-read on every resume: the user may change the permission in system settings and come back.
    var location by remember { mutableStateOf(locationAccess(context)) }
    LifecycleResumeEffect(Unit) {
        location = locationAccess(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) { OfflineData.refresh(context) }

    SettingsScreen(
        settings = settings,
        locationLabel = location,
        version = remember { AppInfo.versionName(context) },
        offlineData = OfflineData.info,
        showDeveloperOptions = remember { AppInfo.isDebuggable(context) },
        actions = actions,
        onOpenLocationSettings = { openAppSettings(context) },
        update = { change -> scope.launch { change(repository) } },
    )
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    locationLabel: String,
    version: String,
    offlineData: OfflineDataInfo?,
    showDeveloperOptions: Boolean,
    actions: SettingsActions,
    onOpenLocationSettings: () -> Unit,
    update: (suspend SettingsRepository.() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Header(onClose = actions.onClose)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.m, end = Spacing.m, top = Spacing.xs, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            SettingsGroup(stringResource(R.string.routing)) {
                Column(Modifier.selectableGroup()) {
                    RoutePreference.entries.forEach { option ->
                        RadioRow(option.label, selected = settings.routePreference == option) {
                            update { setRoutePreference(option) }
                        }
                    }
                }
            }

            SettingsGroup(stringResource(R.string.offline_data)) {
                val health = offlineData.shownHealth
                SettingsRow(
                    stringResource(R.string.timetable_map), icon = KtIcons.Database, onClick = actions.onOpenOfflineData, showDivider = false, showChevron = true,
                    subtitle = offlineData?.let { info ->
                        listOfNotNull(info.validUntil?.let { stringResource(R.string.timetable_until, it.display()) }, formatBytes(info.totalBytes)).joinToString(" · ")
                    },
                    trailing = {
                        when (health) {
                            null -> Unit
                            DataHealth.Ok -> IconChip(stringResource(R.string.up_to_date), KtIcons.Live, Live, LiveTint)
                            DataHealth.ExpiresSoon -> IconChip(stringResource(R.string.ends_soon), KtIcons.Calendar, Delayed, DelayedTint)
                            DataHealth.Expired -> IconChip(stringResource(R.string.expired), KtIcons.Alert, Delayed, DelayedTint)
                            DataHealth.Damaged -> IconChip(stringResource(R.string.repair), KtIcons.Alert, Delayed, DelayedTint)
                        }
                    },
                )
            }

            SettingsGroup(stringResource(R.string.privacy)) {
                LinkRow(stringResource(R.string.location), KtIcons.Nav, locationLabel, onOpenLocationSettings, last = true)
            }

            SettingsGroup(stringResource(R.string.about)) {
                SettingsRow(
                    stringResource(R.string.version), icon = KtIcons.Info,
                    value = version + (offlineData?.timetableDate?.let { " · " + stringResource(R.string.timetable_x, it.display()) } ?: ""),
                )
                LinkRow(stringResource(R.string.about_sources), KtIcons.Database, null, actions.onOpenAbout)
                LinkRow(stringResource(R.string.licenses), KtIcons.Document, null, actions.onOpenLicenses)
                LinkRow(stringResource(R.string.privacy_policy), KtIcons.Shield, null, actions.onOpenPrivacy, last = true)
            }

            if (showDeveloperOptions) {
                SettingsGroup("Developer") {
                    LinkRow("Simulate states", KtIcons.Alert, null, actions.onOpenSimulate)
                    LinkRow("Design system", KtIcons.Layers, null, actions.onOpenDesignSystem, last = true)
                }
            }
        }
    }

}

/** Large title + round close button (design: 112 px bar incl. status bar, 30 px / 800 title). */
@Composable
private fun Header(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.tab_settings),
            modifier = Modifier.semantics { heading() },
            style = KonstanzType.TitleL.copy(fontSize = 30.sp),
            color = Ink,
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Surface)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.close_settings), onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            KonstanzIcon(KtIcons.Close, contentDescription = stringResource(R.string.close_settings), size = 22.dp, tint = Ink)
        }
    }
}

/** Row that opens another screen. Without a destination yet it renders as plain information. */
@Composable
private fun LinkRow(title: String, icon: com.example.konstanz.ui.icons.KtIcon, value: String?, onClick: (() -> Unit)?, last: Boolean = false) {
    SettingsRow(title, icon = icon, value = value, showChevron = onClick != null, showDivider = !last, onClick = onClick)
}

/** One routing preference: the chosen one shows a red check (design), radio semantics for TalkBack. */
@Composable
private fun RadioRow(title: String, selected: Boolean, onSelect: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .heightIn(min = 56.dp)
                .padding(horizontal = Spacing.m, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = KonstanzType.RowTitle, color = Ink)
            if (selected) KonstanzIcon(KtIcons.Check, contentDescription = null, size = 22.dp, tint = Primary, strokeWidth = 2.6f)
        }
        HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

// ---------- Platform helpers ----------

private fun locationAccess(context: Context): String {
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    return when {
        granted(Manifest.permission.ACCESS_FINE_LOCATION) -> context.getString(R.string.loc_while_using)
        granted(Manifest.permission.ACCESS_COARSE_LOCATION) -> context.getString(R.string.loc_approximate)
        else -> context.getString(R.string.loc_off)
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun SettingsPreview() {
    KonstanzTheme {
        SettingsScreen(
            settings = UserSettings(),
            locationLabel = "While using",
            version = "1.0.0",
            offlineData = null,
            showDeveloperOptions = true,
            actions = SettingsActions(onClose = {}),
            onOpenLocationSettings = {},
            update = {},
        )
    }
}
