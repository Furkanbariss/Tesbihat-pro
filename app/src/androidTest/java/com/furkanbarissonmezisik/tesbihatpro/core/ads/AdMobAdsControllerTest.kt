package com.furkanbarissonmezisik.tesbihatpro.core.ads

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdMobAdsControllerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeConsent : ConsentManager {
        override var canRequestAds = false
        override var privacyOptionsRequired = false
        var gatherCalls = 0
        var finishGather: () -> Unit = {}
        var finishPrivacyForm: () -> Unit = {}

        override fun gatherConsent(activity: Activity, onFinished: () -> Unit) {
            gatherCalls++
            finishGather = onFinished
        }

        override fun showPrivacyOptions(activity: Activity, onFinished: () -> Unit) {
            finishPrivacyForm = onFinished
        }
    }

    private val consent = FakeConsent()
    private var sdkStarts = 0
    private var finishSdkStart: () -> Unit = {}
    private lateinit var activity: Activity
    private lateinit var controller: AdMobAdsController

    @Before
    fun setUp() {
        activity = composeRule.activity
        controller = AdMobAdsController(consent) { onReady ->
            sdkStarts++
            finishSdkStart = onReady
        }
    }

    @Test
    fun nothingRunsUntilAnAdScreenAsks() {
        assertEquals(0, consent.gatherCalls)
        assertEquals(0, sdkStarts)
        assertFalse(controller.canShowAds.value)
    }

    @Test
    fun sdkWaitsForConsent() {
        controller.prepare(activity)

        assertEquals(1, consent.gatherCalls)
        assertEquals(0, sdkStarts)
        assertFalse(controller.canShowAds.value)
    }

    @Test
    fun adsWaitForTheSdkAfterConsent() {
        consent.canRequestAds = true
        controller.prepare(activity)
        consent.finishGather()

        assertEquals(1, sdkStarts)
        assertFalse(controller.canShowAds.value)

        finishSdkStart()
        assertTrue(controller.canShowAds.value)
    }

    @Test
    fun withoutConsentTheSdkNeverStarts() {
        controller.prepare(activity)
        consent.finishGather()

        assertEquals(0, sdkStarts)
        assertFalse(controller.canShowAds.value)
    }

    @Test
    fun enteringASecondAdScreenDoesNotStartAgain() {
        consent.canRequestAds = true
        controller.prepare(activity)
        consent.finishGather()
        finishSdkStart()

        controller.prepare(activity)

        assertEquals(1, consent.gatherCalls)
        assertEquals(1, sdkStarts)
    }

    @Test
    fun consentIsNotAskedTwiceWhileAFormIsOpen() {
        controller.prepare(activity)
        controller.prepare(activity)

        assertEquals(1, consent.gatherCalls)
    }

    @Test
    fun failedConsentIsRetriedOnTheNextAdScreen() {
        controller.prepare(activity)
        consent.finishGather()

        consent.canRequestAds = true
        controller.prepare(activity)
        consent.finishGather()

        assertEquals(2, consent.gatherCalls)
        assertEquals(1, sdkStarts)
    }

    @Test
    fun privacyEntryFollowsTheConsentResult() {
        consent.privacyOptionsRequired = true
        controller.prepare(activity)
        assertFalse(controller.privacyOptionsRequired.value)

        consent.finishGather()
        assertTrue(controller.privacyOptionsRequired.value)
    }

    @Test
    fun withdrawingConsentHidesAdsAndGivingItBackShowsThem() {
        consent.canRequestAds = true
        controller.prepare(activity)
        consent.finishGather()
        finishSdkStart()

        controller.showPrivacyOptions(activity)
        consent.canRequestAds = false
        consent.finishPrivacyForm()
        assertFalse(controller.canShowAds.value)

        controller.showPrivacyOptions(activity)
        consent.canRequestAds = true
        consent.finishPrivacyForm()
        assertTrue(controller.canShowAds.value)
        assertEquals(1, sdkStarts)
    }

    @Test
    fun consentWithdrawnDuringSdkStartKeepsAdsHidden() {
        consent.canRequestAds = true
        controller.prepare(activity)
        consent.finishGather()

        consent.canRequestAds = false
        finishSdkStart()

        assertFalse(controller.canShowAds.value)
    }
}
