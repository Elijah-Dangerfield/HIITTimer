package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue

class NoOpAudioCuePlayer : AudioCuePlayer {
    override fun setMode(mode: SoundMode) = Unit
    override fun setSoundPack(pack: SoundPack) = Unit
    override fun setVolume(volume: Float) = Unit
    override fun play(cue: RunnerCue) = Unit
    override fun release() = Unit
}
