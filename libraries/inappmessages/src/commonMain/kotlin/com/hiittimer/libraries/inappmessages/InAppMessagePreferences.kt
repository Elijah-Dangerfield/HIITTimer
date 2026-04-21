package com.dangerfield.hiittimer.libraries.inappmessages

import com.dangerfield.hiittimer.libraries.preferences.Preference

class MessageShownCountPref(messageId: String) : Preference<Int>() {
    override val key: String = "app.inappmessage.$messageId.shown_count"
    override val default: Int = 0
}

class MessageLastShownMsPref(messageId: String) : Preference<Long>() {
    override val key: String = "app.inappmessage.$messageId.last_shown_ms"
    override val default: Long = 0L
}

/** Timestamp (epoch ms) of the most recent show of any in-app message. 0 means never. */
object LastAnyShownAtMsPref : Preference<Long>() {
    override val key: String = "app.inappmessage.last_any.shown_ms"
    override val default: Long = 0L
}

/** Id of the most recent message shown, or empty if never. */
object LastAnyShownIdPref : Preference<String>() {
    override val key: String = "app.inappmessage.last_any.id"
    override val default: String = ""
}

/** Snapshot of CompletedWorkoutsPref at the time of the most recent show. Used to derive
 *  `workoutsSinceLastAnyShown` without needing a separate counter. */
object WorkoutCountAtLastAnyShownPref : Preference<Int>() {
    override val key: String = "app.inappmessage.last_any.workout_count"
    override val default: Int = 0
}
