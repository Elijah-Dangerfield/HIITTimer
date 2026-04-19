package com.dangerfield.hiittimer.features.timers.impl.runner

import android.content.Context
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidRunnerForegroundController @Inject constructor(
    private val context: Context,
) : RunnerForegroundController {
    override fun start() {
        TimerForegroundService.start(context)
    }

    override fun stop() {
        TimerForegroundService.stop(context)
    }
}
