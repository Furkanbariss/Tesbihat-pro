package com.furkanbarissonmezisik.tesbihatpro.core.content

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir

/**
 * Tüm dini metinlerin tek kaynağı. Dini içerikte ölçü Diyanet'tir; kaynaklar CLAUDE.md'dedir.
 *
 * Id'ler sabittir ve oturum kayıtlarında saklanır; değiştirilmez. 1000 altı hazır zikirlere ayrılmıştır.
 */
object PresetContent {
    const val SUBHANALLAH_ID = 1L
    const val ELHAMDULILLAH_ID = 2L
    const val ALLAHU_EKBER_ID = 3L
    const val ESTAGFIRULLAH_ID = 4L
    const val KELIME_I_TEVHID_ID = 5L
    const val SALAVAT_ID = 6L
    const val TEHLIL_ID = 7L

    val zikirs: List<Zikir> = listOf(
        Zikir(
            id = SUBHANALLAH_ID,
            arabic = "سُبْحَانَ اللّٰهِ",
            transliterationTr = "Sübhânallah",
            transliterationEn = "Subhan Allah",
            meaningTr = "Allah'ı her türlü eksiklikten tenzih ederim",
            meaningEn = "Glory be to Allah",
            defaultTarget = 33,
            isPreset = true,
        ),
        Zikir(
            id = ELHAMDULILLAH_ID,
            arabic = "اَلْحَمْدُ لِلّٰهِ",
            transliterationTr = "Elhamdülillâh",
            transliterationEn = "Alhamdulillah",
            meaningTr = "Hamd Allah'a mahsustur",
            meaningEn = "All praise is for Allah",
            defaultTarget = 33,
            isPreset = true,
        ),
        Zikir(
            id = ALLAHU_EKBER_ID,
            arabic = "اَللّٰهُ أَكْبَرُ",
            transliterationTr = "Allâhü ekber",
            transliterationEn = "Allahu Akbar",
            meaningTr = "Allah en büyüktür",
            meaningEn = "Allah is the Greatest",
            defaultTarget = 33,
            isPreset = true,
        ),
        Zikir(
            id = ESTAGFIRULLAH_ID,
            arabic = "أَسْتَغْفِرُ اللّٰهَ",
            transliterationTr = "Estağfirullâh",
            transliterationEn = "Astaghfirullah",
            meaningTr = "Allah'tan bağışlanma dilerim",
            meaningEn = "I ask forgiveness from Allah",
            defaultTarget = 100,
            isPreset = true,
        ),
        Zikir(
            id = KELIME_I_TEVHID_ID,
            arabic = "لَا إِلٰهَ إِلَّا اللّٰهُ",
            transliterationTr = "Lâ ilâhe illallâh",
            transliterationEn = "La ilaha illallah",
            meaningTr = "Allah'tan başka ilâh yoktur",
            meaningEn = "There is no god but Allah",
            defaultTarget = 100,
            isPreset = true,
        ),
        Zikir(
            id = SALAVAT_ID,
            arabic = "اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ",
            transliterationTr = "Allâhümme salli alâ seyyidinâ Muhammed",
            transliterationEn = "Allahumma salli ala sayyidina Muhammad",
            meaningTr = "Allah'ım, efendimiz Muhammed'e salat eyle",
            meaningEn = "O Allah, send salat upon our master Muhammad",
            defaultTarget = 100,
            isPreset = true,
        ),
        Zikir(
            id = TEHLIL_ID,
            arabic = "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliterationTr = "Lâ ilâhe illallâh vahdehû lâ şerîke leh, lehü’l-mülkü ve lehü’l-hamdü ve hüve alâ külli şey’in kadîr",
            transliterationEn = "La ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa huwa ala kulli shay'in qadir",
            meaningTr = "Allah’tan başka ilâh yoktur; yalnız Allah vardır. O tektir, ortağı yoktur. Mülk O’nundur, hamd O’na mahsustur. O’nun gücü her şeye yeter.",
            meaningEn = "There is no god but Allah alone. He is One and has no partner. All sovereignty is His and all praise is His. His power suffices for all things.",
            defaultTarget = 1,
            isPreset = true,
        ),
    )

    /** Diyanet fetvasına göre: 33'er tesbih, tahmid, tekbir; ardından yüze tamamlayan tehlil. */
    val defaultTesbihat: List<TesbihatStep> = listOf(
        TesbihatStep(zikirId = SUBHANALLAH_ID, target = 33),
        TesbihatStep(zikirId = ELHAMDULILLAH_ID, target = 33),
        TesbihatStep(zikirId = ALLAHU_EKBER_ID, target = 33),
        TesbihatStep(zikirId = TEHLIL_ID, target = 1),
    )
}
