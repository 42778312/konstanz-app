package com.example.konstanz.ui.onboarding

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.KonstanzTheme
import kotlinx.coroutines.launch

private const val STEP_COUNT = 5

private object Step {
    const val WELCOME = 0
    const val OFFLINE_MAPS = 1
    const val TRANSPORT = 2
    const val LOCATION = 3
    const val OFFLINE_DATA = 4
}

/**
 * Onboarding artboards 02–06 as a swipeable pager.
 * [onFinished] runs for both "Skip" and "Finish Setup"; the caller stores that onboarding is done.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { STEP_COUNT })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun goTo(page: Int) = scope.launch { pager.animateScrollToPage(page) }
    fun next() = goTo(pager.currentPage + 1)

    // Location is optional: whatever the answer, continue to the last step.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { goTo(Step.OFFLINE_DATA) }

    fun requestLocation() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) goTo(Step.OFFLINE_DATA)
        else permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // System Back steps back through the pages before leaving onboarding.
    BackHandler(enabled = pager.currentPage > 0) { goTo(pager.currentPage - 1) }

    OnboardingPager(
        pager = pager,
        onNext = ::next,
        onSkip = onFinished,
        onEnableLocation = ::requestLocation,
        onNotNow = { goTo(Step.OFFLINE_DATA) },
        onFinish = onFinished,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingPager(
    pager: PagerState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onEnableLocation: () -> Unit,
    onNotNow: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(pager, modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
        when (page) {
            Step.WELCOME -> OnboardingPage(
                step = page, stepCount = STEP_COUNT,
                title = stringResource(R.string.ob1_title),
                body = stringResource(R.string.ob1_body),
                onSkip = onSkip,
                illustration = { WelcomeIllustration() },
                actions = { KtButton(stringResource(R.string.get_started), onNext, Modifier.fillMaxWidth()) },
            )
            Step.OFFLINE_MAPS -> OnboardingPage(
                step = page, stepCount = STEP_COUNT,
                title = stringResource(R.string.ob2_title),
                body = stringResource(R.string.ob2_body),
                onSkip = onSkip,
                illustration = { OfflineMapsIllustration() },
                actions = { KtButton(stringResource(R.string.continue_), onNext, Modifier.fillMaxWidth()) },
            )
            Step.TRANSPORT -> OnboardingPage(
                step = page, stepCount = STEP_COUNT,
                title = stringResource(R.string.ob3_title),
                body = stringResource(R.string.ob3_body),
                onSkip = onSkip,
                illustration = { TransportIllustration() },
                actions = { KtButton(stringResource(R.string.continue_), onNext, Modifier.fillMaxWidth()) },
            )
            Step.LOCATION -> OnboardingPage(
                step = page, stepCount = STEP_COUNT,
                title = stringResource(R.string.start_from_here),
                body = stringResource(R.string.ob4_body),
                illustration = { LocationIllustration() },
                actions = {
                    KtButton(stringResource(R.string.ob_enable_location), onEnableLocation, Modifier.fillMaxWidth(), leadingIcon = KtIcons.Nav)
                    QuietButton(stringResource(R.string.not_now), onNotNow)
                },
            )
            else -> OnboardingPage(
                step = page, stepCount = STEP_COUNT,
                title = stringResource(R.string.ob5_title),
                body = stringResource(R.string.ob5_body),
                illustration = { OfflineDataIllustration() },
                actions = { KtButton(stringResource(R.string.finish_setup), onFinish, Modifier.fillMaxWidth()) },
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingScreenPreview() {
    KonstanzTheme { OnboardingScreen(onFinished = {}) }
}
