package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Dizideki sıra adımlar boyunca kesintisizdir: adım değişince sayı 0'a düşse de dizi ilerlemeye
 * devam eder, geriye sıçramaz.
 */
internal fun beadPosition(state: CounterState): Int =
    state.steps.take(state.stepIndex).sumOf { it.target ?: 0 } + state.count

/** Her adım hedefinin dizideki sırası. Sınırsız adımın imamesi yoktur. */
internal fun imameBeads(steps: List<TesbihatStep>): List<Int> {
    var total = 0
    return steps.mapNotNull { step ->
        step.target?.let { target ->
            total += target
            total
        }
    }
}

/** Dizinin bittiği boncuk; son adım sınırsızsa dizi bitmez. */
internal fun lastBead(steps: List<TesbihatStep>): Int? =
    if (steps.last().target != null) imameBeads(steps).last() else null

internal fun visibleBeads(position: Float, beadsPerSide: Int, lastBead: Int?): IntRange {
    val first = floor(position).toInt() - beadsPerSide
    val last = ceil(position).toInt() + beadsPerSide
    return first..(if (lastBead != null) minOf(last, lastBead) else last)
}

/**
 * Bir parmağın sürüklemesini sayıma çevirir. Parmağın inişi zaten bir sayımdır, bu yüzden ilk boncuk
 * aralığı o sayıma dahildir; sonraki her aralık bir sayım daha ekler. Yukarı sürükleme sayılmaz.
 */
internal class BeadDrag(private val startY: Float) {
    private var counted = 1

    /** [y] konumuna gelince eklenecek sayım adedini döner. */
    fun advance(y: Float, spacing: Float): Int {
        val reached = ((y - startY) / spacing).toInt()
        val newTaps = (reached - counted).coerceAtLeast(0)
        counted += newTaps
        return newTaps
    }
}
