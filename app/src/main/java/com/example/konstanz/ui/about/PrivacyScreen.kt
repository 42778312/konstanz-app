package com.example.konstanz.ui.about

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.konstanz.data.AppInfo
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Spacing

/**
 * The privacy policy, in the app. It describes what the app really does: it has no internet permission,
 * so nothing it knows can leave the phone. Keep it true when adding features (e.g. live times).
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(Background)) {
        KtTopBar(stringResource(R.string.privacy_policy), onBack = onBack, background = Background)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = Spacing.m, end = Spacing.m, top = 8.dp, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section(
                stringResource(R.string.pp_short),
                stringResource(R.string.pp_short_body),
            )
            Section(
                stringResource(R.string.pp_location),
                stringResource(R.string.pp_location_body),
            )
            Section(
                stringResource(R.string.pp_saved),
                stringResource(R.string.pp_saved_body),
            )
            Section(
                stringResource(R.string.pp_internet),
                stringResource(R.string.pp_internet_body),
            )
            Section(
                stringResource(R.string.data_sources),
                stringResource(R.string.pp_sources_body),
            )
            AppInfo.SUPPORT_EMAIL?.let { Section(stringResource(R.string.pp_contact), stringResource(R.string.pp_contact_body, it)) }
            Text(stringResource(R.string.pp_last_updated, AppInfo.PRIVACY_POLICY_DATE), style = KonstanzType.Caption, color = Ink3)
        }
    }
}

@Composable
private fun Section(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, Modifier.semantics { heading() }, style = KonstanzType.Title, color = Ink)
        Text(text, style = KonstanzType.Body.copy(lineHeight = 1.5.em), color = Ink2)
    }
}

@Preview(widthDp = 390, heightDp = 1200)
@Composable
private fun PrivacyPreview() {
    KonstanzTheme { PrivacyScreen(onBack = {}) }
}
