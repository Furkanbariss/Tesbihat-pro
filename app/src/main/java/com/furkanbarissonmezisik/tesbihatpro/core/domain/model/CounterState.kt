package com.furkanbarissonmezisik.tesbihatpro.core.domain.model

data class CounterState(
    /** Tek zikir, tek adımlı listedir. */
    val steps: List<TesbihatStep>,
    val stepIndex: Int = 0,
    val count: Int = 0,
    val completed: Boolean = false,
)
