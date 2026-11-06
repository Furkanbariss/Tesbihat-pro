package com.furkanbarissonmezisik.tesbihatpro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity

/** Zikre yabancı anahtarla bağlı değildir: zikir silinse de geçmiş sayımlar kalır. */
@Entity(tableName = "daily_counts", primaryKeys = ["date", "zikir_id"])
data class DailyCountEntity(
    /** Yerel saate göre yyyy-MM-dd */
    val date: String,
    @ColumnInfo(name = "zikir_id") val zikirId: Long,
    val count: Int,
)
