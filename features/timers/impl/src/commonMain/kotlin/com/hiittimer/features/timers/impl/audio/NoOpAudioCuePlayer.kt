package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.CueRole
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue

class NoOpAudioCuePlayer : AudioCuePlayer {
    override fun setMasterEnabled(enabled: Boolean) = Unit
    override fun setMode(role: CueRole, mode: SoundMode) = Unit
    override fun setCuePack(role: CueRole, pack: SoundPack) = Unit
    override fun setVoice(voiceId: String?) = Unit
    override fun setVolume(volume: Float) = Unit
    override fun play(cue: RunnerCue) = Unit
    override fun release() = Unit
}
