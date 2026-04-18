package com.dangerfield.hiittimer.features.timers.impl.audio

import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosAudioCuePlayerFactory @Inject constructor() : AudioCuePlayerFactory {
    override fun create(): AudioCuePlayer = NoOpAudioCuePlayer()
}
