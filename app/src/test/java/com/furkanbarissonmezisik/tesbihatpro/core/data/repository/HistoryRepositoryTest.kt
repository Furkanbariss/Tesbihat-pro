package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.DailyCount
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeDailyCountDao
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryTest {

    private val dao = FakeDailyCountDao()
    private var today = LocalDate.of(2027, 2, 8)

    private fun TestScope.repository() = HistoryRepository(
        dao = dao,
        scope = backgroundScope,
        ioDispatcher = StandardTestDispatcher(testScheduler),
        today = { today },
    )

    private fun TestScope.passWriteDelay() {
        advanceTimeBy(301)
        runCurrent()
    }

    @Test
    fun `nothing is written before the delay elapses`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 1)
        advanceTimeBy(299)
        runCurrent()

        assertNull(dao.count("2027-02-08", 1))
    }

    @Test
    fun `delta is written once the delay elapses`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 1)
        passWriteDelay()

        assertEquals(1, dao.count("2027-02-08", 1))
    }

    @Test
    fun `deltas inside one window become a single write`() = runTest {
        val repository = repository()

        repeat(5) { repository.record(zikirId = 1, delta = 1) }
        passWriteDelay()

        assertEquals(5, dao.count("2027-02-08", 1))
        assertEquals(1, dao.writes)
    }

    @Test
    fun `new deltas add to the stored total`() = runTest {
        dao.put("2027-02-08", 1, 40)
        val repository = repository()

        repository.record(zikirId = 1, delta = 2)
        passWriteDelay()

        assertEquals(42, dao.count("2027-02-08", 1))
    }

    @Test
    fun `deltas are kept apart per zikir`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 3)
        repository.record(zikirId = 2, delta = 1)
        passWriteDelay()

        assertEquals(3, dao.count("2027-02-08", 1))
        assertEquals(1, dao.count("2027-02-08", 2))
    }

    @Test
    fun `undo lowers the stored total`() = runTest {
        dao.put("2027-02-08", 1, 10)
        val repository = repository()

        repository.record(zikirId = 1, delta = -1)
        passWriteDelay()

        assertEquals(9, dao.count("2027-02-08", 1))
    }

    @Test
    fun `total never goes below zero`() = runTest {
        dao.put("2027-02-08", 1, 1)
        val repository = repository()

        repeat(3) { repository.record(zikirId = 1, delta = -1) }
        passWriteDelay()

        assertEquals(0, dao.count("2027-02-08", 1))
    }

    @Test
    fun `undo on a day without counts stays at zero`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = -1)
        passWriteDelay()

        assertEquals(0, dao.count("2027-02-08", 1))
    }

    @Test
    fun `tap and undo in the same window cause no write`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 1)
        repository.record(zikirId = 1, delta = -1)
        passWriteDelay()

        assertEquals(0, dao.writes)
    }

    @Test
    fun `continuous fast tapping keeps reaching the disk`() = runTest {
        val repository = repository()

        // Saniyede 8 dokunuş, 5 saniye boyunca hiç ara verilmez.
        repeat(40) { index ->
            repository.record(zikirId = 1, delta = 1)
            advanceTimeBy(125)
            runCurrent()
            val unwritten = index + 1 - (dao.count("2027-02-08", 1) ?: 0)
            assertTrue("Yazılmamış dokunuş: $unwritten", unwritten <= 3)
        }
        passWriteDelay()

        assertEquals(40, dao.count("2027-02-08", 1))
    }

    @Test
    fun `flush writes pending deltas immediately`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 7)
        repository.flush()

        assertEquals(7, dao.count("2027-02-08", 1))
    }

    @Test
    fun `flush leaves nothing for the delayed write`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 7)
        repository.flush()
        passWriteDelay()

        assertEquals(7, dao.count("2027-02-08", 1))
        assertEquals(1, dao.writes)
    }

    @Test
    fun `flush without pending deltas writes nothing`() = runTest {
        repository().flush()

        assertEquals(0, dao.writes)
    }

    @Test
    fun `session that passes midnight is written to the new day`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 5)
        passWriteDelay()
        today = today.plusDays(1)
        repository.record(zikirId = 1, delta = 2)
        passWriteDelay()

        assertEquals(5, dao.count("2027-02-08", 1))
        assertEquals(2, dao.count("2027-02-09", 1))
    }

    @Test
    fun `date is taken when writing, not when counting`() = runTest {
        val repository = repository()

        repository.record(zikirId = 1, delta = 1)
        today = today.plusDays(1)
        passWriteDelay()

        assertNull(dao.count("2027-02-08", 1))
        assertEquals(1, dao.count("2027-02-09", 1))
    }

    @Test
    fun `stored rows are read back as dates`() = runTest {
        dao.put("2027-02-08", 1, 33)

        assertEquals(
            listOf(DailyCount(LocalDate.of(2027, 2, 8), zikirId = 1, count = 33)),
            repository().observeSince(LocalDate.of(2027, 2, 1)).first(),
        )
    }

    @Test
    fun `days before the start date are left out`() = runTest {
        dao.put("2027-01-31", 1, 33)
        dao.put("2027-02-01", 1, 11)

        val counts = repository().observeSince(LocalDate.of(2027, 2, 1)).first()

        assertEquals(listOf(11), counts.map { it.count })
    }
}
