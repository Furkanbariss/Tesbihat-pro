package com.furkanbarissonmezisik.tesbihatpro.core.domain.engine

/**
 * Bir olay en çok bir efekt üretir; birden çoğu geçerliyse en güçlüsü döner:
 * [SequenceCompleted] > [StepCompleted] > [Milestone] > [Tick].
 */
sealed interface CounterEffect {
    data object Tick : CounterEffect
    data class StepCompleted(val stepIndex: Int) : CounterEffect
    data object SequenceCompleted : CounterEffect
    data object Milestone : CounterEffect
}
