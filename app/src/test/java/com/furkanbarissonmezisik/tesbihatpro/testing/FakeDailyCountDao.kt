package com.furkanbarissonmezisik.tesbihatpro.testing

import com.furkanbarissonmezisik.tesbihatpro.core.data.db.DailyCountDao
import com.furkanbarissonmezisik.tesbihatpro.core.data.db.DailyCountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDailyCountDao : DailyCountDao {
    private val rows = MutableStateFlow<Map<Pair<String, Long>, Int>>(emptyMap())

    /** addAll çağrı sayısı: her biri tek bir disk yazımıdır. */
    var writes = 0
        private set

    fun count(date: String, zikirId: Long): Int? = rows.value[date to zikirId]

    fun put(date: String, zikirId: Long, count: Int) {
        rows.value = rows.value + ((date to zikirId) to count)
    }

    override fun observeFrom(startDate: String): Flow<List<DailyCountEntity>> = rows.map { all ->
        all.filter { (key, count) -> key.first >= startDate && count > 0 }
            .map { (key, count) -> DailyCountEntity(key.first, key.second, count) }
            .sortedWith(compareByDescending<DailyCountEntity> { it.date }.thenByDescending { it.count })
    }

    override suspend fun ensureRow(date: String, zikirId: Long) {
        if ((date to zikirId) !in rows.value) put(date, zikirId, 0)
    }

    override suspend fun applyDelta(date: String, zikirId: Long, delta: Int) {
        put(date, zikirId, maxOf(0, rows.value.getValue(date to zikirId) + delta))
    }

    override suspend fun addAll(date: String, deltas: Map<Long, Int>) {
        writes++
        super.addAll(date, deltas)
    }
}
