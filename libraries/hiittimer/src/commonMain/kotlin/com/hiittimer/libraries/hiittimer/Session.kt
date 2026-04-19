package com.dangerfield.hiittimer.libraries.hiittimer

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a user session - a contiguous period of app usage.
 */
@Serializable
data class Session(
    val id: String,
    val sessionNumber: Int,
    val startedAt: Instant,
    val endedAt: Instant? = null,
)
