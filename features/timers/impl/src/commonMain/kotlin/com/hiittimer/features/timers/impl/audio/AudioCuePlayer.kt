package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue

interface AudioCuePlayer {
    fun setMode(mode: SoundMode)
    fun play(cue: RunnerCue)
    fun release()
}
