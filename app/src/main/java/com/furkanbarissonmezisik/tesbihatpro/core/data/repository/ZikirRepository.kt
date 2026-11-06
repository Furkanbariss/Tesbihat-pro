package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import android.database.sqlite.SQLiteException
import android.util.Log
import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.ZikirDao
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.ZikirEntity
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.toDomain
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.toEntity
import com.furkanbarissonmezisik.tesbihatpro.core.data.prefs.ContentVersionStore
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ZikirDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ZikirRepository(
    private val dao: ZikirDao,
    private val contentVersion: ContentVersionStore,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val presetsLoaded: Deferred<Unit> = scope.async { loadPresetsIfNeeded() }

    /** Hazır içerik yüklenmeden yayın başlamaz; ilk liste her zaman hazırları içerir. */
    val zikirs: Flow<List<Zikir>> = flow {
        presetsLoaded.await()
        emitAll(dao.observeAll().map { entities -> entities.map { it.toDomain() } })
    }

    suspend fun add(draft: ZikirDraft) = logFailure("eklenemedi") {
        dao.insert(
            ZikirEntity(
                arabic = draft.arabic,
                transliterationTr = draft.name,
                transliterationEn = null,
                meaningTr = draft.meaning,
                meaningEn = null,
                defaultTarget = draft.target,
                isPreset = false,
                sortOrder = 0,
                createdAt = now(),
            ),
        )
    }

    suspend fun update(id: Long, draft: ZikirDraft) = logFailure("güncellenemedi") {
        dao.updateOwn(id, draft.name, draft.arabic, draft.meaning, draft.target)
    }

    suspend fun delete(id: Long) = logFailure("silinemedi") {
        dao.deleteOwn(id)
    }

    private suspend fun loadPresetsIfNeeded() {
        if (contentVersion.read() >= PresetContent.VERSION) return
        logFailure("hazır içerik yüklenemedi") {
            dao.upsertPresets(
                PresetContent.zikirs.mapIndexed { index, zikir ->
                    zikir.toEntity(sortOrder = index, createdAt = 0)
                },
            )
            contentVersion.write(PresetContent.VERSION)
        }
    }

    private suspend fun logFailure(action: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (exception: SQLiteException) {
            Log.e(TAG, "Zikir $action", exception)
        }
    }

    private companion object {
        const val TAG = "ZikirRepository"
    }
}
