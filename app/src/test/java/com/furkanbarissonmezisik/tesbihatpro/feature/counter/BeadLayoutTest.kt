package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEngine
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEvent
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeadLayoutTest {

    private val tesbihat = PresetContent.defaultTesbihat
    private val unlimited = listOf(TesbihatStep(zikirId = 1, target = null))

    @Test
    fun `position equals count on the first step`() {
        assertEquals(12, beadPosition(CounterState(tesbihat, count = 12)))
    }

    @Test
    fun `position adds the targets of finished steps`() {
        assertEquals(33 + 33 + 5, beadPosition(CounterState(tesbihat, stepIndex = 2, count = 5)))
    }

    @Test
    fun `position on an unlimited step is the count`() {
        assertEquals(500, beadPosition(CounterState(unlimited, count = 500)))
    }

    @Test
    fun `every tap of a full tesbihat moves the position by exactly one`() {
        var state = CounterState(tesbihat)
        repeat(100) { index ->
            state = CounterEngine.reduce(state, CounterEvent.Tap).state
            assertEquals(index + 1, beadPosition(state))
        }
        assertTrue(state.completed)
    }

    @Test
    fun `every undo of a full tesbihat moves the position back by exactly one`() {
        var state = (1..100).fold(CounterState(tesbihat)) { current, _ ->
            CounterEngine.reduce(current, CounterEvent.Tap).state
        }
        repeat(100) { index ->
            state = CounterEngine.reduce(state, CounterEvent.Undo).state
            assertEquals(99 - index, beadPosition(state))
        }
    }

    @Test
    fun `imames sit at each cumulative step target`() {
        assertEquals(listOf(33, 66, 99, 100), imameBeads(tesbihat))
    }

    @Test
    fun `unlimited step has no imame and never ends`() {
        assertEquals(emptyList<Int>(), imameBeads(unlimited))
        assertNull(lastBead(unlimited))
    }

    @Test
    fun `string ends at the last imame`() {
        assertEquals(100, lastBead(tesbihat))
    }

    @Test
    fun `string with a final unlimited step does not end`() {
        val steps = listOf(TesbihatStep(1, 33), TesbihatStep(2, null))

        assertEquals(listOf(33), imameBeads(steps))
        assertNull(lastBead(steps))
    }

    @Test
    fun `visible beads surround the position`() {
        assertEquals(5..15, visibleBeads(position = 10f, beadsPerSide = 5, lastBead = null))
    }

    @Test
    fun `visible beads cover both neighbours while animating`() {
        assertEquals(5..16, visibleBeads(position = 10.4f, beadsPerSide = 5, lastBead = null))
    }

    @Test
    fun `visible beads stop at the end of the string`() {
        assertEquals(93..100, visibleBeads(position = 98f, beadsPerSide = 5, lastBead = 100))
    }
}
