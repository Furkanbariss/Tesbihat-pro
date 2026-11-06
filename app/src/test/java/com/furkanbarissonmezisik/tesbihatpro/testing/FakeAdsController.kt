package com.furkanbarissonmezisik.tesbihatpro.testing

import android.app.Activity
import com.furkanbarissonmezisik.tesbihatpro.core.ads.AdsController
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAdsController : AdsController {
    override val canShowAds = MutableStateFlow(false)
    override val privacyOptionsRequired = MutableStateFlow(false)

    override fun prepare(activity: Activity) = Unit

    override fun showPrivacyOptions(activity: Activity) = Unit
}
