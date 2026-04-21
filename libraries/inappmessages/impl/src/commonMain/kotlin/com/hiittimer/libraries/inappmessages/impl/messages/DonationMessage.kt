package com.dangerfield.hiittimer.libraries.inappmessages.impl.messages

import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageContext
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialog
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogHost
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogImage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogResult
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageTrigger
import com.dangerfield.hiittimer.libraries.navigation.Router
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.donation_copy_01_body
import rounds.libraries.resources.generated.resources.donation_copy_01_title
import rounds.libraries.resources.generated.resources.donation_copy_02_body
import rounds.libraries.resources.generated.resources.donation_copy_02_title
import rounds.libraries.resources.generated.resources.donation_copy_03_body
import rounds.libraries.resources.generated.resources.donation_copy_03_title
import rounds.libraries.resources.generated.resources.donation_copy_04_body
import rounds.libraries.resources.generated.resources.donation_copy_04_title
import rounds.libraries.resources.generated.resources.donation_copy_05_body
import rounds.libraries.resources.generated.resources.donation_copy_05_title
import rounds.libraries.resources.generated.resources.donation_copy_06_body
import rounds.libraries.resources.generated.resources.donation_copy_06_title
import rounds.libraries.resources.generated.resources.donation_copy_07_body
import rounds.libraries.resources.generated.resources.donation_copy_07_title
import rounds.libraries.resources.generated.resources.donation_copy_08_body
import rounds.libraries.resources.generated.resources.donation_copy_08_title
import rounds.libraries.resources.generated.resources.donation_copy_09_body
import rounds.libraries.resources.generated.resources.donation_copy_09_title
import rounds.libraries.resources.generated.resources.donation_copy_10_body
import rounds.libraries.resources.generated.resources.donation_copy_10_title
import rounds.libraries.resources.generated.resources.donation_copy_11_body
import rounds.libraries.resources.generated.resources.donation_copy_11_title
import rounds.libraries.resources.generated.resources.donation_copy_12_body
import rounds.libraries.resources.generated.resources.donation_copy_12_title
import rounds.libraries.resources.generated.resources.donation_copy_13_body
import rounds.libraries.resources.generated.resources.donation_copy_13_title
import rounds.libraries.resources.generated.resources.donation_copy_14_body
import rounds.libraries.resources.generated.resources.donation_copy_14_title
import rounds.libraries.resources.generated.resources.donation_copy_15_body
import rounds.libraries.resources.generated.resources.donation_copy_15_title
import rounds.libraries.resources.generated.resources.donation_copy_16_body
import rounds.libraries.resources.generated.resources.donation_copy_16_title
import rounds.libraries.resources.generated.resources.donation_copy_17_body
import rounds.libraries.resources.generated.resources.donation_copy_17_title
import rounds.libraries.resources.generated.resources.donation_copy_18_body
import rounds.libraries.resources.generated.resources.donation_copy_18_title
import rounds.libraries.resources.generated.resources.donation_copy_19_body
import rounds.libraries.resources.generated.resources.donation_copy_19_title
import rounds.libraries.resources.generated.resources.donation_copy_20_body
import rounds.libraries.resources.generated.resources.donation_copy_20_title
import rounds.libraries.resources.generated.resources.donation_negative
import rounds.libraries.resources.generated.resources.donation_positive
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration.Companion.days

private const val MIN_COMPLETED_WORKOUTS = 7
private const val MAX_LIFETIME_SHOWS = 6
private const val MIN_WORKOUTS_BETWEEN_ANY_MESSAGES = 2
private val MIN_TIME_BETWEEN_SHOWS = 14.days
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

    override suspend fun canShow(context: InAppMessageContext): Boolean {
        if (context.workoutsCompleted < MIN_COMPLETED_WORKOUTS) return false
        if (context.thisShownCount >= MAX_LIFETIME_SHOWS) return false
        // don't stack donation right after any other notification
        if (context.workoutsSinceLastAnyShown < MIN_WORKOUTS_BETWEEN_ANY_MESSAGES) return false
        val sinceSelf = context.thisLastShownAt?.let { context.now - it }
        if (sinceSelf != null && sinceSelf < MIN_TIME_BETWEEN_SHOWS) return false
        return true
    }

    override suspend fun show() {
        val copy = pickCopy()
        val result = dialogHost.present(
            InAppMessageDialog(
                title = copy.title,
                body = copy.body,
                image = InAppMessageDialogImage.CreatorHeadshot,
                positiveLabel = copy.positiveLabel,
                negativeLabel = copy.negativeLabel,
            )
        )
        if (result == InAppMessageDialogResult.Positive) {
            router.openWebLink(TIP_JAR_URL)
        }
    }

    /** Cycle through copy deterministically. The coordinator has already incremented this
     *  message's persisted count by the time show() runs, so subtract one to put the first
     *  ever show at index 0. */
    private suspend fun pickCopy(): DonationCopy {
        val count = preferences.get(
            com.dangerfield.hiittimer.libraries.inappmessages.MessageShownCountPref(id)
        )
        val index = ((count - 1).coerceAtLeast(0)) % DONATION_COPY_RESOURCES.size
        val (titleRes, bodyRes) = DONATION_COPY_RESOURCES[index]
        return DonationCopy(
            title = getString(titleRes),
            body = getString(bodyRes),
            positiveLabel = getString(AppRes.string.donation_positive),
            negativeLabel = getString(AppRes.string.donation_negative),
        )
    }
}

private data class DonationCopy(
    val title: String,
    val body: String,
    val positiveLabel: String,
    val negativeLabel: String,
)

private val DONATION_COPY_RESOURCES: List<Pair<StringResource, StringResource>> = listOf(
    AppRes.string.donation_copy_01_title to AppRes.string.donation_copy_01_body,
    AppRes.string.donation_copy_02_title to AppRes.string.donation_copy_02_body,
    AppRes.string.donation_copy_03_title to AppRes.string.donation_copy_03_body,
    AppRes.string.donation_copy_04_title to AppRes.string.donation_copy_04_body,
    AppRes.string.donation_copy_05_title to AppRes.string.donation_copy_05_body,
    AppRes.string.donation_copy_06_title to AppRes.string.donation_copy_06_body,
    AppRes.string.donation_copy_07_title to AppRes.string.donation_copy_07_body,
    AppRes.string.donation_copy_08_title to AppRes.string.donation_copy_08_body,
    AppRes.string.donation_copy_09_title to AppRes.string.donation_copy_09_body,
    AppRes.string.donation_copy_10_title to AppRes.string.donation_copy_10_body,
    AppRes.string.donation_copy_11_title to AppRes.string.donation_copy_11_body,
    AppRes.string.donation_copy_12_title to AppRes.string.donation_copy_12_body,
    AppRes.string.donation_copy_13_title to AppRes.string.donation_copy_13_body,
    AppRes.string.donation_copy_14_title to AppRes.string.donation_copy_14_body,
    AppRes.string.donation_copy_15_title to AppRes.string.donation_copy_15_body,
    AppRes.string.donation_copy_16_title to AppRes.string.donation_copy_16_body,
    AppRes.string.donation_copy_17_title to AppRes.string.donation_copy_17_body,
    AppRes.string.donation_copy_18_title to AppRes.string.donation_copy_18_body,
    AppRes.string.donation_copy_19_title to AppRes.string.donation_copy_19_body,
    AppRes.string.donation_copy_20_title to AppRes.string.donation_copy_20_body,
)
