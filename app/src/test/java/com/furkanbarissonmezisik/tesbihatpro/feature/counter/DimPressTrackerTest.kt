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
    fun `two fingers count separately even when they overlap`() {
        tracker.press(pointer = 1, time = 0)
        tracker.press(pointer = 2, time = 50)

        assertTrue(tracker.release(pointer = 1, time = 100))
        assertTrue(tracker.release(pointer = 2, time = 150))
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
    fun `oldest press decides the exit time`() {
        tracker.press(pointer = 1, time = 0)
        tracker.press(pointer = 2, time = 600)

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
