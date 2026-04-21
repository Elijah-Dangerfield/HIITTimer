package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.CueRole
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue

interface AudioCuePlayer {
    /** Master mute. When disabled, all cues are silent regardless of per-role mode. */
    fun setMasterEnabled(enabled: Boolean)
    fun setMode(role: CueRole, mode: SoundMode)
    fun setCuePack(role: CueRole, pack: SoundPack)
    /** Platform-specific voice identifier for TTS cues. Null/empty = system default. */
    fun setVoice(voiceId: String?)
    /** Layered on top of system volume. 0.0..1.0. */
    fun setVolume(volume: Float)
    fun play(cue: RunnerCue)
    fun release()
}

/**
 * Shared cue-to-role mapping. Any `RunnerCue` is classified into a [CueRole]
 * (which determines the sound pack to use) plus a [CueSample] (which file
 * within that pack).
 */
internal enum class CueSample { Short, Long, Finish }

internal data class CueRouting(val role: CueRole, val sample: CueSample)

internal fun RunnerCue.route(): CueRouting = when (this) {
    is RunnerCue.Countdown -> CueRouting(CueRole.Countdown, CueSample.Short)
    is RunnerCue.Prepare -> CueRouting(CueRole.Countdown, CueSample.Short)
    is RunnerCue.BlockStart -> CueRouting(CueRole.BlockStart, CueSample.Long)
    is RunnerCue.Halfway -> CueRouting(CueRole.Halfway, CueSample.Short)
    RunnerCue.Finish -> CueRouting(CueRole.Finish, CueSample.Finish)
}
