package com.furkanbarissonmezisik.tesbihatpro.core.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings

/** Boncuk görünümü veridir; yeni tesbih modelleri bu listeye eklenir. */
data class BeadStyle(
    val id: String,
    @StringRes val nameRes: Int,
    val baseColor: Color,
    val highlightColor: Color,
    val threadColor: Color,
)

object BeadStyles {
    val Amber = BeadStyle(
        id = AppSettings.DEFAULT_BEAD_STYLE_ID,
        nameRes = R.string.bead_style_amber,
        baseColor = Color(0xFFC4761B),
        highlightColor = Color(0xFFFFD98A),
        threadColor = Color(0xFF8D6E52),
    )
    val DarkWood = BeadStyle(
        id = "dark_wood",
        nameRes = R.string.bead_style_dark_wood,
        baseColor = Color(0xFF4E2E1C),
        highlightColor = Color(0xFFB07F5A),
        threadColor = Color(0xFF8D6E52),
    )

    val all: List<BeadStyle> = listOf(Amber, DarkWood)

    /** Bilinmeyen id varsayılana düşer; kaldırılan bir model uygulamayı bozmaz. */
    fun find(id: String): BeadStyle = all.find { it.id == id } ?: Amber
}
