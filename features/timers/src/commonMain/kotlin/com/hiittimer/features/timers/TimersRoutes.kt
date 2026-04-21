package com.dangerfield.hiittimer.features.timers

import com.dangerfield.hiittimer.libraries.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data object TimerListRoute : Route()

@Serializable
data object TimerPresetRoute : Route()

@Serializable
data class TimerDetailRoute(val timerId: String, val isNew: Boolean = false) : Route()

@Serializable
data class BlockEditRoute(val timerId: String, val blockId: String) : Route()

@Serializable
data class RunnerRoute(val timerId: String) : Route()
