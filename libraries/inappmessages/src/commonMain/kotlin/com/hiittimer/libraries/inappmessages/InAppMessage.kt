package com.dangerfield.hiittimer.libraries.inappmessages

import kotlin.time.Duration

interface InAppMessage {
    val id: String
    val priority: Int
    val triggers: Set<InAppMessageTrigger>
    val maxPerSession: Int
        get() = 1
    val maxLifetime: Int?
        get() = null
    val cooldown: Duration?
        get() = null
    val suppressIfAnyShownThisSession: Boolean
        get() = true

    suspend fun isEligible(): Boolean = true

    suspend fun show()
}
