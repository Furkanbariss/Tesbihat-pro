package com.furkanbarissonmezisik.tesbihatpro.core.domain.engine

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState

data class Transition(val state: CounterState, val effect: CounterEffect?)

object CounterEngine {
    private const val MILESTONE_INTERVAL = 33

    fun reduce(state: CounterState, event: CounterEvent): Transition = when (event) {
        CounterEvent.Tap -> tap(state)
        CounterEvent.Undo -> Transition(undo(state), effect = null)
        CounterEvent.ResetStep -> Transition(
            state.copy(count = 0, completed = false),
            effect = null,
        )
        CounterEvent.ResetAll -> Transition(
            state.copy(stepIndex = 0, count = 0, completed = false),
            effect = null,
        )
    }

    private fun tap(state: CounterState): Transition {
        if (state.completed) return Transition(state, effect = null)

        val count = state.count + 1
        val target = state.steps[state.stepIndex].target
        return when {
            target == null -> Transition(
                state.copy(count = count),
                if (count % MILESTONE_INTERVAL == 0) CounterEffect.Milestone else CounterEffect.Tick,
            )
            count < target -> Transition(state.copy(count = count), CounterEffect.Tick)
            state.stepIndex < state.steps.lastIndex -> Transition(
                state.copy(stepIndex = state.stepIndex + 1, count = 0),
                CounterEffect.StepCompleted(state.stepIndex),
            )
            else -> Transition(
                state.copy(count = count, completed = true),
                CounterEffect.SequenceCompleted,
            )
        }
    }

    private fun undo(state: CounterState): CounterState = when {
        state.completed -> state.copy(
            count = countBeforeTarget(state, state.stepIndex),
            completed = false,
        )
        state.count > 0 -> state.copy(count = state.count - 1)
        state.stepIndex > 0 -> state.copy(
            stepIndex = state.stepIndex - 1,
            count = countBeforeTarget(state, state.stepIndex - 1),
        )
        else -> state
    }

    /** Sınırsız adım tamamlanamadığı için geri dönülen adımın hedefi her zaman vardır. */
    private fun countBeforeTarget(state: CounterState, stepIndex: Int): Int =
        (state.steps[stepIndex].target ?: 1) - 1
}
