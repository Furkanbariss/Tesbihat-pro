package com.furkanbarissonmezisik.tesbihatpro.core.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface AdsController {
    /** Onay alınıp SDK hazır olana kadar false kalır; false iken reklam isteği yapılmaz. */
    val canShowAds: StateFlow<Boolean>

    /** Ayarlarda "Gizlilik seçenekleri" girişinin gösterilip gösterilmeyeceği. */
    val privacyOptionsRequired: StateFlow<Boolean>

    /**
     * Onayı toplar ve SDK'yı başlatır. Yalnız reklamlı ekranlardan çağrılır: Tesbih ve Zikirler
     * ibadet sırasında açılır, oralarda reklam kodu çalışmaz ve onay formu çıkmaz.
     */
    fun prepare(activity: Activity)

    fun showPrivacyOptions(activity: Activity)
}
