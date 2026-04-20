package com.dangerfield.hiittimer.features.timers.impl.haptics

import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue

/**
 * Fires device haptics for runner cues. Independent of [AudioCuePlayer] so
 * users can have vibration in addition to, or instead of, sound.
 */
interface HapticsCuePlayer {
    fun setEnabled(enabled: Boolean)
    fun play(cue: RunnerCue)
    fun release()
}

interface HapticsCuePlayerFactory {
    fun create(): HapticsCuePlayer
}
