package com.furkanbarissonmezisik.tesbihatpro.core.domain.model

/** Kullanıcının kendi zikri için girdiği alanlar. [target] null = sınırsız sayım. */
data class ZikirDraft(
    val name: String,
    val arabic: String?,
    val meaning: String?,
    val target: Int?,
)
