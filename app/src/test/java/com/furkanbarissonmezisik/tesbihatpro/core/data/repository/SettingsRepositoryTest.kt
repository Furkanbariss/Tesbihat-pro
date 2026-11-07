package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ThemeMode
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeSettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @Test
    fun `defaults start silent and keep the other features on`() {
        assertEquals(
            AppSettings(
                soundEnabled = false,
                hapticsEnabled = true,
                volumeKeysEnabled = true,
                keepScreenOn = true,
                theme = ThemeMode.SYSTEM,
                beadStyleId = "amber",
                adsEnabled = true,
            ),
            AppSettings(),
        )
    }

    @Test
    fun `stored settings replace the defaults once read`() = runTest {
        val stored = AppSettings(soundEnabled = true, theme = ThemeMode.DARK)
        val repository = SettingsRepository(FakeSettingsStore(stored), backgroundScope)

        runCurrent()

        assertEquals(stored, repository.settings.value)
    }

    @Test
    fun `awaitLoaded waits for the first read`() = runTest {
        val stored = AppSettings(theme = ThemeMode.LIGHT)
        val repository = SettingsRepository(FakeSettingsStore(stored), backgroundScope)

        val waiting = async { repository.awaitLoaded() }
        assertFalse(waiting.isCompleted)
        runCurrent()

        assertTrue(waiting.isCompleted)
        assertEquals(ThemeMode.LIGHT, repository.settings.value.theme)
    }

    @Test
    fun `update reaches the store and the live value`() = runTest {
        val store = FakeSettingsStore()
        val repository = SettingsRepository(store, backgroundScope)
        runCurrent()

        repository.update { it.copy(hapticsEnabled = false) }
        runCurrent()

        assertFalse(store.current.hapticsEnabled)
        assertFalse(repository.settings.value.hapticsEnabled)
    }

    @Test
    fun `quick updates to different settings do not overwrite each other`() = runTest {
        val store = FakeSettingsStore()
        val repository = SettingsRepository(store, backgroundScope)
        runCurrent()

        repository.update { it.copy(soundEnabled = true) }
        repository.update { it.copy(keepScreenOn = false) }
        runCurrent()

        assertTrue(store.current.soundEnabled)
        assertFalse(store.current.keepScreenOn)
    }
}
