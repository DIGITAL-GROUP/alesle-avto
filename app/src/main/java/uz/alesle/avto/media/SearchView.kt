package uz.alesle.avto.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.alesle.avto.R
import uz.alesle.avto.ui.car.carFocusHighlight

/**
 * Search entry. The car screen is a virtual display where the phone's IME does not appear, so
 * we draw our own keyboard; like Android Auto, it is locked while driving.
 */
@Composable
internal fun SearchView(ui: MediaUi, driving: Boolean) {
    if (driving) {
        Placeholder(stringResource(R.string.search_parked_only), loading = false, icon = Icons.Rounded.Search)
        return
    }
    CarKeyboard(
        onText = { ui.query += it },
        onBackspace = { ui.query = ui.query.dropLast(1) },
        onClear = { ui.query = "" },
        onSearch = { ui.search(ui.query) },
        searchEnabled = ui.query.isNotBlank(),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private val ROWS = listOf("1234567890", "qwertyuiop", "asdfghjkl'", "zxcvbnm-")

/**
 * A big-key QWERTY keyboard whose rows share the available height: keys from [MIN_KEY]
 * to [MAX_KEY] tall. When five rows of [MIN_KEY] don't fit (a phone held sideways) the digit row
 * goes; below four rows it still keeps [MIN_KEY], so the caller must give it at least
 * [CAR_KEYBOARD_MIN_HEIGHT]. Readable keys come before anything placed around it.
 */
@Composable
internal fun CarKeyboard(
    onText: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    searchEnabled: Boolean,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier) {
        val bounded = constraints.hasBoundedHeight
        val digits = !bounded || maxHeight >= MIN_KEY * 5 + GAP * 4
        val rows = if (digits) ROWS else ROWS.drop(1)
        val n = rows.size + 1
        val key = if (bounded) ((maxHeight - GAP * (n - 1)) / n).coerceIn(MIN_KEY, MAX_KEY) else 64.dp
        Column(Modifier.fillMaxWidth().align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(GAP)) {
            rows.forEachIndexed { r, keys ->
                KeyRow(key) {
                    keys.forEach { c -> Key(c.uppercase(), Modifier.weight(1f)) { onText(c.toString()) } }
                    if (r == rows.lastIndex) Key(null, Modifier.weight(2f), Icons.AutoMirrored.Rounded.Backspace, onClick = onBackspace)
                }
            }
            KeyRow(key) {
                Key(null, Modifier.weight(1.5f), Icons.Rounded.Clear, onClick = onClear)
                Key(null, Modifier.weight(5f), Icons.Rounded.SpaceBar) { onText(" ") }
                Key(stringResource(R.string.media_action_search), Modifier.weight(3f), Icons.Rounded.Search, primary = true, enabled = searchEnabled, onClick = onSearch)
            }
        }
    }
}

private val MIN_KEY = 44.dp
private val MAX_KEY = 76.dp
private val GAP = 6.dp

/** The least height a [CarKeyboard] needs: four rows (letters and the space bar) of 44 dp keys. */
internal val CAR_KEYBOARD_MIN_HEIGHT = MIN_KEY * 4 + GAP * 3

@Composable
private fun KeyRow(height: Dp, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun Key(
    label: String?,
    modifier: Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (primary && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (primary && enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier.fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(bg)
            .carFocusHighlight(RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, label, tint = fg, modifier = Modifier.size(30.dp))
            if (label != null) {
                if (icon != null) Box(Modifier.size(8.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, color = fg)
            }
        }
    }
}
