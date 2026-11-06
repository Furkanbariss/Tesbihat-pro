package com.furkanbarissonmezisik.tesbihatpro.core.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEffect

interface HapticsController {
    fun perform(effect: CounterEffect)
}

class VibratorHapticsController(context: Context) : HapticsController {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }

    private val tick: VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    private val doublePulse = VibrationEffect.createWaveform(longArrayOf(0, 45, 70, 45), -1)
    private val longPattern =
        VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120, 80, 260), -1)

    override fun perform(effect: CounterEffect) {
        vibrator.vibrate(
            when (effect) {
                CounterEffect.Tick -> tick
                is CounterEffect.StepCompleted, CounterEffect.Milestone -> doublePulse
                CounterEffect.SequenceCompleted -> longPattern
            },
        )
    }
}
