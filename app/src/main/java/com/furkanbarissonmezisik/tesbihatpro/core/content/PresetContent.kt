package com.furkanbarissonmezisik.tesbihatpro.core.content

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir

/**
 * Tüm dini metinlerin tek kaynağı. Metinler yayından önce güvenilir bir kaynaktan kontrol edilir.
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
            transliterationEn = "Al-hamdu Lillah",
            meaningTr = "Hamd Allah'a mahsustur",
            meaningEn = "Praise be to Allah",
            defaultTarget = 33,
            isPreset = true,
        ),
        Zikir(
            id = ALLAHU_EKBER_ID,
            arabic = "اَللّٰهُ أَكْبَرُ",
            transliterationTr = "Allâhü ekber",
            transliterationEn = "Allahu akbar",
            meaningTr = "Allah en büyüktür",
            meaningEn = "Allah is most great",
            defaultTarget = 33,
            isPreset = true,
        ),
        Zikir(
            id = ESTAGFIRULLAH_ID,
            arabic = "أَسْتَغْفِرُ اللّٰهَ",
            transliterationTr = "Estağfirullâh",
            transliterationEn = "Astaghfirullah",
            meaningTr = "Allah'tan bağışlanma dilerim",
            meaningEn = "I ask Allah for forgiveness",
            defaultTarget = 100,
            isPreset = true,
        ),
        Zikir(
            id = KELIME_I_TEVHID_ID,
            arabic = "لَا إِلٰهَ إِلَّا اللّٰهُ",
            transliterationTr = "Lâ ilâhe illallâh",
            transliterationEn = "La ilaha illa Allah",
            meaningTr = "Allah'tan başka ilah yoktur",
            meaningEn = "There is no god but Allah",
            defaultTarget = 100,
            isPreset = true,
        ),
        Zikir(
            id = SALAVAT_ID,
            arabic = "اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ",
            transliterationTr = "Allâhümme salli alâ seyyidinâ Muhammed",
            transliterationEn = "Allahumma salli ‘ala sayyidina Muhammad",
            meaningTr = "Allah'ım, efendimiz Muhammed'e salat eyle",
            meaningEn = "O Allah, send prayers upon our master Muhammad",
            defaultTarget = 100,
            isPreset = true,
        ),
    )

    val defaultTesbihat: List<TesbihatStep> = listOf(
        TesbihatStep(zikirId = SUBHANALLAH_ID, target = 33),
        TesbihatStep(zikirId = ELHAMDULILLAH_ID, target = 33),
        TesbihatStep(zikirId = ALLAHU_EKBER_ID, target = 33),
    )
}
