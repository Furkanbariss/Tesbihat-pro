package com.furkanbarissonmezisik.tesbihatpro.core.ui.components

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.furkanbarissonmezisik.tesbihatpro.core.ads.AdsController
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Banner gerçek reklam istemesin diye [AdsController.canShowAds] hep false tutulur; burada yalnız
 * onay ve SDK hazırlığının kullanıcının seçimine uyduğu denenir.
 */
@RunWith(AndroidJUnit4::class)
class BannerAdTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class RecordingAdsController : AdsController {
        override val canShowAds = MutableStateFlow(false)
        override val privacyOptionsRequired = MutableStateFlow(false)
        var prepareCalls = 0

        override fun prepare(activity: Activity) {
            prepareCalls++
        }

        override fun showPrivacyOptions(activity: Activity) = Unit
    }

    private val ads = RecordingAdsController()

    @Test
    fun adsTurnedOffNeverAskForConsentOrStartTheSdk() {
        composeRule.setContent { BannerAd(ads, enabled = false) }
        composeRule.waitForIdle()

        assertEquals(0, ads.prepareCalls)
    }

    @Test
    fun adsTurnedOnPrepareOnce() {
        composeRule.setContent { BannerAd(ads, enabled = true) }
        composeRule.waitForIdle()

        assertEquals(1, ads.prepareCalls)
    }

    @Test
    fun turningAdsBackOnPreparesThen() {
        var enabled by mutableStateOf(false)
        composeRule.setContent { BannerAd(ads, enabled = enabled) }
        composeRule.waitForIdle()
        assertEquals(0, ads.prepareCalls)

        enabled = true
        composeRule.waitForIdle()

        assertEquals(1, ads.prepareCalls)
    }
}
