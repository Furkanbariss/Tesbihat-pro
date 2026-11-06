package com.furkanbarissonmezisik.tesbihatpro.core.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Faz 9'a kadar kullanılır; Play Billing kurulmadan uygulama eksiksiz çalışır. */
class FakePremiumRepository : PremiumRepository {
    override val isPremium: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
}
