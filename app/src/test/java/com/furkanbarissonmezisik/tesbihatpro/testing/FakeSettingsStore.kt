package com.furkanbarissonmezisik.tesbihatpro.testing

import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.SettingsStore
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private val stored = MutableStateFlow(initial)

    val current: AppSettings get() = stored.value

    override val settings: Flow<AppSettings> = stored

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        stored.value = transform(stored.value)
    }
}
