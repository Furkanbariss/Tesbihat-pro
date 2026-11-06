package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeSessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {

    private val steps = listOf(TesbihatStep(zikirId = 1, target = 33))
    private val store = FakeSessionStore()

    private fun state(count: Int) = CounterState(steps, count = count)

    // backgroundScope gecikmeleri advanceUntilIdle ile ilerlemez, zaman elle ilerletilir.
    private fun TestScope.passWriteDelay() {
        advanceTimeBy(301)
        runCurrent()
    }

    private fun TestScope.repository() = SessionRepository(
        store = store,
        scope = backgroundScope,
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `nothing is written before the delay elapses`() = runTest {
        val repository = repository()

        repository.save(state(1))
        advanceTimeBy(299)
        runCurrent()

        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `state is written once the delay elapses`() = runTest {
        val repository = repository()

        repository.save(state(1))
        passWriteDelay()

        assertEquals(listOf(state(1)), store.writes)
    }

    @Test
    fun `saves inside one window collapse into a single write of the latest state`() = runTest {
        val repository = repository()

        repository.save(state(1))
        repository.save(state(2))
        repository.save(state(3))
        passWriteDelay()

        assertEquals(listOf(state(3)), store.writes)
    }

    @Test
    fun `continuous fast tapping keeps reaching the disk`() = runTest {
        val repository = repository()

        // Saniyede 8 dokunuş, 5 saniye boyunca hiç ara verilmez.
        repeat(40) { index ->
            repository.save(state(index + 1))
            advanceTimeBy(125)
            runCurrent()
            val unwritten = index + 1 - (store.writes.lastOrNull()?.count ?: 0)
            assertTrue("Yazılmamış dokunuş: $unwritten", unwritten <= 3)
        }
        passWriteDelay()

        assertEquals(state(40), store.writes.last())
        assertTrue(store.writes.size in 10..20)
    }

    @Test
    fun `flush writes the pending state immediately`() = runTest {
        val repository = repository()

        repository.save(state(7))
        repository.flush()

        assertEquals(listOf(state(7)), store.writes)
    }

    @Test
    fun `flush leaves nothing for the delayed write`() = runTest {
        val repository = repository()

        repository.save(state(7))
        repository.flush()
        passWriteDelay()

        assertEquals(listOf(state(7)), store.writes)
    }

    @Test
    fun `flush without a pending state writes nothing`() = runTest {
        val repository = repository()

        repository.flush()

        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `saves after a flush are written again`() = runTest {
        val repository = repository()

        repository.save(state(1))
        repository.flush()
        repository.save(state(2))
        passWriteDelay()

        assertEquals(listOf(state(1), state(2)), store.writes)
    }

    @Test
    fun `load returns what the store holds`() = runTest {
        store.stored = state(12)

        assertEquals(state(12), repository().load())
    }
}
