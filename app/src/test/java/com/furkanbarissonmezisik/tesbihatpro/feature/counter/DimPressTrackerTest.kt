package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DimPressTrackerTest {

    private val tracker = DimPressTracker(exitHoldMillis = 1_000)

    @Test
    fun `short press counts when released`() {
        tracker.press(pointer = 1, time = 100)

        assertTrue(tracker.release(pointer = 1, time = 180))
    }

    @Test
    fun `press just under the limit still counts`() {
        tracker.press(pointer = 1, time = 0)

        assertTrue(tracker.release(pointer = 1, time = 999))
    }

    @Test
    fun `press at the limit does not count`() {
        tracker.press(pointer = 1, time = 0)

        assertFalse(tracker.release(pointer = 1, time = 1_000))
    }

    @Test
    fun `release of an unknown pointer does not count`() {
        assertFalse(tracker.release(pointer = 1, time = 100))
    }

    @Test
    fun `overlapping fingers count once when the last one lifts`() {
        tracker.press(pointer = 1, time = 0)
        tracker.press(pointer = 2, time = 50)

        assertFalse(tracker.release(pointer = 1, time = 100))
        assertTrue(tracker.release(pointer = 2, time = 150))
    }

    @Test
    fun `three fingers at once count once`() {
        tracker.press(pointer = 1, time = 0)
        tracker.press(pointer = 2, time = 10)
        tracker.press(pointer = 3, time = 20)

        val counted = listOf(
            tracker.release(pointer = 1, time = 120),
            tracker.release(pointer = 2, time = 125),
            tracker.release(pointer = 3, time = 130),
        )

        assertEquals(listOf(false, false, true), counted)
    }

    @Test
    fun `a new press counts again once every finger has lifted`() {
        tracker.press(pointer = 1, time = 0)
        tracker.release(pointer = 1, time = 100)

        tracker.press(pointer = 2, time = 200)

        assertTrue(tracker.release(pointer = 2, time = 300))
    }

    @Test
    fun `a press that lasts past the limit across fingers does not count`() {
        tracker.press(pointer = 1, time = 0)
        tracker.press(pointer = 2, time = 600)
        tracker.release(pointer = 1, time = 700)

        assertFalse(tracker.release(pointer = 2, time = 1_100))
    }

    @Test
    fun `nothing is waited for without a press`() {
        assertNull(tracker.millisUntilExit(now = 500))
    }

    @Test
    fun `exit waits for the remaining hold time`() {
        tracker.press(pointer = 1, time = 100)

        assertEquals(1_000L, tracker.millisUntilExit(now = 100))
        assertEquals(400L, tracker.millisUntilExit(now = 700))
    }

    @Test
    fun `first finger of the press decides the exit time`() {
        tracker.press(pointer = 1, time = 0)
        tracker.press(pointer = 2, time = 600)

        assertEquals(300L, tracker.millisUntilExit(now = 700))
    }

    @Test
    fun `exit time stays with the first finger after it lifts`() {
        tracker.press(pointer = 1, time = 0)
        tracker.press(pointer = 2, time = 600)
        tracker.release(pointer = 1, time = 650)

        assertEquals(300L, tracker.millisUntilExit(now = 700))
    }

    @Test
    fun `exit time never goes negative`() {
        tracker.press(pointer = 1, time = 0)

        assertEquals(0L, tracker.millisUntilExit(now = 5_000))
    }

    @Test
    fun `reset forgets presses so a held finger neither counts nor exits again`() {
        tracker.press(pointer = 1, time = 0)

        tracker.reset()

        assertNull(tracker.millisUntilExit(now = 5_000))
        assertFalse(tracker.release(pointer = 1, time = 100))
    }
}
