package net.prototux.aare.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayTouchTest {
    @Test
    fun sameSizeIsOneToOne() {
        val m = DisplayTouch(1184, 720, 1184, 720)
        assertTrue(m.identity)
        assertEquals(100.5f, m.x(100.5f), 0f)
        assertEquals(719f, m.y(719f), 0f)
    }

    @Test
    fun scalesWhileAResizeIsPending() {
        // the content area grew (rail moved to the bottom) before the display followed
        val m = DisplayTouch(1280, 624, 1184, 720)
        assertFalse(m.identity)
        assertEquals(592f, m.x(640f), 0.01f)
        assertEquals(360f, m.y(312f), 0.01f)
    }

    @Test
    fun clampsToTheDisplay() {
        val m = DisplayTouch(800, 480, 1600, 960)
        assertEquals(0f, m.x(-20f), 0f)
        assertEquals(1599f, m.x(900f), 0f)
        assertEquals(959f, m.y(10_000f), 0f)
    }

    @Test
    fun emptyViewDoesntDivideByZero() {
        val m = DisplayTouch(0, 0, 0, 0)
        assertEquals(0f, m.x(5f), 0f)
        assertEquals(0f, m.y(5f), 0f)
    }
}
