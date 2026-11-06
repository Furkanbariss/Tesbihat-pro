package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bekleyen veriyi en geç [DELAY_MILLIS] içinde yazar. Gecikme her istekte yeniden başlamaz: yeniden
 * başlasaydı, kesintisiz hızlı sayımda hiç yazım olmaz ve çökmede tüm seri kaybolurdu.
 *
 * [scope] uygulama ömürlü olmalıdır; ViewModel kapansa da bekleyen yazım tamamlanır.
 */
internal class DelayedWrites(scope: CoroutineScope, write: suspend () -> Unit) {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                delay(DELAY_MILLIS)
                write()
            }
        }
    }

    fun request() {
        requests.trySend(Unit)
    }

    private companion object {
        const val DELAY_MILLIS = 300L
    }
}
