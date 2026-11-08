package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.apps.PhoneApps
import net.prototux.aare.media.CarKeyboard
import net.prototux.aare.media.Placeholder
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * The launcher's "Phone apps" tab: every app of the phone's own launcher, run as itself on the
 * car screen. A search field on top opens the car keyboard (parked only, like media search).
 * While driving only navigation and music apps are listed, as the phone screen mirror allows.
 */
@Composable
internal fun PhoneAppsGrid(state: CarUiState, actions: CarActions, modifier: Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    val driving = state.drivingRestricted
    if (driving) typing = false
    val known = state.apps.filter { it.kind == CarAppKind.NAVIGATION || it.kind == CarAppKind.MEDIA }.mapTo(HashSet()) { it.packageName }
    val shown = PhoneApps.filter(
        state.phoneApps.filter { !driving || PhoneApps.drivingAllowed(it.packageName, known) },
        query, { it.label }, { it.packageName },
    )
    Column(modifier) {
        SearchField(query, enabled = !driving, onOpen = { typing = !typing }, onClear = { query = "" })
        if (driving) {
            Text(stringResource(R.string.phone_apps_driving_only), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.phoneApps.isEmpty() -> Placeholder(stringResource(R.string.phone_apps_loading), loading = true, icon = Icons.Rounded.Apps)
                shown.isEmpty() -> Placeholder(stringResource(R.string.phone_apps_none_found), loading = false, icon = Icons.Rounded.Search)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(shown, key = { it.id }) { app ->
                        Column(
                            Modifier.clip(RoundedCornerShape(20.dp)).carFocusHighlight(RoundedCornerShape(20.dp))
                                .clickable { typing = false; actions.openApp(app) }.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppIcon(app, 72)
                            Spacer(Modifier.height(8.dp))
                            Text(app.label, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        if (typing) {
            CarKeyboard(
                onText = { query += it },
                onBackspace = { query = query.dropLast(1) },
                onClear = { query = "" },
                onSearch = { typing = false },
                searchEnabled = true,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** The search box: shows the query (typed on the car keyboard below), or a hint. */
@Composable
private fun SearchField(query: String, enabled: Boolean, onOpen: () -> Unit, onClear: () -> Unit) {
    val d = LocalCarDimens.current
    Row(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp).fillMaxWidth().heightIn(min = 64.dp)
            .clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .carFocusHighlight(RoundedCornerShape(32.dp)).clickable(enabled = enabled, onClick = onOpen)
            .padding(start = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            query.ifEmpty { stringResource(if (enabled) R.string.phone_apps_search_hint else R.string.search_parked_only) },
            style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (query.isNotEmpty()) {
            Box(Modifier.size(d.touchTarget - 12.dp).clip(RoundedCornerShape(32.dp)).clickable(onClick = onClear), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, stringResource(R.string.phone_apps_clear_search), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            }
        }
    }
}
