package com.furkanbarissonmezisik.tesbihatpro.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class BeadStylesTest {

    private val lightBackground = Color(0xFFFFF8F5)
    private val darkBackground = Color(0xFF19120C)

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance() + 0.05f
        val b = second.luminance() + 0.05f
        return max(a, b) / min(a, b)
    }

    @Test
    fun `imame stands out from the beads in every style`() {
        BeadStyles.all.forEach { style ->
            assertTrue(style.id, contrast(style.imameBaseColor, style.baseColor) >= 1.8f)
        }
    }

    @Test
    fun `imame stays visible on both theme backgrounds`() {
        BeadStyles.all.forEach { style ->
            assertTrue(style.id, contrast(style.imameBaseColor, lightBackground) >= 1.8f)
            assertTrue(style.id, contrast(style.imameBaseColor, darkBackground) >= 1.8f)
        }
    }

    @Test
    fun `style ids are unique`() {
        assertEquals(BeadStyles.all.size, BeadStyles.all.map { it.id }.toSet().size)
    }

    @Test
    fun `unknown style falls back to the default`() {
        assertEquals(BeadStyles.Amber, BeadStyles.find("removed_model"))
    }
}
