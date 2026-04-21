package com.dangerfield.hiittimer.features.settings

import com.dangerfield.hiittimer.libraries.navigation.NavigableWhileBlocked
import com.dangerfield.hiittimer.libraries.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
class SettingsRoute : Route()

@Serializable
class SoundSettingsRoute : Route()

@Serializable
class FeedbackRoute : Route()

@Serializable
data class BugReportRoute(
    val logId: String? = null,
    val errorCode: Int? = null,
    val contextMessage: String? = null,
) : Route(), NavigableWhileBlocked

