package com.example.konstanz.ui.recent

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.konstanz.data.transit.Transit
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.RecentSearch
import com.example.konstanz.data.SavedStore
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.components.TopBarTextAction
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.OnPrimaryTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White

/** 29 Recent searches, backed by the sample store until the database exists (Part D). */
@Composable
fun RecentRoute(onBack: () -> Unit, onOpenStop: (stopId: String) -> Unit, onOpenPlace: (placeId: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    RecentScreen(
        items = SavedStore.recent,
        onBack = onBack,
        onOpen = { item ->
            if (item.isStop) scope.launch { Transit.repository.stopByName(item.name)?.let { onOpenStop(it.id) } }
            else scope.launch { Transit.repository.placeByName(item.name)?.let { onOpenPlace(it.id) } }
        },
        onRemove = { SavedStore.removeRecent(it) },
        onClearAll = { SavedStore.clearRecent() },
    )
}

@Composable
fun RecentScreen(
    items: List<RecentSearch>,
    onBack: () -> Unit,
    onOpen: (RecentSearch) -> Unit,
    onRemove: (RecentSearch) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(White)) {
        KtTopBar(stringResource(R.string.recent_searches), onBack = onBack, showDivider = true) {
            if (items.isNotEmpty()) TopBarTextAction(stringResource(R.string.clear_all), onClearAll, color = OnPrimaryTint)
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            items(items, key = { it.id }) { item ->
                SwipeToRemove(onRemove = { onRemove(item) }) {
                    RecentRow(item, onClick = { onOpen(item) }, onRemove = { onRemove(item) })
                }
            }
            item {
                Text(
                    stringResource(if (items.isEmpty()) R.string.no_recent_searches else R.string.recent_hint),
                    modifier = Modifier.padding(16.dp),
                    style = KonstanzType.BodySmall.copy(fontSize = 14.sp),
                    color = Ink3,
                )
            }
        }
    }
}

/**
 * Swipe a row to the left to remove it. Behind the row: the design's ink "Remove" panel.
 * (The design shows a reveal-then-tap; a full swipe removes directly, which is one step shorter.)
 */
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onRemove()
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.CenterEnd) {
                Column(
                    Modifier.width(96.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    KonstanzIcon(KtIcons.Trash, contentDescription = null, size = 20.dp, tint = White)
                    Text(stringResource(R.string.remove), style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), color = White)
                }
            }
        },
    ) { content() }
}

/** 68 dp row: tinted tile (grey pin for places, pink bus for stops), name, when, arrow. */
@Composable
private fun RecentRow(item: RecentSearch, onClick: () -> Unit, onRemove: () -> Unit) {
    val removeLabel = stringResource(R.string.remove)
    Column(Modifier.background(White)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clickable(role = Role.Button, onClick = onClick)
                // Swiping isn't available to everyone: offer "Remove" as an accessibility action too.
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(removeLabel) { onRemove(); true })
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(if (item.isStop) PrimaryTint else Background, Radius.Small),
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(
                    if (item.isStop) KtIcons.Bus else KtIcons.Pin,
                    contentDescription = null,
                    size = 20.dp,
                    tint = if (item.isStop) Primary else Ink,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(item.name, style = KonstanzType.Body.copy(fontWeight = FontWeight.Bold), color = Ink, maxLines = 1)
                Text(item.whenLabel, style = KonstanzType.BodySmall.copy(fontSize = 14.sp), color = Ink3)
            }
            KonstanzIcon(KtIcons.ArrowRight, contentDescription = null, size = 20.dp, tint = Ink3)
        }
        HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RecentPreview() {
    KonstanzTheme {
        RecentScreen(SavedStore.recent, onBack = {}, onOpen = {}, onRemove = {}, onClearAll = {})
    }
}
