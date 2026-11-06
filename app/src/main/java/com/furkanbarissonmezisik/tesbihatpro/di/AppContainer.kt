package com.furkanbarissonmezisik.tesbihatpro.di

import android.content.Context
import com.furkanbarissonmezisik.tesbihatpro.core.ads.AdsController
import com.furkanbarissonmezisik.tesbihatpro.core.ads.NoOpAdsController
import com.furkanbarissonmezisik.tesbihatpro.core.billing.FakePremiumRepository
import com.furkanbarissonmezisik.tesbihatpro.core.billing.PremiumRepository
import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.DataStoreSessionStore
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.SessionRepository
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.HapticsController
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.SoundController
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.SoundPoolController
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.VibratorHapticsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Tüm repository ve servislerin tek kaynağı. */
class AppContainer(context: Context) {
    /** Ekran kapansa da bitmesi gereken işler (bekleyen disk yazımları) için. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val adsController: AdsController = NoOpAdsController
    val premiumRepository: PremiumRepository = FakePremiumRepository()

    val sessionRepository = SessionRepository(DataStoreSessionStore(context), applicationScope)
    val hapticsController: HapticsController = VibratorHapticsController(context)
    val soundController: SoundController = SoundPoolController(context)

    fun flushPendingWrites() {
        applicationScope.launch { sessionRepository.flush() }
    }
}
