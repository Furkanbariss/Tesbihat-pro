package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.SessionStore
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Sayımı en geç [WRITE_DELAY_MILLIS] içinde diske yazar. Gecikme her dokunuşta yeniden başlamaz:
 * yeniden başlasaydı, kesintisiz hızlı sayımda hiç yazım olmaz ve çökmede tüm seri kaybolurdu.
 *
 * [scope] uygulama ömürlü olmalıdır; ViewModel kapansa da bekleyen yazım tamamlanır.
 */
class SessionRepository(
    private val store: SessionStore,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val pending = AtomicReference<CounterState?>(null)
    private val writeRequests = Channel<Unit>(Channel.CONFLATED)
    private val writeLock = Mutex()
    private val _requestedSteps = MutableStateFlow<List<TesbihatStep>?>(null)

    /**
     * Zikirler ekranından seçilen ve sayacın henüz açmadığı adımlar. Değer tutulduğu için sayaç o
     * anda ayakta olmasa da seçim kaybolmaz.
     */
    val requestedSteps: StateFlow<List<TesbihatStep>?> = _requestedSteps.asStateFlow()

    init {
        scope.launch {
            for (request in writeRequests) {
                delay(WRITE_DELAY_MILLIS)
                flush()
            }
        }
    }

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
        writeRequests.trySend(Unit)
    }

    /** Bekleyen yazımı hemen yapar; uygulama arka plana geçerken çağrılır. */
    suspend fun flush() {
        writeLock.withLock {
            val state = pending.getAndSet(null) ?: return
            withContext(ioDispatcher) { store.write(state) }
        }
    }

    private companion object {
        const val WRITE_DELAY_MILLIS = 300L
    }
}
