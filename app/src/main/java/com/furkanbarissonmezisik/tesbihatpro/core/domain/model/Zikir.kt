package com.furkanbarissonmezisik.tesbihatpro.core.domain.model

data class Zikir(
    val id: Long,
    val arabic: String?,
    val transliterationTr: String,
    val transliterationEn: String?,
    val meaningTr: String?,
    val meaningEn: String?,
    /** null = sınırsız sayım */
    val defaultTarget: Int?,
    val isPreset: Boolean,
)
