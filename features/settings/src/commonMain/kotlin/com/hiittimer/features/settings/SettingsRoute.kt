package com.dangerfield.hiittimer.features.settings

import com.dangerfield.hiittimer.libraries.navigation.NavigableWhileBlocked
import com.dangerfield.hiittimer.libraries.navigation.Route
import com.dangerfield.hiittimer.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

@Serializable
class SettingsRoute : Route()

@Serializable
class FeedbackRoute : TrackableRoute("feedbackScreenOpens")

@Serializable
data class BugReportRoute(
    val logId: String? = null,
    val errorCode: Int? = null,
    val contextMessage: String? = null,
) : TrackableRoute("bugReportScreenOpens"), NavigableWhileBlocked

