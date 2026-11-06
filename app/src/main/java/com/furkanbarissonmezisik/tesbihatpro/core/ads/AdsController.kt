package com.furkanbarissonmezisik.tesbihatpro.core.ads

import kotlinx.coroutines.flow.StateFlow

interface AdsController {
    /** Onay alınmadan veya premium kullanıcıda false kalır; false iken reklam isteği yapılmaz. */
    val canShowAds: StateFlow<Boolean>
}
