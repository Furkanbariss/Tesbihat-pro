package com.furkanbarissonmezisik.tesbihatpro.core.ui

import androidx.compose.ui.graphics.Color

/** Boncuk görünümü veridir; yeni tesbih modelleri bu listeye eklenir. */
data class BeadStyle(
    val id: String,
    val baseColor: Color,
    val highlightColor: Color,
    val threadColor: Color,
)

object BeadStyles {
    val Amber = BeadStyle(
        id = "amber",
        baseColor = Color(0xFFC4761B),
        highlightColor = Color(0xFFFFD98A),
        threadColor = Color(0xFF8D6E52),
    )
    val DarkWood = BeadStyle(
        id = "dark_wood",
        baseColor = Color(0xFF4E2E1C),
        highlightColor = Color(0xFFB07F5A),
        threadColor = Color(0xFF8D6E52),
    )

    val all: List<BeadStyle> = listOf(Amber, DarkWood)
    val default: BeadStyle = Amber
}
