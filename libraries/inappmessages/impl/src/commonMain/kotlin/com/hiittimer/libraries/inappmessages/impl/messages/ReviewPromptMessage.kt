package com.dangerfield.hiittimer.libraries.inappmessages.impl.messages

import com.dangerfield.hiittimer.features.timers.CompletedWorkoutsPref
import com.dangerfield.hiittimer.features.timers.ReviewPromptShownPref
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageTrigger
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import com.dangerfield.hiittimer.libraries.review.RequestReviewIfPossible
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

private const val MIN_COMPLETED_WORKOUTS = 3

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class ReviewPromptMessage(
    private val preferences: Preferences,
    private val requestReviewIfPossible: RequestReviewIfPossible,
) : InAppMessage {

    override val id: String = "review"
    override val priority: Int = 10
    override val triggers: Set<InAppMessageTrigger> = setOf(InAppMessageTrigger.WorkoutCompleted)
    override val maxLifetime: Int = 1
    override val suppressIfAnyShownThisSession: Boolean = false

    override suspend fun isEligible(): Boolean {
        if (preferences.get(ReviewPromptShownPref)) return false
        val completed = preferences.get(CompletedWorkoutsPref)
        return completed >= MIN_COMPLETED_WORKOUTS
    }

    override suspend fun show() {
        preferences.set(ReviewPromptShownPref, true)
        requestReviewIfPossible.invoke()
    }
}
