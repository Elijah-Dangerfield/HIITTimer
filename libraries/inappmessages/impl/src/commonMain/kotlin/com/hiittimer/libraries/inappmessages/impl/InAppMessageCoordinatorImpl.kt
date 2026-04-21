package com.dangerfield.hiittimer.libraries.inappmessages.impl

import com.dangerfield.hiittimer.features.timers.CompletedWorkoutsPref
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import com.dangerfield.hiittimer.libraries.hiittimer.AppEvent
import com.dangerfield.hiittimer.libraries.hiittimer.AppEventListener
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageContext
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageCoordinator
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageTrigger
import com.dangerfield.hiittimer.libraries.inappmessages.LastAnyShownAtMsPref
import com.dangerfield.hiittimer.libraries.inappmessages.LastAnyShownIdPref
import com.dangerfield.hiittimer.libraries.inappmessages.MessageLastShownMsPref
import com.dangerfield.hiittimer.libraries.inappmessages.MessageShownCountPref
import com.dangerfield.hiittimer.libraries.inappmessages.WorkoutCountAtLastAnyShownPref
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock
import kotlin.time.Instant

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = InAppMessageCoordinator::class)
@ContributesBinding(AppScope::class, multibinding = true, boundType = AppEventListener::class)
@Inject
class InAppMessageCoordinatorImpl(
    private val messages: Set<InAppMessage>,
    private val preferences: Preferences,
    private val clock: Clock,
    private val appScope: AppCoroutineScope,
) : InAppMessageCoordinator, AppEventListener {

    private val logger = KLog.withTag("InAppMessages")
    private val mutex = Mutex()
    private val shownThisForeground = mutableSetOf<String>()

    override fun onForeground(event: AppEvent.OnForeground) {
        if (event.isColdBoot) return
        appScope.launch {
            mutex.withLock { shownThisForeground.clear() }
        }
    }

    override suspend fun tryShow(trigger: InAppMessageTrigger) {
        mutex.withLock {
            val message = pickEligible(trigger) ?: return
            record(message)
            try {
                message.show()
            } catch (error: Throwable) {
                logger.e(error) { "Message ${message.id} show() threw" }
            }
        }
    }

    private suspend fun pickEligible(trigger: InAppMessageTrigger): InAppMessage? {
        val now = clock.now()
        val lastAnyShownAt = preferences.get(LastAnyShownAtMsPref).toInstantOrNull()
        val lastAnyShownId = preferences.get(LastAnyShownIdPref).takeIf { it.isNotEmpty() }
        val workoutsCompleted = preferences.get(CompletedWorkoutsPref)
        val workoutCountAtLastShown = preferences.get(WorkoutCountAtLastAnyShownPref)
        val workoutsSinceLastAnyShown = if (lastAnyShownAt == null) {
            workoutsCompleted
        } else {
            (workoutsCompleted - workoutCountAtLastShown).coerceAtLeast(0)
        }

        val candidates = messages
            .filter { trigger in it.triggers }
            .filter { it.id !in shownThisForeground }
            .sortedByDescending { it.priority }

        for (message in candidates) {
            val context = InAppMessageContext(
                now = now,
                trigger = trigger,
                lastAnyShownAt = lastAnyShownAt,
                lastAnyShownId = lastAnyShownId,
                thisLastShownAt = preferences.get(MessageLastShownMsPref(message.id)).toInstantOrNull(),
                thisShownCount = preferences.get(MessageShownCountPref(message.id)),
                workoutsCompleted = workoutsCompleted,
                workoutsSinceLastAnyShown = workoutsSinceLastAnyShown,
            )
            val allowed = try {
                message.canShow(context)
            } catch (error: Throwable) {
                logger.e(error) { "Message ${message.id} canShow() threw" }
                false
            }
            if (allowed) return message
        }
        return null
    }

    private suspend fun record(message: InAppMessage) {
        val nowMs = clock.now().toEpochMilliseconds()
        shownThisForeground += message.id
        val shownCountPref = MessageShownCountPref(message.id)
        preferences.set(shownCountPref, preferences.get(shownCountPref) + 1)
        preferences.set(MessageLastShownMsPref(message.id), nowMs)
        preferences.set(LastAnyShownAtMsPref, nowMs)
        preferences.set(LastAnyShownIdPref, message.id)
        preferences.set(WorkoutCountAtLastAnyShownPref, preferences.get(CompletedWorkoutsPref))
    }
}

private fun Long.toInstantOrNull(): Instant? =
    if (this <= 0L) null else Instant.fromEpochMilliseconds(this)
