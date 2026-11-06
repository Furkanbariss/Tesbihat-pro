package com.furkanbarissonmezisik.tesbihatpro.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Faz 8'e kadar kullanılır; AdMob kurulmadan uygulama eksiksiz çalışır. */
object NoOpAdsController : AdsController {
    override val canShowAds: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
}
