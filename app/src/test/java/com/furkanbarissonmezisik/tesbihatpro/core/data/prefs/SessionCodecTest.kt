package com.furkanbarissonmezisik.tesbihatpro.core.data.prefs

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCodecTest {

    private val steps = listOf(
        TesbihatStep(zikirId = 1, target = 33),
        TesbihatStep(zikirId = 7, target = 1),
        TesbihatStep(zikirId = 1000, target = null),
    )

    @Test
    fun `steps are encoded as id and target pairs`() {
        assertEquals("1:33,7:1,1000:", SessionCodec.encodeSteps(steps))
    }

    @Test
    fun `state survives an encode and decode round trip`() {
        val state = CounterState(steps, stepIndex = 2, count = 12, completed = false)

        val decoded = SessionCodec.decode(
            steps = SessionCodec.encodeSteps(state.steps),
            stepIndex = state.stepIndex,
            count = state.count,
            completed = state.completed,
        )

        assertEquals(state, decoded)
    }

    @Test
    fun `completed flag survives decoding`() {
        val decoded = SessionCodec.decode("1:33", stepIndex = 0, count = 33, completed = true)

        assertEquals(
            CounterState(listOf(TesbihatStep(1, 33)), count = 33, completed = true),
            decoded,
        )
    }

    @Test
    fun `malformed steps decode to null`() {
        listOf("", "1", "abc:33", "1:abc", "1:33,", ":33", "1:2:3").forEach { encoded ->
            assertNull(encoded, SessionCodec.decode(encoded, 0, 0, false))
        }
    }

    @Test
    fun `out of range position decodes to null`() {
        assertNull(SessionCodec.decode("1:33", stepIndex = 1, count = 0, completed = false))
        assertNull(SessionCodec.decode("1:33", stepIndex = -1, count = 0, completed = false))
        assertNull(SessionCodec.decode("1:33", stepIndex = 0, count = -1, completed = false))
    }
}
