package com.furkanbarissonmezisik.tesbihatpro.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ZikirDao {
    @Query("SELECT * FROM zikirs ORDER BY is_preset DESC, sort_order, created_at")
    fun observeAll(): Flow<List<ZikirEntity>>

    @Upsert
    suspend fun upsertPresets(zikirs: List<ZikirEntity>)

    @Insert
    suspend fun insert(zikir: ZikirEntity): Long

    /** Hazır zikirler düzenlenemez; koşul bunu veritabanı düzeyinde de garanti eder. */
    @Query(
        """
        UPDATE zikirs
        SET translit_tr = :name, arabic = :arabic, meaning_tr = :meaning, default_target = :target
        WHERE id = :id AND is_preset = 0
        """,
    )
    suspend fun updateOwn(id: Long, name: String, arabic: String?, meaning: String?, target: Int?)

    /** Hazır zikirler silinemez; koşul bunu veritabanı düzeyinde de garanti eder. */
    @Query("DELETE FROM zikirs WHERE id = :id AND is_preset = 0")
    suspend fun deleteOwn(id: Long)
}
