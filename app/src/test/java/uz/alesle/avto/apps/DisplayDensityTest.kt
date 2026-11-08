package uz.alesle.avto.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayDensityTest {
    private fun smallestWidthDp(w: Int, h: Int, dpi: Int) = minOf(w, h) * 160 / dpi

    @Test fun phonePreviewIsLoweredToALargeScreen() {
        // The preview's content area on a 520 dpi phone held sideways.
        val dpi = DisplayDensity.pick(2344, 1005, 520)
        assertEquals(268, dpi)
        assertTrue(smallestWidthDp(2344, 1005, dpi) >= 600)
    }

    @Test fun carDensityIsKeptWhenAlreadyLargeEnough() {
        assertEquals(160, DisplayDensity.pick(1824, 1080, 160))
    }

    @Test fun portraitContentAreaUsesItsWidth() {
        val dpi = DisplayDensity.pick(1200, 1800, 320)
        assertEquals(320, dpi)
        assertTrue(smallestWidthDp(1200, 1800, dpi) >= 600)
    }

    @Test fun tinyAreaStaysReadable() {
        assertEquals(DisplayDensity.MIN_DPI, DisplayDensity.pick(640, 360, 240))
    }
}
