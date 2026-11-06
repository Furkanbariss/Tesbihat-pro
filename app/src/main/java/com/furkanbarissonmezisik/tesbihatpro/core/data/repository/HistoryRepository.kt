package com.furkanbarissonmezisik.tesbihatpro.core.data.repository

import android.database.sqlite.SQLiteException
import android.util.Log
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.DailyCountDao
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.DailyCount
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Günlük toplamlar bellekte delta olarak birikir; diske gecikmeli yazılır. */
class HistoryRepository(
    private val dao: DailyCountDao,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val today: () -> LocalDate = LocalDate::now,
) {
    private val pending = mutableMapOf<Long, Int>()
    private val writeLock = Mutex()
    private val delayedWrites = DelayedWrites(scope, ::flush)

    fun observeSince(startDate: LocalDate): Flow<List<DailyCount>> =
        dao.observeFrom(startDate.toString()).map { rows ->
            rows.map { DailyCount(LocalDate.parse(it.date), it.zikirId, it.count) }
        }

    fun record(zikirId: Long, delta: Int) {
        synchronized(pending) {
            pending[zikirId] = (pending[zikirId] ?: 0) + delta
        }
        delayedWrites.request()
    }

    /** Tarih yazım anında alınır: gece yarısını geçen oturum yeni güne yazılır. */
    suspend fun flush() {
        writeLock.withLock {
            val deltas = takePending()
            if (deltas.isEmpty()) return
            try {
                withContext(ioDispatcher) { dao.addAll(today().toString(), deltas) }
            } catch (exception: SQLiteException) {
                Log.e(TAG, "Günlük toplam yazılamadı", exception)
                restorePending(deltas)
            }
        }
    }

    private fun takePending(): Map<Long, Int> = synchronized(pending) {
        pending.filterValues { it != 0 }.also { pending.clear() }
    }

    /** Yazılamayan sayımlar kaybolmaz, bir sonraki yazıma kalır. */
    private fun restorePending(deltas: Map<Long, Int>) {
        synchronized(pending) {
            deltas.forEach { (zikirId, delta) ->
                pending[zikirId] = (pending[zikirId] ?: 0) + delta
            }
        }
    }

    private companion object {
        const val TAG = "HistoryRepository"
    }
}
