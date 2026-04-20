package com.dangerfield.hiittimer.features.timers.impl.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidHapticsCuePlayerFactory @Inject constructor(
    private val context: Context,
) : HapticsCuePlayerFactory {
    override fun create(): HapticsCuePlayer = AndroidHapticsCuePlayer(context)
}

private class AndroidHapticsCuePlayer(context: Context) : HapticsCuePlayer {

    private val vibrator: Vibrator? = resolveVibrator(context)
    private var enabled: Boolean = true

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun play(cue: RunnerCue) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        when (cue) {
            is RunnerCue.Countdown -> v.vibrateEffect(durationMs = 35, amplitude = 80)
            is RunnerCue.Prepare -> if (cue.phase == 1) {
                v.vibrateEffect(durationMs = 120, amplitude = 255)
            } else {
                v.vibrateEffect(durationMs = 35, amplitude = 80)
            }
            is RunnerCue.BlockStart -> v.vibrateEffect(durationMs = 80, amplitude = 200)
            is RunnerCue.Halfway -> v.vibrateEffect(durationMs = 35, amplitude = 80)
            RunnerCue.Finish -> v.vibratePattern(
                // double-tap ish: pause 0, buzz 120, pause 80, buzz 200
                timings = longArrayOf(0, 120, 80, 200),
                amplitudes = intArrayOf(0, 200, 0, 255),
            )
        }
    }

    override fun release() = Unit
}

private fun resolveVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

private fun Vibrator.vibrateEffect(durationMs: Long, amplitude: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val clamped = amplitude.coerceIn(1, 255)
        vibrate(VibrationEffect.createOneShot(durationMs, clamped))
    } else {
        @Suppress("DEPRECATION")
        vibrate(durationMs)
    }
}

private fun Vibrator.vibratePattern(timings: LongArray, amplitudes: IntArray) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, /* repeat = */ -1))
    } else {
        @Suppress("DEPRECATION")
        vibrate(timings, -1)
    }
}
