package com.furkanbarissonmezisik.tesbihatpro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir

@Entity(tableName = "zikirs")
data class ZikirEntity(
    /** Kullanıcı zikirlerinde 0 verilir, veritabanı üretir; silinen id tekrar kullanılmaz. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val arabic: String?,
    @ColumnInfo(name = "translit_tr") val transliterationTr: String,
    @ColumnInfo(name = "translit_en") val transliterationEn: String?,
    @ColumnInfo(name = "meaning_tr") val meaningTr: String?,
    @ColumnInfo(name = "meaning_en") val meaningEn: String?,
    @ColumnInfo(name = "default_target") val defaultTarget: Int?,
    @ColumnInfo(name = "is_preset") val isPreset: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

internal fun ZikirEntity.toDomain() = Zikir(
    id = id,
    arabic = arabic,
    transliterationTr = transliterationTr,
    transliterationEn = transliterationEn,
    meaningTr = meaningTr,
    meaningEn = meaningEn,
    defaultTarget = defaultTarget,
    isPreset = isPreset,
)

internal fun Zikir.toEntity(sortOrder: Int, createdAt: Long) = ZikirEntity(
    id = id,
    arabic = arabic,
    transliterationTr = transliterationTr,
    transliterationEn = transliterationEn,
    meaningTr = meaningTr,
    meaningEn = meaningEn,
    defaultTarget = defaultTarget,
    isPreset = isPreset,
    sortOrder = sortOrder,
    createdAt = createdAt,
)
