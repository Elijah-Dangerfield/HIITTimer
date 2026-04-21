package com.dangerfield.hiittimer.libraries.inappmessages

import kotlin.time.Instant

interface InAppMessage {
    val id: String
    val priority: Int
    val triggers: Set<InAppMessageTrigger>

    /**
     * Return true if this message is allowed to show given the global + per-message history
     * in [context]. Each message owns all its own gating logic (thresholds, cooldowns,
     * anti-spam, lifetime caps). The coordinator picks the first eligible candidate in
     * descending priority order.
     */
    suspend fun canShow(context: InAppMessageContext): Boolean

    suspend fun show()
}

/**
 * Snapshot of the show-history state, built by the coordinator before asking each candidate
 * whether it wants to show. All time math is done against [now].
 */
data class InAppMessageContext(
    val now: Instant,
    val trigger: InAppMessageTrigger,
    /** When the most recent message of any id was shown. Null if no message has ever been shown. */
    val lastAnyShownAt: Instant?,
    /** The id of the most recent message shown. Null if no message has ever been shown. */
    val lastAnyShownId: String?,
    /** When THIS message was last shown. Null if this message has never been shown. */
    val thisLastShownAt: Instant?,
    /** Lifetime show count for THIS message. */
    val thisShownCount: Int,
    /** Total workouts the user has completed. */
    val workoutsCompleted: Int,
    /** Workouts completed since the most recent show of any message. Equals [workoutsCompleted]
     *  if nothing has ever been shown. */
    val workoutsSinceLastAnyShown: Int,
)
