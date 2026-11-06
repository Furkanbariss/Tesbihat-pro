package com.furkanbarissonmezisik.tesbihatpro.feature.history

import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.HistoryRepository
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.ZikirRepository
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.DailyCount
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeAdsController
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeContentVersionStore
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeDailyCountDao
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeZikirDao
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val mainDispatcher: TestDispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2027, 2, 8)
    private val zikirs = PresetContent.zikirs
    private val dao = FakeDailyCountDao()

    @Before
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun count(daysAgo: Long, zikirId: Long, count: Int) =
        DailyCount(today.minusDays(daysAgo), zikirId, count)

    private fun TestScope.createViewModel(repository: HistoryRepository): HistoryViewModel {
        val viewModel = HistoryViewModel(
            historyRepository = repository,
            zikirRepository = ZikirRepository(
                FakeZikirDao(),
                FakeContentVersionStore(),
                backgroundScope,
            ),
            adsController = FakeAdsController(),
            today = today,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private fun TestScope.repository() = HistoryRepository(
        dao = dao,
        scope = backgroundScope,
        ioDispatcher = mainDispatcher,
        today = { today },
    )

    @Test
    fun `no counts gives an empty state`() {
        assertEquals(HistoryUiState(), historyUiState(emptyList(), zikirs, today))
    }

    @Test
    fun `today is summed across zikirs`() {
        val state = historyUiState(listOf(count(0, 1, 33), count(0, 2, 10)), zikirs, today)

        assertEquals(43, state.todayTotal)
        assertEquals(listOf(33, 10), state.todayEntries.map { it.count })
        assertTrue(state.pastDays.isEmpty())
    }

    @Test
    fun `entries of a day are ordered by count`() {
        val state = historyUiState(listOf(count(0, 1, 5), count(0, 2, 40)), zikirs, today)

        assertEquals(listOf(2L, 1L), state.todayEntries.map { it.zikir?.id })
    }

    @Test
    fun `past days are grouped and ordered newest first`() {
        val state = historyUiState(
            listOf(count(3, 1, 7), count(1, 1, 33), count(1, 2, 33), count(0, 1, 1)),
            zikirs,
            today,
        )

        assertEquals(listOf(today.minusDays(1), today.minusDays(3)), state.pastDays.map { it.date })
        assertEquals(listOf(66, 7), state.pastDays.map { it.total })
    }

    @Test
    fun `today is not repeated among the past days`() {
        val state = historyUiState(listOf(count(0, 1, 1)), zikirs, today)

        assertTrue(state.pastDays.none { it.date == today })
    }

    @Test
    fun `counts of a deleted zikir stay with no zikir attached`() {
        val state = historyUiState(listOf(count(0, 1234, 9)), zikirs, today)

        assertEquals(9, state.todayTotal)
        assertNull(state.todayEntries.single().zikir)
    }

    @Test
    fun `screen shows thirty days including today`() = runTest(mainDispatcher) {
        dao.put("2027-01-10", 1, 1)
        dao.put("2027-01-09", 1, 2)
        val viewModel = createViewModel(repository())

        assertEquals(
            listOf(LocalDate.of(2027, 1, 10)),
            viewModel.uiState.value.pastDays.map { it.date },
        )
    }

    @Test
    fun `new counts reach the open screen`() = runTest(mainDispatcher) {
        val repository = repository()
        val viewModel = createViewModel(repository)

        repository.record(zikirId = 1, delta = 3)
        advanceTimeBy(301)
        runCurrent()

        assertEquals(3, viewModel.uiState.value.todayTotal)
    }

    @Test
    fun `undo lowers the total on the open screen`() = runTest(mainDispatcher) {
        dao.put(today.toString(), 1, 10)
        val repository = repository()
        val viewModel = createViewModel(repository)

        repository.record(zikirId = 1, delta = -1)
        advanceTimeBy(301)
        runCurrent()

        assertEquals(9, viewModel.uiState.value.todayTotal)
    }

    @Test
    fun `zikir undone to zero disappears from the day`() = runTest(mainDispatcher) {
        dao.put(today.toString(), 1, 1)
        val repository = repository()
        val viewModel = createViewModel(repository)

        repository.record(zikirId = 1, delta = -1)
        advanceTimeBy(301)
        runCurrent()

        assertEquals(HistoryUiState(), viewModel.uiState.value)
    }
}
