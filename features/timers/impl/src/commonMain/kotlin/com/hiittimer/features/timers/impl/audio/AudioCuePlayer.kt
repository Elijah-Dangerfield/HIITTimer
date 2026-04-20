package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue

interface AudioCuePlayer {
    fun setMode(mode: SoundMode)
    fun setSoundPack(pack: SoundPack)
    /** Layered on top of system volume. 0.0..1.0. */
    fun setVolume(volume: Float)
    fun play(cue: RunnerCue)
    fun release()
}
