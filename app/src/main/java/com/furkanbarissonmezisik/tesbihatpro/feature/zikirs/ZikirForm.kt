package com.furkanbarissonmezisik.tesbihatpro.feature.zikirs

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ZikirDraft

/** Düzenleme diyaloğundaki ham alanlar. Boş hedef sınırsız sayımdır. */
internal data class ZikirForm(
    val name: String = "",
    val arabic: String = "",
    val meaning: String = "",
    val target: String = "",
) {
    /** Geçersizse null döner; ad zorunlu, hedef verilmişse 1 ile [MAX_TARGET] arasında olmalıdır. */
    fun toDraft(): ZikirDraft? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return null
        val parsedTarget = if (target.isBlank()) {
            null
        } else {
            target.trim().toIntOrNull()?.takeIf { it in 1..MAX_TARGET } ?: return null
        }
        return ZikirDraft(
            name = trimmedName,
            arabic = arabic.trim().ifEmpty { null },
            meaning = meaning.trim().ifEmpty { null },
            target = parsedTarget,
        )
    }

    companion object {
        const val MAX_TARGET = 999_999
        const val MAX_TARGET_DIGITS = 6
    }
}
