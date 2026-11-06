package com.furkanbarissonmezisik.tesbihatpro.testing

import com.furkanbarissonmezisik.tesbihatpro.core.data.db.ZikirDao
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.ZikirEntity
import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.ContentVersionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeZikirDao : ZikirDao {
    val rows = MutableStateFlow<List<ZikirEntity>>(emptyList())
    var upsertCalls = 0
    private var nextId = 1000L

    override fun observeAll(): Flow<List<ZikirEntity>> = rows

    override suspend fun upsertPresets(zikirs: List<ZikirEntity>) {
        upsertCalls++
        val ids = zikirs.map { it.id }.toSet()
        rows.value = zikirs + rows.value.filterNot { it.id in ids }
    }

    override suspend fun insert(zikir: ZikirEntity): Long {
        val id = nextId++
        rows.value = rows.value + zikir.copy(id = id)
        return id
    }

    override suspend fun updateOwn(
        id: Long,
        name: String,
        arabic: String?,
        meaning: String?,
        target: Int?,
    ) {
        rows.value = rows.value.map { row ->
            if (row.id == id && !row.isPreset) {
                row.copy(
                    transliterationTr = name,
                    arabic = arabic,
                    meaningTr = meaning,
                    defaultTarget = target,
                )
            } else {
                row
            }
        }
    }

    override suspend fun deleteOwn(id: Long) {
        rows.value = rows.value.filterNot { it.id == id && !it.isPreset }
    }
}

class FakeContentVersionStore(var version: Int = 0) : ContentVersionStore {
    override suspend fun read(): Int = version

    override suspend fun write(version: Int) {
        this.version = version
    }
}
