package com.furkanbarissonmezisik.tesbihatpro.core.domain.engine

sealed interface CounterEvent {
    data object Tap : CounterEvent
    data object Undo : CounterEvent
    data object ResetStep : CounterEvent
    data object ResetAll : CounterEvent
}
