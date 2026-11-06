package com.furkanbarissonmezisik.tesbihatpro.core.content

import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEffect
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEngine
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEvent
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetContentTest {

    @Test
    fun `preset ids are the fixed values stored in saved sessions`() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L), PresetContent.zikirs.map { it.id })
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
            listOf(33, 33, 33, 100, 100, 100, 1),
            PresetContent.zikirs.map { it.defaultTarget },
        )
    }

    @Test
    fun `default tesbihat is 33 tesbih tahmid tekbir then one tehlil`() {
        assertEquals(
            listOf(
                TesbihatStep(PresetContent.SUBHANALLAH_ID, target = 33),
                TesbihatStep(PresetContent.ELHAMDULILLAH_ID, target = 33),
                TesbihatStep(PresetContent.ALLAHU_EKBER_ID, target = 33),
                TesbihatStep(PresetContent.TEHLIL_ID, target = 1),
            ),
            PresetContent.defaultTesbihat,
        )
    }

    @Test
    fun `default tesbihat adds up to one hundred`() {
        assertEquals(100, PresetContent.defaultTesbihat.sumOf { it.target ?: 0 })
    }

    @Test
    fun `default tesbihat reaches tehlil after 99 taps and completes on the 100th`() {
        val initial = CounterState(PresetContent.defaultTesbihat)

        val afterNinetyNine = (1..99).fold(initial) { state, _ ->
            CounterEngine.reduce(state, CounterEvent.Tap).state
        }
        val finished = CounterEngine.reduce(afterNinetyNine, CounterEvent.Tap)

        assertEquals(3, afterNinetyNine.stepIndex)
        assertFalse(afterNinetyNine.completed)
        assertTrue(finished.state.completed)
        assertEquals(CounterEffect.SequenceCompleted, finished.effect)
    }
}
