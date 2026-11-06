package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.SettingsStore
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Ayarlar bellekte durur: her dokunuşta okunurlar, diski beklemek olmaz. */
class SettingsRepository(
    private val store: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val loaded = CompletableDeferred<Unit>()

    init {
        scope.launch {
            store.settings.collect { stored ->
                _settings.value = stored
                loaded.complete(Unit)
            }
        }
    }

    /** İlk okuma bitene kadar bekler; tema ilk karede doğru çizilsin diye açılışta çağrılır. */
    suspend fun awaitLoaded() = loaded.await()

    fun update(transform: (AppSettings) -> AppSettings) {
        scope.launch { store.update(transform) }
    }
}
