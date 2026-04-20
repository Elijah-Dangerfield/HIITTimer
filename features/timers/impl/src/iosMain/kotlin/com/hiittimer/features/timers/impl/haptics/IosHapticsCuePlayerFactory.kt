package com.dangerfield.hiittimer.features.timers.impl.haptics

import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
import me.tatarka.inject.annotations.Inject
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosHapticsCuePlayerFactory @Inject constructor() : HapticsCuePlayerFactory {
    override fun create(): HapticsCuePlayer = IosHapticsCuePlayer()
}

private class IosHapticsCuePlayer : HapticsCuePlayer {

    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notification = UINotificationFeedbackGenerator()

    private var enabled: Boolean = true

    init {
        // Pre-warm — reduces latency of the first haptic event by a noticeable margin.
        light.prepare()
        medium.prepare()
        heavy.prepare()
        notification.prepare()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun play(cue: RunnerCue) {
        if (!enabled) return
        when (cue) {
            is RunnerCue.Countdown -> light.impactOccurred()
            is RunnerCue.Prepare -> if (cue.phase == 1) heavy.impactOccurred() else light.impactOccurred()
            is RunnerCue.BlockStart -> medium.impactOccurred()
            is RunnerCue.Halfway -> light.impactOccurred()
            RunnerCue.Finish -> notification.notificationOccurred(
                UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
            )
        }
    }

    override fun release() = Unit
}
