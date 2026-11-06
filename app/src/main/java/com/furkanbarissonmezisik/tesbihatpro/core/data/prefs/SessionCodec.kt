package com.furkanbarissonmezisik.tesbihatpro.core.data.prefs

import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep

/** Adım listesini DataStore için `1:33,2:33,5:` biçiminde tutar; boş hedef sınırsız sayımdır. */
internal object SessionCodec {

    fun encodeSteps(steps: List<TesbihatStep>): String =
        steps.joinToString(separator = ",") { "${it.zikirId}:${it.target ?: ""}" }

    /** Kayıt bozuksa null döner; çağıran varsayılan oturuma düşer. */
    fun decode(steps: String, stepIndex: Int, count: Int, completed: Boolean): CounterState? {
        val decodedSteps = decodeSteps(steps) ?: return null
        if (stepIndex !in decodedSteps.indices || count < 0) return null
        return CounterState(decodedSteps, stepIndex, count, completed)
    }

    private fun decodeSteps(encoded: String): List<TesbihatStep>? {
        if (encoded.isEmpty()) return null
        return encoded.split(',').map { part ->
            val fields = part.split(':')
            if (fields.size != 2) return null
            val zikirId = fields[0].toLongOrNull() ?: return null
            val target = if (fields[1].isEmpty()) null else fields[1].toIntOrNull() ?: return null
            TesbihatStep(zikirId, target)
        }
    }
}
