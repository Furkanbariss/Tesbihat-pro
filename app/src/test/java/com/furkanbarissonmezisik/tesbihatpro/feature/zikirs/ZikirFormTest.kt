package com.furkanbarissonmezisik.tesbihatpro.feature.zikirs

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ZikirDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZikirFormTest {

    @Test
    fun `name is required`() {
        assertNull(ZikirForm(name = "", target = "33").toDraft())
        assertNull(ZikirForm(name = "   ", target = "33").toDraft())
    }

    @Test
    fun `empty target means unlimited`() {
        assertNull(ZikirForm(name = "Ya Latif").toDraft()?.target)
    }

    @Test
    fun `target is parsed`() {
        assertEquals(129, ZikirForm(name = "Ya Latif", target = "129").toDraft()?.target)
    }

    @Test
    fun `zero target is rejected`() {
        assertNull(ZikirForm(name = "Ya Latif", target = "0").toDraft())
    }

    @Test
    fun `target above the limit is rejected`() {
        assertNull(ZikirForm(name = "Ya Latif", target = "1000000").toDraft())
    }

    @Test
    fun `largest target is accepted`() {
        assertEquals(999_999, ZikirForm(name = "Ya Latif", target = "999999").toDraft()?.target)
    }

    @Test
    fun `text is trimmed and blanks become null`() {
        assertEquals(
            ZikirDraft(name = "Ya Latif", arabic = null, meaning = null, target = null),
            ZikirForm(name = "  Ya Latif ", arabic = "  ", meaning = " ").toDraft(),
        )
    }
}
