package com.furkanbarissonmezisik.tesbihatpro.core.content

import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEngine
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEvent
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetContentTest {

    @Test
    fun `preset ids are the fixed values stored in saved sessions`() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), PresetContent.zikirs.map { it.id })
    }

    @Test
    fun `preset ids stay below the range reserved for user zikirs`() {
        assertTrue(PresetContent.zikirs.all { it.id in 1 until 1000 })
    }

    @Test
    fun `every zikir is marked as preset and has arabic and turkish text`() {
        PresetContent.zikirs.forEach { zikir ->
            assertTrue(zikir.isPreset)
            assertFalse(zikir.arabic.isNullOrBlank())
            assertTrue(zikir.transliterationTr.isNotBlank())
            assertFalse(zikir.meaningTr.isNullOrBlank())
        }
    }

    @Test
    fun `default targets match the content table`() {
        assertEquals(
            listOf(33, 33, 33, 100, 100, 100),
            PresetContent.zikirs.map { it.defaultTarget },
        )
    }

    @Test
    fun `default tesbihat is the first three zikirs with 33 each`() {
        val steps = PresetContent.defaultTesbihat

        assertEquals(PresetContent.zikirs.take(3).map { it.id }, steps.map { it.zikirId })
        assertTrue(steps.all { it.target == 33 })
    }

    @Test
    fun `default tesbihat completes after 99 taps`() {
        val initial = CounterState(PresetContent.defaultTesbihat)

        val afterNinetyEight = (1..98).fold(initial) { state, _ ->
            CounterEngine.reduce(state, CounterEvent.Tap).state
        }
        val finished = CounterEngine.reduce(afterNinetyEight, CounterEvent.Tap).state

        assertFalse(afterNinetyEight.completed)
        assertTrue(finished.completed)
    }
}
