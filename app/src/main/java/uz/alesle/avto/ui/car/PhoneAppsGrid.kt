package uz.alesle.avto.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.alesle.avto.R
import uz.alesle.avto.apps.PhoneApps
import uz.alesle.avto.media.CAR_KEYBOARD_MIN_HEIGHT
import uz.alesle.avto.media.CarKeyboard
import uz.alesle.avto.media.Placeholder
import uz.alesle.avto.ui.theme.LocalCarDimens
import uz.alesle.avto.ui.theme.WALLPAPER_CHROME_ALPHA
import uz.alesle.avto.ui.theme.WallpaperLabel
import uz.alesle.avto.ui.theme.WallpaperTile

/** The phone apps to list: all of them parked, navigation and music while driving, filtered by the search. */
private fun matches(state: CarUiState): List<CarAppEntry> {
    val driving = state.drivingRestricted
    val known = state.apps.filter { it.kind == CarAppKind.NAVIGATION || it.kind == CarAppKind.MEDIA }.mapTo(HashSet()) { it.packageName }
    return PhoneApps.filter(
        state.phoneApps.filter { !driving || PhoneApps.drivingAllowed(it.packageName, known) },
        state.phoneAppsQuery, { it.label }, { it.packageName },
    )
}

/**
 * The launcher's "Phone apps" tab: every app of the phone's own launcher, run as itself on the
 * car screen. Tapping the search field opens [PhoneAppsSearch] (parked only, like media search);
 * a query kept from it filters the grid. While driving only navigation and music apps are
 * listed, as the phone screen mirror allows.
 */
@Composable
internal fun PhoneAppsGrid(state: CarUiState, actions: CarActions, modifier: Modifier, onWallpaper: Boolean = false) {
    val driving = state.drivingRestricted
    val shown = matches(state)
    Column(modifier) {
        SearchField(
            state.phoneAppsQuery, enabled = !driving, onWallpaper,
            onOpen = { state.phoneAppsSearching = true }, onClear = { state.phoneAppsQuery = "" },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp).heightIn(min = 64.dp),
        )
        if (driving) {
            Text(stringResource(R.string.phone_apps_driving_only), style = MaterialTheme.typography.bodyMedium,
                color = if (onWallpaper) WallpaperLabel else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
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
                            Modifier.launcherTile(onWallpaper, RoundedCornerShape(20.dp)).carFocusHighlight(RoundedCornerShape(20.dp))
                                .clickable { actions.openApp(app) }.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppIcon(app, 72)
                            Spacer(Modifier.height(8.dp))
                            Text(app.label, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                color = if (onWallpaper) WallpaperLabel else Color.Unspecified)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Searching the phone apps, in the whole launcher area: on top the field (with close and clear),
 * under it one scrolling row of matches updated as you type, and the keyboard below in the rest.
 * The keyboard never gets less than its readable minimum; on a short screen the row gives way
 * first. The search key goes back to the grid, keeping the filter; close or Back drops it.
 */
@Composable
internal fun PhoneAppsSearch(state: CarUiState, actions: CarActions, onWallpaper: Boolean, modifier: Modifier) {
    val driving = state.drivingRestricted
    LaunchedEffect(driving) { if (driving) state.closePhoneAppsSearch() }
    val shown = matches(state)
    BoxWithConstraints(modifier) {
        val spare = maxHeight - FIELD_TOP - FIELD_HEIGHT - (CAR_KEYBOARD_MIN_HEIGHT + KEYBOARD_PAD * 2)
        val resultsHeight = spare.coerceIn(0.dp, RESULTS_MAX)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.padding(start = 12.dp, end = 24.dp, top = FIELD_TOP).height(FIELD_HEIGHT).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(FIELD_HEIGHT).clip(RoundedCornerShape(FIELD_HEIGHT / 2)).carFocusHighlight(RoundedCornerShape(FIELD_HEIGHT / 2))
                        .clickable { state.closePhoneAppsSearch() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back),
                        tint = if (onWallpaper) WallpaperLabel else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
                }
                SearchField(
                    state.phoneAppsQuery, enabled = true, onWallpaper, editing = true,
                    onOpen = {}, onClear = { state.phoneAppsQuery = "" },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            if (resultsHeight >= RESULTS_MIN) ResultsRow(state, shown, resultsHeight, onWallpaper) { actions.openApp(it) }
            CarKeyboard(
                onText = { state.phoneAppsQuery += it },
                onBackspace = { state.phoneAppsQuery = state.phoneAppsQuery.dropLast(1) },
                onClear = { state.phoneAppsQuery = "" },
                onSearch = { state.phoneAppsSearching = false },
                searchEnabled = true,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = KEYBOARD_PAD),
            )
        }
    }
}

/** The matching apps in one row, as icon-and-label chips filling [height]. */
@Composable
private fun ResultsRow(state: CarUiState, shown: List<CarAppEntry>, height: Dp, onWallpaper: Boolean, open: (CarAppEntry) -> Unit) {
    val chip = height - 8.dp
    val label = if (onWallpaper) WallpaperLabel else MaterialTheme.colorScheme.onSurface
    if (shown.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(height).padding(horizontal = 28.dp), contentAlignment = Alignment.CenterStart) {
            Text(stringResource(if (state.phoneApps.isEmpty()) R.string.phone_apps_loading else R.string.phone_apps_none_found),
                style = MaterialTheme.typography.bodyLarge, color = label, maxLines = 1)
        }
        return
    }
    LazyRow(
        Modifier.fillMaxWidth().height(height),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(shown, key = { it.id }) { app ->
            Row(
                Modifier.height(chip).widthIn(min = 64.dp, max = 260.dp).clip(RoundedCornerShape(chip / 2))
                    .background(if (onWallpaper) WallpaperTile else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .carFocusHighlight(RoundedCornerShape(chip / 2)).clickable { open(app) }
                    .padding(start = 6.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app, (chip - 12.dp).value.toInt().coerceIn(24, 48))
                Spacer(Modifier.width(10.dp))
                Text(app.label, style = MaterialTheme.typography.labelLarge, color = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private val FIELD_TOP = 6.dp
private val FIELD_HEIGHT = 52.dp
private val KEYBOARD_PAD = 4.dp
private val RESULTS_MIN = 36.dp
private val RESULTS_MAX = 88.dp

/** The search box: shows the query (typed on the car keyboard), or a hint. */
@Composable
private fun SearchField(
    query: String,
    enabled: Boolean,
    onWallpaper: Boolean,
    editing: Boolean = false,
    onOpen: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
) {
    val d = LocalCarDimens.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(scheme.surfaceContainerHigh.let { if (onWallpaper) it.copy(alpha = WALLPAPER_CHROME_ALPHA) else it })
            .carFocusHighlight(RoundedCornerShape(32.dp)).clickable(enabled = enabled && !editing, onClick = onOpen)
            .padding(start = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            query.ifEmpty { stringResource(if (enabled) R.string.phone_apps_search_hint else R.string.search_parked_only) },
            style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (query.isEmpty()) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (query.isNotEmpty()) {
            Box(Modifier.size(d.touchTarget - 24.dp).clip(RoundedCornerShape(32.dp)).clickable(onClick = onClear), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, stringResource(R.string.phone_apps_clear_search), tint = scheme.onSurface, modifier = Modifier.size(28.dp))
            }
        }
    }
}
