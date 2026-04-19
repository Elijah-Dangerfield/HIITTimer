package com.dangerfield.hiittimer.features.timers.impl.runner

import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosRunnerForegroundController @Inject constructor() : RunnerForegroundController {
    override fun start() = Unit
    override fun stop() = Unit
}
