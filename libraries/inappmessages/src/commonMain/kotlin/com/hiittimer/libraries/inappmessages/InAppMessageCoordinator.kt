package com.dangerfield.hiittimer.libraries.inappmessages

interface InAppMessageCoordinator {
    suspend fun tryShow(trigger: InAppMessageTrigger)
}
