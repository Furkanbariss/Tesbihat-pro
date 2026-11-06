package com.furkanbarissonmezisik.tesbihatpro.feature.settings

import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.SettingsRepository
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ThemeMode
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeSettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val store = FakeSettingsStore()

    private fun TestScope.viewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(SettingsRepository(store, backgroundScope))
        runCurrent()
        return viewModel
    }

    @Test
    fun `each control changes only its own setting`() = runTest {
        val viewModel = viewModel()
        val changes: List<Pair<() -> Unit, AppSettings>> = listOf(
            { viewModel.onSoundChange(false) } to AppSettings(soundEnabled = false),
            { viewModel.onHapticsChange(false) } to AppSettings(hapticsEnabled = false),
            { viewModel.onVolumeKeysChange(false) } to AppSettings(volumeKeysEnabled = false),
            { viewModel.onKeepScreenOnChange(false) } to AppSettings(keepScreenOn = false),
            { viewModel.onThemeChange(ThemeMode.DARK) } to AppSettings(theme = ThemeMode.DARK),
            { viewModel.onBeadStyleChange("dark_wood") } to AppSettings(beadStyleId = "dark_wood"),
        )

        changes.forEach { (change, expected) ->
            store.update { AppSettings() }
            runCurrent()

            change()
            runCurrent()

            assertEquals(expected, store.current)
        }
    }

    @Test
    fun `screen follows the stored settings`() = runTest {
        val viewModel = viewModel()

        store.update { it.copy(theme = ThemeMode.LIGHT) }
        runCurrent()

        assertEquals(ThemeMode.LIGHT, viewModel.settings.value.theme)
    }
}
