package com.furkanbarissonmezisik.tesbihatpro.core.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    // Camide ilk dokunuş duyulmasın diye ses kapalı başlar.
    val soundEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val volumeKeysEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val beadStyleId: String = DEFAULT_BEAD_STYLE_ID,
    // Uygulama kâr için yapılmadı; kullanıcı reklamları Ayarlar'dan kapatabilir.
    val adsEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_BEAD_STYLE_ID = "amber"
    }
}
