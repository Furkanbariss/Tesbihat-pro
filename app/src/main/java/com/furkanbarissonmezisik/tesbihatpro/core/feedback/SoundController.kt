package com.furkanbarissonmezisik.tesbihatpro.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEffect

interface SoundController {
    fun play(effect: CounterEffect)
}

class SoundPoolController(context: Context) : SoundController {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val tick = soundPool.load(context, R.raw.tesbih_tick, 1)
    private val step = soundPool.load(context, R.raw.tesbih_step, 1)

    override fun play(effect: CounterEffect) {
        val sound = if (effect == CounterEffect.Tick) tick else step
        soundPool.play(sound, 1f, 1f, 1, 0, 1f)
    }

    private companion object {
        const val MAX_STREAMS = 4
    }
}
