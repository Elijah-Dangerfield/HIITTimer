package com.dangerfield.hiittimer.libraries.inappmessages.impl.messages

import com.dangerfield.hiittimer.features.timers.CompletedWorkoutsPref
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialog
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogHost
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogImage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogResult
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageTrigger
import com.dangerfield.hiittimer.libraries.navigation.Router
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration.Companion.days

private const val MIN_COMPLETED_WORKOUTS = 10
private const val TIP_JAR_URL = "https://buymeacoffee.com/elidangerfield"

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class DonationMessage(
    private val preferences: Preferences,
    private val dialogHost: InAppMessageDialogHost,
    private val router: Router,
) : InAppMessage {

    override val id: String = "donation"
    override val priority: Int = 5
    override val triggers: Set<InAppMessageTrigger> = setOf(InAppMessageTrigger.WorkoutCompleted)
    override val maxLifetime: Int = 2
    override val cooldown = 30.days
    override val suppressIfAnyShownThisSession: Boolean = true

    override suspend fun isEligible(): Boolean {
        val completed = preferences.get(CompletedWorkoutsPref)
        return completed >= MIN_COMPLETED_WORKOUTS
    }

    override suspend fun show() {
        val result = dialogHost.present(
            InAppMessageDialog(
                title = "I refuse to charge for this app.",
                body = "But if you like it, please consider donating.",
                image = InAppMessageDialogImage.CreatorHeadshot,
                positiveLabel = "I'm a cool person, I'll donate",
                negativeLabel = "No, I'm not cool",
            )
        )
        if (result == InAppMessageDialogResult.Positive) {
            router.openWebLink(TIP_JAR_URL)
        }
    }
}
