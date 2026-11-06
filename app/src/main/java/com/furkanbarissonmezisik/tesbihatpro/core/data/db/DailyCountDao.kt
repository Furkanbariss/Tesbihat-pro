package com.furkanbarissonmezisik.tesbihatpro.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCountDao {
    @Query(
        """
        SELECT * FROM daily_counts
        WHERE date >= :startDate AND count > 0
        ORDER BY date DESC, count DESC
        """,
    )
    fun observeFrom(startDate: String): Flow<List<DailyCountEntity>>

    @Query("INSERT OR IGNORE INTO daily_counts (date, zikir_id, count) VALUES (:date, :zikirId, 0)")
    suspend fun ensureRow(date: String, zikirId: Long)

    /** Toplam 0'ın altına inmez. */
    @Query(
        """
        UPDATE daily_counts SET count = MAX(0, count + :delta)
        WHERE date = :date AND zikir_id = :zikirId
        """,
    )
    suspend fun applyDelta(date: String, zikirId: Long, delta: Int)

    /** Tek işlem: yazım yarıda kalırsa bu tarihteki hiçbir delta uygulanmaz. */
    @Transaction
    suspend fun addAll(date: String, deltas: Map<Long, Int>) {
        deltas.forEach { (zikirId, delta) ->
            ensureRow(date, zikirId)
            applyDelta(date, zikirId, delta)
        }
    }
}
