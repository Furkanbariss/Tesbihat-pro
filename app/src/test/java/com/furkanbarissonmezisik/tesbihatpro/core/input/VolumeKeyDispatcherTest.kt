package com.furkanbarissonmezisik.tesbihatpro.core.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeKeyDispatcherTest {

    private class Recorder : VolumeKeyDispatcher.Listener {
        var ups = 0
        var downs = 0

        override fun onVolumeUp() {
            ups++
        }

        override fun onVolumeDown() {
            downs++
        }
    }

    private val up = KeyEvent.KEYCODE_VOLUME_UP
    private val down = KeyEvent.KEYCODE_VOLUME_DOWN
    private val press = KeyEvent.ACTION_DOWN
    private val release = KeyEvent.ACTION_UP

    private val dispatcher = VolumeKeyDispatcher()
    private val recorder = Recorder()

    @Test
    fun `without a listener the event stays with the system`() {
        assertFalse(dispatcher.onKeyEvent(up, press, repeatCount = 0))
        assertFalse(dispatcher.onKeyEvent(down, press, repeatCount = 0))
    }

    @Test
    fun `volume up goes to the listener and is consumed`() {
        dispatcher.register(recorder)

        assertTrue(dispatcher.onKeyEvent(up, press, repeatCount = 0))
        assertEquals(1, recorder.ups)
        assertEquals(0, recorder.downs)
    }

    @Test
    fun `volume down goes to the listener and is consumed`() {
        dispatcher.register(recorder)

        assertTrue(dispatcher.onKeyEvent(down, press, repeatCount = 0))
        assertEquals(1, recorder.downs)
        assertEquals(0, recorder.ups)
    }

    @Test
    fun `key release is consumed but does not count again`() {
        dispatcher.register(recorder)
        dispatcher.onKeyEvent(up, press, repeatCount = 0)

        assertTrue(dispatcher.onKeyEvent(up, release, repeatCount = 0))
        assertEquals(1, recorder.ups)
    }

    @Test
    fun `holding the key counts once but keeps the volume unchanged`() {
        dispatcher.register(recorder)
        dispatcher.onKeyEvent(up, press, repeatCount = 0)

        repeat(20) { index ->
            assertTrue(dispatcher.onKeyEvent(up, press, repeatCount = index + 1))
        }
        assertEquals(1, recorder.ups)
    }

    @Test
    fun `other keys stay with the system`() {
        dispatcher.register(recorder)

        assertFalse(dispatcher.onKeyEvent(KeyEvent.KEYCODE_BACK, press, repeatCount = 0))
        assertFalse(dispatcher.onKeyEvent(KeyEvent.KEYCODE_VOLUME_MUTE, press, repeatCount = 0))
    }

    @Test
    fun `after unregistering the event stays with the system`() {
        dispatcher.register(recorder)
        dispatcher.unregister(recorder)

        assertFalse(dispatcher.onKeyEvent(up, press, repeatCount = 0))
        assertEquals(0, recorder.ups)
    }

    @Test
    fun `unregistering a stale listener keeps the current one`() {
        val stale = Recorder()
        dispatcher.register(stale)
        dispatcher.register(recorder)

        dispatcher.unregister(stale)

        assertTrue(dispatcher.onKeyEvent(up, press, repeatCount = 0))
        assertEquals(1, recorder.ups)
    }
}
