package com.furkanbarissonmezisik.tesbihatpro.core.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZikirDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ZikirDao

    private fun own(name: String) = ZikirEntity(
        arabic = null,
        transliterationTr = name,
        transliterationEn = null,
        meaningTr = null,
        meaningEn = null,
        defaultTarget = 33,
        isPreset = false,
        sortOrder = 0,
        createdAt = 1,
    )

    @Before
    fun setUp() = runBlocking {
        database = Room
            .inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                AppDatabase::class.java,
            )
            .addCallback(AppDatabase.ReserveIdsForPresets)
            .build()
        dao = database.zikirDao()
        dao.upsertPresets(
            PresetContent.zikirs.mapIndexed { index, zikir -> zikir.toEntity(index, createdAt = 0) },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun presetsKeepTheirFixedIdsAndOrder() = runBlocking {
        assertEquals(PresetContent.zikirs, dao.observeAll().first().map { it.toDomain() })
    }

    @Test
    fun firstUserZikirGetsId1000() = runBlocking {
        assertEquals(1000L, dao.insert(own("a")))
    }

    @Test
    fun deletedIdIsNeverReused() = runBlocking {
        val deleted = dao.insert(own("a"))
        dao.deleteOwn(deleted)

        assertNotEquals(deleted, dao.insert(own("b")))
    }

    @Test
    fun loadingPresetsAgainKeepsUserIdsAboveThePresetRange() = runBlocking {
        dao.insert(own("a"))
        dao.upsertPresets(
            PresetContent.zikirs.mapIndexed { index, zikir -> zikir.toEntity(index, createdAt = 0) },
        )

        assertTrue(dao.insert(own("b")) > 1000L)
        assertEquals(PresetContent.zikirs.size + 2, dao.observeAll().first().size)
    }

    @Test
    fun presetsAreListedBeforeUserZikirs() = runBlocking {
        dao.insert(own("a"))

        assertEquals("a", dao.observeAll().first().last().transliterationTr)
    }

    @Test
    fun presetCannotBeDeleted() = runBlocking {
        dao.deleteOwn(PresetContent.SUBHANALLAH_ID)

        assertEquals(PresetContent.zikirs.size, dao.observeAll().first().size)
    }

    @Test
    fun presetCannotBeUpdated() = runBlocking {
        dao.updateOwn(PresetContent.SUBHANALLAH_ID, "x", null, null, 1)

        assertEquals(PresetContent.zikirs, dao.observeAll().first().map { it.toDomain() })
    }

    @Test
    fun updateChangesOnlyTheGivenUserZikir() = runBlocking {
        val first = dao.insert(own("a"))
        dao.insert(own("b"))

        dao.updateOwn(first, "c", "ar", "m", null)

        val names = dao.observeAll().first().map { it.transliterationTr }
        assertTrue("c" in names && "b" in names && "a" !in names)
    }
}
