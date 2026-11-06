package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.DailyCountDao
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.DailyCountEntity
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryRepositoryFailureTest {

    private class FlakyDao : DailyCountDao {
        var failures = 0
        val written = mutableListOf<Map<Long, Int>>()

        override fun observeFrom(startDate: String): Flow<List<DailyCountEntity>> = emptyFlow()

        override suspend fun ensureRow(date: String, zikirId: Long) = Unit

        override suspend fun applyDelta(date: String, zikirId: Long, delta: Int) = Unit

        override suspend fun addAll(date: String, deltas: Map<Long, Int>) {
            if (failures > 0) {
                failures--
                throw SQLiteException("disk dolu")
            }
            written += deltas
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val dao = FlakyDao()
    private val repository = HistoryRepository(dao, scope, today = { LocalDate.of(2027, 2, 8) })

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun failedWriteKeepsTheCountsForTheNextOne() = runBlocking {
        dao.failures = 1
        repository.record(zikirId = 1, delta = 5)

        repository.flush()
        repository.flush()

        assertEquals(listOf(mapOf(1L to 5)), dao.written)
    }

    @Test
    fun countsMadeAfterAFailureAddToTheKeptOnes() = runBlocking {
        dao.failures = 1
        repository.record(zikirId = 1, delta = 5)
        repository.flush()

        repository.record(zikirId = 1, delta = 2)
        repository.flush()

        assertEquals(listOf(mapOf(1L to 7)), dao.written)
    }
}
