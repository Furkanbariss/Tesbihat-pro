package com.furkanbarissonmezisik.tesbihatpro.core.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val volumeKeysEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val beadStyleId: String = DEFAULT_BEAD_STYLE_ID,
) {
    companion object {
        const val DEFAULT_BEAD_STYLE_ID = "amber"
    }
}
