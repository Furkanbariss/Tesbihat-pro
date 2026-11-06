package com.furkanbarissonmezisik.tesbihatpro.core.domain.engine

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterEngineTest {

    private val tesbihat = listOf(
        TesbihatStep(zikirId = 1, target = 33),
        TesbihatStep(zikirId = 2, target = 33),
        TesbihatStep(zikirId = 3, target = 33),
    )
    private val unlimited = listOf(TesbihatStep(zikirId = 1, target = null))
    private val single = listOf(TesbihatStep(zikirId = 4, target = 100))

    private fun CounterState.send(event: CounterEvent) = CounterEngine.reduce(this, event)

    private fun CounterState.tapped(times: Int): CounterState =
        (1..times).fold(this) { state, _ -> state.send(CounterEvent.Tap).state }

    // Tap

    @Test
    fun `tap increments count and returns tick`() {
        val result = CounterState(tesbihat).send(CounterEvent.Tap)

        assertEquals(CounterState(tesbihat, count = 1), result.state)
        assertEquals(CounterEffect.Tick, result.effect)
    }

    @Test
    fun `tap one below target stays on the step with tick`() {
        val result = CounterState(tesbihat, count = 31).send(CounterEvent.Tap)

        assertEquals(CounterState(tesbihat, count = 32), result.state)
        assertEquals(CounterEffect.Tick, result.effect)
    }

    @Test
    fun `tap reaching target moves to next step and returns only step completed`() {
        val result = CounterState(tesbihat, count = 32).send(CounterEvent.Tap)

        assertEquals(CounterState(tesbihat, stepIndex = 1, count = 0), result.state)
        assertEquals(CounterEffect.StepCompleted(stepIndex = 0), result.effect)
    }

    @Test
    fun `step completed carries the index of the finished step`() {
        val result = CounterState(tesbihat, stepIndex = 1, count = 32).send(CounterEvent.Tap)

        assertEquals(CounterState(tesbihat, stepIndex = 2, count = 0), result.state)
        assertEquals(CounterEffect.StepCompleted(stepIndex = 1), result.effect)
    }

    @Test
    fun `tap reaching last target completes and returns only sequence completed`() {
        val result = CounterState(tesbihat, stepIndex = 2, count = 32).send(CounterEvent.Tap)

        assertEquals(
            CounterState(tesbihat, stepIndex = 2, count = 33, completed = true),
            result.state,
        )
        assertEquals(CounterEffect.SequenceCompleted, result.effect)
    }

    @Test
    fun `single step zikir completes at its target`() {
        val result = CounterState(single, count = 99).send(CounterEvent.Tap)

        assertEquals(CounterState(single, count = 100, completed = true), result.state)
        assertEquals(CounterEffect.SequenceCompleted, result.effect)
    }

    @Test
    fun `step with target of one completes on first tap`() {
        val steps = listOf(TesbihatStep(zikirId = 1, target = 1), TesbihatStep(zikirId = 2, target = 1))

        val first = CounterState(steps).send(CounterEvent.Tap)
        val second = first.state.send(CounterEvent.Tap)

        assertEquals(CounterEffect.StepCompleted(stepIndex = 0), first.effect)
        assertEquals(CounterEffect.SequenceCompleted, second.effect)
    }

    @Test
    fun `full tesbihat emits two step completions then sequence completed`() {
        var state = CounterState(tesbihat)
        val effects = (1..99).map { _ ->
            val result = state.send(CounterEvent.Tap)
            state = result.state
            result.effect
        }

        assertEquals(CounterEffect.StepCompleted(0), effects[32])
        assertEquals(CounterEffect.StepCompleted(1), effects[65])
        assertEquals(CounterEffect.SequenceCompleted, effects[98])
        assertEquals(96, effects.count { it == CounterEffect.Tick })
        assertTrue(state.completed)
    }

    @Test
    fun `tap while completed is ignored without effect`() {
        val completed = CounterState(tesbihat, stepIndex = 2, count = 33, completed = true)

        val result = completed.send(CounterEvent.Tap)

        assertSame(completed, result.state)
        assertNull(result.effect)
    }

    // Sınırsız sayım

    @Test
    fun `unlimited step returns milestone at 33 and 66 and tick otherwise`() {
        var state = CounterState(unlimited)
        val effects = (1..67).map { _ ->
            val result = state.send(CounterEvent.Tap)
            state = result.state
            result.effect
        }

        assertEquals(CounterEffect.Tick, effects[31])
        assertEquals(CounterEffect.Milestone, effects[32])
        assertEquals(CounterEffect.Tick, effects[33])
        assertEquals(CounterEffect.Milestone, effects[65])
        assertEquals(CounterEffect.Tick, effects[66])
        assertEquals(2, effects.count { it == CounterEffect.Milestone })
    }

    @Test
    fun `unlimited step never completes`() {
        val state = CounterState(unlimited).tapped(500)

        assertEquals(500, state.count)
        assertFalse(state.completed)
    }

    @Test
    fun `step with target gives no milestone at 33`() {
        val result = CounterState(single, count = 32).send(CounterEvent.Tap)

        assertEquals(CounterEffect.Tick, result.effect)
    }

    // Undo

    @Test
    fun `undo decrements count without effect`() {
        val result = CounterState(tesbihat, count = 5).send(CounterEvent.Undo)

        assertEquals(CounterState(tesbihat, count = 4), result.state)
        assertNull(result.effect)
    }

    @Test
    fun `undo at zero on first step changes nothing`() {
        val result = CounterState(tesbihat).send(CounterEvent.Undo)

        assertEquals(CounterState(tesbihat), result.state)
        assertNull(result.effect)
    }

    @Test
    fun `undo at step boundary returns to previous step at target minus one`() {
        val result = CounterState(tesbihat, stepIndex = 1, count = 0).send(CounterEvent.Undo)

        assertEquals(CounterState(tesbihat, stepIndex = 0, count = 32), result.state)
        assertNull(result.effect)
    }

    @Test
    fun `undo at boundary uses the target of the previous step`() {
        val steps = listOf(TesbihatStep(zikirId = 1, target = 10), TesbihatStep(zikirId = 2, target = 33))

        val result = CounterState(steps, stepIndex = 1).send(CounterEvent.Undo)

        assertEquals(CounterState(steps, stepIndex = 0, count = 9), result.state)
    }

    @Test
    fun `undo while completed returns to last step at target minus one`() {
        val completed = CounterState(tesbihat, stepIndex = 2, count = 33, completed = true)

        val result = completed.send(CounterEvent.Undo)

        assertEquals(CounterState(tesbihat, stepIndex = 2, count = 32), result.state)
        assertNull(result.effect)
    }

    @Test
    fun `tap after undo while completed completes again`() {
        val completed = CounterState(tesbihat, stepIndex = 2, count = 33, completed = true)

        val result = completed.send(CounterEvent.Undo).state.send(CounterEvent.Tap)

        assertEquals(completed, result.state)
        assertEquals(CounterEffect.SequenceCompleted, result.effect)
    }

    @Test
    fun `undo decrements on unlimited step`() {
        val result = CounterState(unlimited, count = 34).send(CounterEvent.Undo)

        assertEquals(CounterState(unlimited, count = 33), result.state)
        assertNull(result.effect)
    }

    // Sıfırlama

    @Test
    fun `reset step clears count and keeps the step`() {
        val result = CounterState(tesbihat, stepIndex = 1, count = 12).send(CounterEvent.ResetStep)

        assertEquals(CounterState(tesbihat, stepIndex = 1, count = 0), result.state)
        assertNull(result.effect)
    }

    @Test
    fun `reset step while completed reopens the last step so taps count again`() {
        val completed = CounterState(tesbihat, stepIndex = 2, count = 33, completed = true)

        val reset = completed.send(CounterEvent.ResetStep)
        val tapped = reset.state.send(CounterEvent.Tap)

        assertEquals(CounterState(tesbihat, stepIndex = 2, count = 0), reset.state)
        assertNull(reset.effect)
        assertEquals(1, tapped.state.count)
        assertEquals(CounterEffect.Tick, tapped.effect)
    }

    @Test
    fun `reset all returns to first step`() {
        val result = CounterState(tesbihat, stepIndex = 2, count = 7).send(CounterEvent.ResetAll)

        assertEquals(CounterState(tesbihat), result.state)
        assertNull(result.effect)
    }

    @Test
    fun `reset all clears completed`() {
        val completed = CounterState(tesbihat, stepIndex = 2, count = 33, completed = true)

        val result = completed.send(CounterEvent.ResetAll)

        assertEquals(CounterState(tesbihat), result.state)
        assertNull(result.effect)
    }

    // Uzun seriler

    @Test
    fun `1000 taps on unlimited step stay consistent`() {
        var state = CounterState(unlimited)
        var milestones = 0
        repeat(1_000) {
            val result = state.send(CounterEvent.Tap)
            state = result.state
            if (result.effect == CounterEffect.Milestone) milestones++
        }

        assertEquals(CounterState(unlimited, count = 1_000), state)
        assertEquals(30, milestones)
    }

    @Test
    fun `1000 taps on tesbihat stop at completion`() {
        val state = CounterState(tesbihat).tapped(1_000)

        assertEquals(CounterState(tesbihat, stepIndex = 2, count = 33, completed = true), state)
    }

    @Test
    fun `undoing every tap of a full tesbihat returns to the start`() {
        val finished = CounterState(tesbihat).tapped(99)

        val state = (1..99).fold(finished) { current, _ -> current.send(CounterEvent.Undo).state }

        assertEquals(CounterState(tesbihat), state)
    }
}
