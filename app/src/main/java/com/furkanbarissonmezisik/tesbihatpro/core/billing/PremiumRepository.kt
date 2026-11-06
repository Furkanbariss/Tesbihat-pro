package com.furkanbarissonmezisik.tesbihatpro.core.billing

import kotlinx.coroutines.flow.StateFlow

interface PremiumRepository {
    val isPremium: StateFlow<Boolean>
}
