package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ZikirDraft
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeContentVersionStore
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeZikirDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZikirRepositoryTest {

    private val dao = FakeZikirDao()
    private val contentVersion = FakeContentVersionStore()
    private val draft = ZikirDraft(name = "Ya Latif", arabic = null, meaning = "m", target = 129)

    private fun TestScope.repository() = ZikirRepository(
        dao = dao,
        contentVersion = contentVersion,
        scope = backgroundScope,
        now = { 42L },
    )

    @Test
    fun `first list already contains every preset in order`() = runTest {
        val zikirs = repository().zikirs.first()

        assertEquals(PresetContent.zikirs, zikirs)
    }

    @Test
    fun `presets are stored with their table order`() = runTest {
        repository().zikirs.first()

        assertEquals((0..6).toList(), dao.rows.value.map { it.sortOrder })
    }

    @Test
    fun `content version is recorded after loading`() = runTest {
        repository().zikirs.first()

        assertEquals(PresetContent.VERSION, contentVersion.version)
    }

    @Test
    fun `presets are not loaded again when the version is current`() = runTest {
        contentVersion.version = PresetContent.VERSION

        repository().zikirs.first()

        assertEquals(0, dao.upsertCalls)
    }

    @Test
    fun `older content version loads presets again`() = runTest {
        contentVersion.version = PresetContent.VERSION - 1

        repository().zikirs.first()

        assertEquals(1, dao.upsertCalls)
    }

    @Test
    fun `reloading presets keeps user zikirs`() = runTest {
        val repository = repository()
        repository.zikirs.first()
        repository.add(draft)
        contentVersion.version = 0

        ZikirRepository(dao, contentVersion, backgroundScope).zikirs.first()

        assertTrue(dao.rows.value.any { it.transliterationTr == "Ya Latif" })
    }

    @Test
    fun `added zikir is stored as the users own`() = runTest {
        val repository = repository()
        repository.zikirs.first()

        repository.add(draft)

        val zikir = repository.zikirs.first().last()
        assertEquals("Ya Latif", zikir.transliterationTr)
        assertEquals("m", zikir.meaningTr)
        assertEquals(129, zikir.defaultTarget)
        assertFalse(zikir.isPreset)
        assertNull(zikir.transliterationEn)
        assertEquals(42L, dao.rows.value.last().createdAt)
    }

    @Test
    fun `user zikir ids start above the preset range`() = runTest {
        val repository = repository()
        repository.zikirs.first()

        repository.add(draft)

        assertTrue(repository.zikirs.first().last().id >= 1000)
    }

    @Test
    fun `update changes the users zikir`() = runTest {
        val repository = repository()
        repository.zikirs.first()
        repository.add(draft)
        val id = repository.zikirs.first().last().id

        repository.update(id, draft.copy(name = "Ya Hafiz", target = null))

        val zikir = repository.zikirs.first().last()
        assertEquals("Ya Hafiz", zikir.transliterationTr)
        assertNull(zikir.defaultTarget)
    }

    @Test
    fun `presets cannot be updated`() = runTest {
        val repository = repository()
        repository.zikirs.first()

        repository.update(PresetContent.SUBHANALLAH_ID, draft)

        assertEquals(PresetContent.zikirs, repository.zikirs.first())
    }

    @Test
    fun `delete removes the users zikir`() = runTest {
        val repository = repository()
        repository.zikirs.first()
        repository.add(draft)
        val id = repository.zikirs.first().last().id

        repository.delete(id)

        assertEquals(PresetContent.zikirs, repository.zikirs.first())
    }

    @Test
    fun `presets cannot be deleted`() = runTest {
        val repository = repository()
        repository.zikirs.first()

        repository.delete(PresetContent.SUBHANALLAH_ID)

        assertEquals(PresetContent.zikirs, repository.zikirs.first())
    }
}
