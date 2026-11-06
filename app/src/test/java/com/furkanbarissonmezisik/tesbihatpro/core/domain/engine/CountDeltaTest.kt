package com.furkanbarissonmezisik.tesbihatpro.core.domain.engine

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountDeltaTest {

    private val steps = listOf(
        TesbihatStep(zikirId = 1, target = 33),
        TesbihatStep(zikirId = 2, target = 33),
        TesbihatStep(zikirId = 3, target = 1),
    )

    private fun deltaOf(event: CounterEvent, state: CounterState): CountDelta? =
        countDelta(event, state, CounterEngine.reduce(state, event).state)

    @Test
    fun `tap adds one to the current zikir`() {
        assertEquals(
            CountDelta(zikirId = 1, amount = 1),
            deltaOf(CounterEvent.Tap, CounterState(steps, count = 5)),
        )
    }

    @Test
    fun `tap that finishes a step counts for the finished zikir, not the next one`() {
        assertEquals(
            CountDelta(zikirId = 1, amount = 1),
            deltaOf(CounterEvent.Tap, CounterState(steps, count = 32)),
        )
    }

    @Test
    fun `tap that completes the sequence counts for the last zikir`() {
        assertEquals(
            CountDelta(zikirId = 3, amount = 1),
            deltaOf(CounterEvent.Tap, CounterState(steps, stepIndex = 2)),
        )
    }

    @Test
    fun `tap while completed counts for nothing`() {
        val completed = CounterState(steps, stepIndex = 2, count = 1, completed = true)

        assertNull(deltaOf(CounterEvent.Tap, completed))
    }

    @Test
    fun `undo removes one from the current zikir`() {
        assertEquals(
            CountDelta(zikirId = 2, amount = -1),
            deltaOf(CounterEvent.Undo, CounterState(steps, stepIndex = 1, count = 5)),
        )
    }

    @Test
    fun `undo at a step boundary removes from the previous zikir`() {
        assertEquals(
            CountDelta(zikirId = 1, amount = -1),
            deltaOf(CounterEvent.Undo, CounterState(steps, stepIndex = 1, count = 0)),
        )
    }

    @Test
    fun `undo while completed removes from the last zikir`() {
        val completed = CounterState(steps, stepIndex = 2, count = 1, completed = true)

        assertEquals(CountDelta(zikirId = 3, amount = -1), deltaOf(CounterEvent.Undo, completed))
    }

    @Test
    fun `undo with nothing to undo counts for nothing`() {
        assertNull(deltaOf(CounterEvent.Undo, CounterState(steps)))
    }

    @Test
    fun `resets leave the history alone`() {
        val state = CounterState(steps, stepIndex = 1, count = 12)

        assertNull(deltaOf(CounterEvent.ResetStep, state))
        assertNull(deltaOf(CounterEvent.ResetAll, state))
    }

    @Test
    fun `tapping and undoing a whole tesbihat nets to zero per zikir`() {
        var state = CounterState(steps)
        val totals = mutableMapOf<Long, Int>()
        val events = List(67) { CounterEvent.Tap } + List(67) { CounterEvent.Undo }
        events.forEach { event ->
            val next = CounterEngine.reduce(state, event).state
            countDelta(event, state, next)?.let { delta ->
                totals[delta.zikirId] = (totals[delta.zikirId] ?: 0) + delta.amount
            }
            state = next
        }

        assertEquals(mapOf(1L to 0, 2L to 0, 3L to 0), totals)
    }
}
