package com.furkanbarissonmezisik.tesbihatpro.core.domain.engine

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState

data class CountDelta(val zikirId: Long, val amount: Int)

/**
 * Bir olayın günlük toplama etkisi. Adımı bitiren `Tap` sonrası durum artık sonraki adımdadır, bu
 * yüzden sayım [before]'daki zikre yazılır; adım sınırındaki `Undo` ise [after]'daki zikirden düşer.
 * Sıfırlama geçmişi değiştirmez.
 */
fun countDelta(event: CounterEvent, before: CounterState, after: CounterState): CountDelta? {
    if (before == after) return null
    return when (event) {
        CounterEvent.Tap -> CountDelta(before.steps[before.stepIndex].zikirId, amount = 1)
        CounterEvent.Undo -> CountDelta(after.steps[after.stepIndex].zikirId, amount = -1)
        CounterEvent.ResetStep, CounterEvent.ResetAll -> null
    }
}
