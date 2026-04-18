package com.dangerfield.hiittimer.features.profile

import com.dangerfield.hiittimer.libraries.navigation.NavigableWhileBlocked
import com.dangerfield.hiittimer.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

@Serializable
data class BugReportRoute(
	val logId: String? = null,
	val errorCode: Int? = null,
	val contextMessage: String? = null,
) : TrackableRoute("bugReportScreenOpens"), NavigableWhileBlocked
