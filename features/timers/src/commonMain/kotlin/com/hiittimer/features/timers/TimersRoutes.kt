package com.dangerfield.hiittimer.features.timers

import com.dangerfield.hiittimer.libraries.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
class TimerListRoute : Route()

@Serializable
data class TimerBuilderRoute(val timerId: String? = null) : Route()

@Serializable
data class BlockEditRoute(val timerId: String, val blockId: String) : Route()

@Serializable
data class RunnerRoute(val timerId: String) : Route()
