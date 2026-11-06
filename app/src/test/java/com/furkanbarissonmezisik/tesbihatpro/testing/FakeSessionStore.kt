package com.furkanbarissonmezisik.tesbihatpro.testing

import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.SessionStore
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState

class FakeSessionStore(var stored: CounterState? = null) : SessionStore {
    val writes = mutableListOf<CounterState>()

    override suspend fun read(): CounterState? = stored

    override suspend fun write(state: CounterState) {
        writes += state
        stored = state
    }
}
