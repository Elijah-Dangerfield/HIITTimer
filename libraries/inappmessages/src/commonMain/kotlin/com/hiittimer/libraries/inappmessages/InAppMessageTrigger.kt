package com.dangerfield.hiittimer.libraries.inappmessages

sealed interface InAppMessageTrigger {
    data object WorkoutCompleted : InAppMessageTrigger
}
