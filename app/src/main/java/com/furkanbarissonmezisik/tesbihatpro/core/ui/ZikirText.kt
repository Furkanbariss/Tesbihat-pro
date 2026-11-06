package com.furkanbarissonmezisik.tesbihatpro.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.booleanResource
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir

/** Arayüz diline göre okunuş; İngilizcesi olmayan zikirde Türkçe okunuşa düşer. */
@Composable
@ReadOnlyComposable
fun Zikir.displayTransliteration(): String =
    if (booleanResource(R.bool.prefer_turkish_zikir_text)) {
        transliterationTr
    } else {
        transliterationEn ?: transliterationTr
    }

/** Arayüz diline göre anlam; İngilizcesi olmayan zikirde Türkçe anlama düşer. */
@Composable
@ReadOnlyComposable
fun Zikir.displayMeaning(): String? =
    if (booleanResource(R.bool.prefer_turkish_zikir_text)) meaningTr else meaningEn ?: meaningTr
