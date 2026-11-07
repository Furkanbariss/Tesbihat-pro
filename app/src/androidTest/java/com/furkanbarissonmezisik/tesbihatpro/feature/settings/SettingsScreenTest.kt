package com.furkanbarissonmezisik.tesbihatpro.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ThemeMode
import com.furkanbarissonmezisik.tesbihatpro.core.ui.theme.TesbihatProTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var privacyClicks = 0

    private fun show(
        initial: AppSettings = AppSettings(),
        privacyOptionsRequired: Boolean = false,
    ): () -> AppSettings {
        var settings by mutableStateOf(initial)
        composeRule.setContent {
            TesbihatProTheme {
                SettingsScreen(
                    settings = settings,
                    versionName = "1.2",
                    privacyOptionsRequired = privacyOptionsRequired,
                    onBack = {},
                    onPrivacyOptions = { privacyClicks++ },
                    onSoundChange = { settings = settings.copy(soundEnabled = it) },
                    onHapticsChange = { settings = settings.copy(hapticsEnabled = it) },
                    onVolumeKeysChange = { settings = settings.copy(volumeKeysEnabled = it) },
                    onKeepScreenOnChange = { settings = settings.copy(keepScreenOn = it) },
                    onThemeChange = { settings = settings.copy(theme = it) },
                    onBeadStyleChange = { settings = settings.copy(beadStyleId = it) },
                    onAdsChange = { settings = settings.copy(adsEnabled = it) },
                )
            }
        }
        return { settings }
    }

    private fun row(id: Int) = composeRule.onNodeWithText(context.getString(id))

    /** Liste tembel çizilir; aşağıdaki satırlar kaydırılmadan ağaçta yoktur. */
    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    private fun scrollTo(id: Int) = scrollTo(context.getString(id))

    @Test
    fun switchRowReflectsAndChangesItsSetting() {
        val settings = show()

        row(R.string.settings_sound).assertIsOff()
        row(R.string.settings_sound).performClick()

        row(R.string.settings_sound).assertIsOn()
        assertEquals(true, settings().soundEnabled)
    }

    @Test
    fun switchRowsAreIndependent() {
        val settings = show(AppSettings(soundEnabled = true))

        row(R.string.settings_vibration).performClick()

        row(R.string.settings_sound).assertIsOn()
        assertEquals(false, settings().hapticsEnabled)
    }

    @Test
    fun volumeKeysRowHasItsHint() {
        show(AppSettings(volumeKeysEnabled = false))

        row(R.string.settings_volume_keys).assertIsOff()
        row(R.string.settings_volume_keys_hint).performScrollTo()
    }

    @Test
    fun themeIsChosenFromThreeOptions() {
        val settings = show()

        row(R.string.settings_theme_system).assertIsSelected()
        row(R.string.settings_theme_dark).performClick()

        row(R.string.settings_theme_dark).assertIsSelected()
        assertEquals(ThemeMode.DARK, settings().theme)
    }

    @Test
    fun beadStyleIsChosenFromTheStyleList() {
        val settings = show()

        row(R.string.bead_style_amber).assertIsSelected()
        row(R.string.bead_style_dark_wood).performClick()

        assertEquals("dark_wood", settings().beadStyleId)
    }

    @Test
    fun unknownBeadStyleShowsTheDefaultAsSelected() {
        show(AppSettings(beadStyleId = "removed_model"))

        row(R.string.bead_style_amber).assertIsSelected()
    }

    @Test
    fun privacyEntryIsHiddenWhenNotRequired() {
        show()

        scrollTo(R.string.settings_privacy_policy)
        row(R.string.settings_privacy_options).assertDoesNotExist()
    }

    @Test
    fun privacyEntryOpensTheFormWhenRequired() {
        show(privacyOptionsRequired = true)

        scrollTo(R.string.settings_privacy_options)
        row(R.string.settings_privacy_options).performClick()

        assertEquals(1, privacyClicks)
    }

    @Test
    fun adsAreOnUntilTurnedOff() {
        val settings = show()

        scrollTo(R.string.settings_ads)
        row(R.string.settings_ads).assertIsOn()
        row(R.string.settings_ads).performClick()

        row(R.string.settings_ads).assertIsOff()
        assertEquals(false, settings().adsEnabled)
    }

    @Test
    fun privacyEntryIsHiddenWhileAdsAreOff() {
        show(AppSettings(adsEnabled = false), privacyOptionsRequired = true)

        scrollTo(R.string.settings_privacy_policy)
        row(R.string.settings_ads).assertIsOff()
        row(R.string.settings_privacy_options).assertDoesNotExist()
    }

    @Test
    fun privacyPolicyCanBeReadInsideTheApp() {
        show()

        scrollTo(R.string.settings_privacy_policy)
        row(R.string.settings_privacy_policy).performClick()

        val text = context.getString(R.string.privacy_policy_text)
        composeRule.onNodeWithText(text).assertExists()

        composeRule.onNodeWithText(context.getString(R.string.action_close)).performClick()
        composeRule.onNodeWithText(text).assertDoesNotExist()
    }

    @Test
    fun aboutShowsTheVersion() {
        show()

        val version = context.getString(R.string.settings_about_version, "1.2")
        scrollTo(version)
        composeRule.onNodeWithText(version).assertExists()
    }
}
