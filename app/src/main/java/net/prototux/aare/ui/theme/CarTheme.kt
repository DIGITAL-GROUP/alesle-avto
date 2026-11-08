package net.prototux.aare.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R

/**
 * The in-car look: near-black graphite surfaces, one signal-orange accent, big type and big targets.
 *
 * Car screens are viewed from ~70 cm at a glance, so sizes follow the car-UI rule of thumb:
 * touch targets ≥ 76 dp, body text ≥ 24 sp, and no more than two text styles per card.
 */
@Immutable
data class CarDimens(
    val touchTarget: Dp = 76.dp,
    val railWidth: Dp = 96.dp,
    val gutter: Dp = 12.dp,
    val cardRadius: Dp = 20.dp,
    val iconSize: Dp = 36.dp,
)

val LocalCarDimens = staticCompositionLocalOf { CarDimens() }

/** Brand accent. At night it sits on near-black (5.9:1); by day it is deepened to stay ≥ 4.5:1 on light grey. */
private val Signal = Color(0xFFFF4F00)
private val SignalDeep = Color(0xFFC43D00)

/** Night surfaces, darkest to lightest: page, card, raised card. */
private val Ink = Color(0xFF0B0D10)
private val Graphite = Color(0xFF15181D)
private val GraphiteHigh = Color(0xFF1E2228)

/** Behind phone apps on their car display, day and night, and around an app that is letterboxed there. */
val AppDisplayBackground = Ink

private val NightColors: ColorScheme = darkColorScheme(
    primary = Signal,
    // White on #FF4F00 is only 3.3:1; near-black is 5.9:1, which reads better at a glance.
    onPrimary = Ink,
    primaryContainer = Color(0xFF3A1A0A),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFB8BEC6),
    onSecondary = Color(0xFF23272D),
    secondaryContainer = GraphiteHigh,
    onSecondaryContainer = Color(0xFFE4E7EB),
    tertiary = Color(0xFFFFB547),
    onTertiary = Color(0xFF2B1700),
    background = Ink,
    onBackground = Color(0xFFF2F3F5),
    surface = Ink,
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = GraphiteHigh,
    onSurfaceVariant = Color(0xFFA3A9B3),
    surfaceContainerLowest = Color(0xFF07080A),
    surfaceContainerLow = Color(0xFF111317),
    surfaceContainer = Graphite,
    surfaceContainerHigh = GraphiteHigh,
    surfaceContainerHighest = Color(0xFF272C33),
    outline = Color(0xFF5B636D),
    outlineVariant = Color(0xFF2A2F36),
    error = Color(0xFFFF8A80),
)

private val DayColors: ColorScheme = lightColorScheme(
    primary = SignalDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A1000),
    secondary = Color(0xFF50575F),
    secondaryContainer = Color(0xFFE1E4E8),
    onSecondaryContainer = Graphite,
    tertiary = Color(0xFF8B5000),
    background = Color(0xFFF4F5F7),
    onBackground = Graphite,
    surface = Color(0xFFF4F5F7),
    onSurface = Graphite,
    surfaceVariant = Color(0xFFE1E4E8),
    onSurfaceVariant = Color(0xFF444A52),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF0F2),
    surfaceContainer = Color(0xFFE8EAED),
    surfaceContainerHigh = Color(0xFFE2E5E8),
    surfaceContainerHighest = Color(0xFFDCDFE3),
    outline = Color(0xFF737A84),
    outlineVariant = Color(0xFFC3C8CE),
)

/**
 * Geist (SIL OFL 1.1, see docs/licenses/Geist-OFL.txt), one variable file for every weight.
 * It has no U+02BB (Uzbek oʻ/gʻ); Android falls back to the system font for that glyph only.
 */
private fun geist(weight: FontWeight) =
    Font(R.font.geist, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Geist = FontFamily(
    geist(FontWeight.Normal),
    geist(FontWeight.Medium),
    geist(FontWeight.SemiBold),
    geist(FontWeight.Bold),
)

private fun TextStyle.geist() = copy(fontFamily = Geist)

/** Applies [Geist] to every style, including the ones the car scale leaves at Material defaults. */
private fun Typography.withGeist() = Typography(
    displayLarge = displayLarge.geist(), displayMedium = displayMedium.geist(), displaySmall = displaySmall.geist(),
    headlineLarge = headlineLarge.geist(), headlineMedium = headlineMedium.geist(), headlineSmall = headlineSmall.geist(),
    titleLarge = titleLarge.geist(), titleMedium = titleMedium.geist(), titleSmall = titleSmall.geist(),
    bodyLarge = bodyLarge.geist(), bodyMedium = bodyMedium.geist(), bodySmall = bodySmall.geist(),
    labelLarge = labelLarge.geist(), labelMedium = labelMedium.geist(), labelSmall = labelSmall.geist(),
)

private val CarType = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.Medium),
    headlineMedium = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium),
    headlineSmall = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontSize = 22.sp, lineHeight = 30.sp),
    labelLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
).withGeist()

private val CarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CarTheme(night: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (night) NightColors else DayColors,
        typography = CarType,
        shapes = CarShapes,
        content = content,
    )
}

/** Phone UI theme: same palette, normal phone type scale. */
@Composable
fun PhoneTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) NightColors else DayColors, content = content)
}
