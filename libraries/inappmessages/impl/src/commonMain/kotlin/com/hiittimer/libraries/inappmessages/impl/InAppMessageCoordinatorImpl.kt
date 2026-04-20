package com.dangerfield.hiittimer.libraries.inappmessages.impl

import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageCoordinator
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageTrigger
import com.dangerfield.hiittimer.libraries.inappmessages.MessageLastShownMsPref
import com.dangerfield.hiittimer.libraries.inappmessages.MessageShownCountPref
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class InAppMessageCoordinatorImpl(
    private val messages: Set<InAppMessage>,
    private val preferences: Preferences,
    private val clock: Clock,
) : InAppMessageCoordinator {

    private val logger = KLog.withTag("InAppMessages")
    private val mutex = Mutex()
    private val shownThisSession = mutableSetOf<String>()
    private val sessionCounts = mutableMapOf<String, Int>()

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
        val anyShown = shownThisSession.isNotEmpty()
        val now = clock.now().toEpochMilliseconds()
        val candidates = messages
            .filter { trigger in it.triggers }
            .sortedByDescending { it.priority }

        for (message in candidates) {
            if (message.suppressIfAnyShownThisSession && anyShown) continue
            if ((sessionCounts[message.id] ?: 0) >= message.maxPerSession) continue
            val lifetimeCap = message.maxLifetime
            if (lifetimeCap != null) {
                val shown = preferences.get(MessageShownCountPref(message.id))
                if (shown >= lifetimeCap) continue
            }
            val cooldown = message.cooldown
            if (cooldown != null) {
                val last = preferences.get(MessageLastShownMsPref(message.id))
                if (last > 0L && now - last < cooldown.inWholeMilliseconds) continue
            }
            if (!message.isEligible()) continue
            return message
        }
        return null
    }

    private suspend fun record(message: InAppMessage) {
        shownThisSession += message.id
        sessionCounts[message.id] = (sessionCounts[message.id] ?: 0) + 1
        val shownCountPref = MessageShownCountPref(message.id)
        preferences.set(shownCountPref, preferences.get(shownCountPref) + 1)
        preferences.set(MessageLastShownMsPref(message.id), clock.now().toEpochMilliseconds())
    }
}
