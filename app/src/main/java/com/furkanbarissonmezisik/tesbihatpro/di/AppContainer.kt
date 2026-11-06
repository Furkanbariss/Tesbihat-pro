package com.furkanbarissonmezisik.tesbihatpro.di

import com.furkanbarissonmezisik.tesbihatpro.core.ads.AdsController
import com.furkanbarissonmezisik.tesbihatpro.core.ads.NoOpAdsController
import com.furkanbarissonmezisik.tesbihatpro.core.billing.FakePremiumRepository
import com.furkanbarissonmezisik.tesbihatpro.core.billing.PremiumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Tüm repository ve servislerin tek kaynağı. */
class AppContainer {
    /** Ekran kapansa da bitmesi gereken işler (bekleyen disk yazımları) için. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val adsController: AdsController = NoOpAdsController
    val premiumRepository: PremiumRepository = FakePremiumRepository()
}
