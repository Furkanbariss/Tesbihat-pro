package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.SessionStore
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Sayımı gecikmeli yazar; ayrıntı için [DelayedWrites]. */
class SessionRepository(
    private val store: SessionStore,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val pending = AtomicReference<CounterState?>(null)
    private val writeLock = Mutex()
    private val delayedWrites = DelayedWrites(scope, ::flush)
    private val _requestedSteps = MutableStateFlow<List<TesbihatStep>?>(null)

    /**
     * Zikirler ekranından seçilen ve sayacın henüz açmadığı adımlar. Değer tutulduğu için sayaç o
     * anda ayakta olmasa da seçim kaybolmaz.
     */
    val requestedSteps: StateFlow<List<TesbihatStep>?> = _requestedSteps.asStateFlow()

    suspend fun load(): CounterState? {
        flush()
        return withContext(ioDispatcher) { store.read() }
    }

    fun requestSteps(steps: List<TesbihatStep>) {
        _requestedSteps.value = steps
    }

    fun clearRequestedSteps() {
        _requestedSteps.value = null
    }

    fun save(state: CounterState) {
        pending.set(state)
        delayedWrites.request()
    }

    /** Bekleyen yazımı hemen yapar; uygulama arka plana geçerken çağrılır. */
    suspend fun flush() {
        writeLock.withLock {
            val state = pending.getAndSet(null) ?: return
            withContext(ioDispatcher) { store.write(state) }
        }
    }
}
