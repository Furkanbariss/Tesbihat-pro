package com.furkanbarissonmezisik.tesbihatpro.core.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyCountDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DailyCountDao

    private val day = "2027-02-08"

    @Before
    fun setUp() {
        database = Room
            .inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                AppDatabase::class.java,
            )
            .addCallback(AppDatabase.ReserveIdsForPresets)
            .build()
        dao = database.dailyCountDao()
    }

    @After
    fun tearDown() = database.close()

    private suspend fun rows() = dao.observeFrom("0000-00-00").first()

    @Test
    fun firstWriteCreatesTheRow() = runBlocking {
        dao.addAll(day, mapOf(1L to 5))

        assertEquals(listOf(DailyCountEntity(day, 1, 5)), rows())
    }

    @Test
    fun laterWritesAddToTheRow() = runBlocking {
        dao.addAll(day, mapOf(1L to 5))
        dao.addAll(day, mapOf(1L to 2))

        assertEquals(listOf(DailyCountEntity(day, 1, 7)), rows())
    }

    @Test
    fun negativeDeltaLowersTheRow() = runBlocking {
        dao.addAll(day, mapOf(1L to 5))
        dao.addAll(day, mapOf(1L to -1))

        assertEquals(listOf(DailyCountEntity(day, 1, 4)), rows())
    }

    @Test
    fun totalNeverGoesBelowZero() = runBlocking {
        dao.addAll(day, mapOf(1L to 1))
        dao.addAll(day, mapOf(1L to -5))
        dao.addAll(day, mapOf(1L to 2))

        assertEquals(listOf(DailyCountEntity(day, 1, 2)), rows())
    }

    @Test
    fun negativeDeltaOnANewRowStaysAtZeroAndIsHidden() = runBlocking {
        dao.addAll(day, mapOf(1L to -1))

        assertTrue(rows().isEmpty())
    }

    @Test
    fun onePrimaryKeyPerDayAndZikir() = runBlocking {
        dao.addAll(day, mapOf(1L to 1, 2L to 2))
        dao.addAll("2027-02-09", mapOf(1L to 3))

        assertEquals(3, rows().size)
    }

    @Test
    fun rowsComeNewestDayFirstThenHighestCount() = runBlocking {
        dao.addAll("2027-02-07", mapOf(1L to 100))
        dao.addAll(day, mapOf(1L to 1, 2L to 2))

        assertEquals(
            listOf(
                DailyCountEntity(day, 2, 2),
                DailyCountEntity(day, 1, 1),
                DailyCountEntity("2027-02-07", 1, 100),
            ),
            rows(),
        )
    }

    @Test
    fun startDateIsInclusive() = runBlocking {
        dao.addAll("2027-02-07", mapOf(1L to 1))
        dao.addAll(day, mapOf(1L to 2))

        assertEquals(listOf(day), dao.observeFrom(day).first().map { it.date })
    }

    @Test
    fun deletingAZikirKeepsItsCounts() = runBlocking {
        val id = database.zikirDao().insert(
            ZikirEntity(
                arabic = null,
                transliterationTr = "a",
                transliterationEn = null,
                meaningTr = null,
                meaningEn = null,
                defaultTarget = 33,
                isPreset = false,
                sortOrder = 0,
                createdAt = 1,
            ),
        )
        dao.addAll(day, mapOf(id to 9))

        database.zikirDao().deleteOwn(id)

        assertEquals(listOf(DailyCountEntity(day, id, 9)), rows())
    }
}
