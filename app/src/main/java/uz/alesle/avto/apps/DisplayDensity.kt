package uz.alesle.avto.apps

/**
 * The density of the phone-apps display: at most the car's own, but low enough that the display's
 * smallest width is at least [LARGE_SCREEN_DP]. Android then treats it as a large screen: apps
 * lay out their tablet UIs, and from Android 16 apps that ask for portrait or a fixed size are made
 * resizable there instead of being letterboxed. Never below [MIN_DPI], where car text gets too
 * small to read; a display that small stays a phone-sized one.
 */
object DisplayDensity {
    const val LARGE_SCREEN_DP = 600
    const val MIN_DPI = 120

    fun pick(width: Int, height: Int, baseDpi: Int): Int {
        // smallestScreenWidthDp = minSide * 160 / dpi, rounded down: this keeps it ≥ 600.
        val fit = minOf(width, height) * 160 / LARGE_SCREEN_DP
        return minOf(baseDpi, fit).coerceAtLeast(MIN_DPI)
    }
}
