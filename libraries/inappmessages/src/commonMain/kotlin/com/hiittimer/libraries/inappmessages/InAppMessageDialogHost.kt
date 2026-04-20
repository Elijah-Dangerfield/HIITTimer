package com.dangerfield.hiittimer.libraries.inappmessages

import kotlinx.coroutines.flow.StateFlow

interface InAppMessageDialogHost {
    val active: StateFlow<Active?>

    suspend fun present(dialog: InAppMessageDialog): InAppMessageDialogResult

    data class Active(
        val dialog: InAppMessageDialog,
        val onResult: (InAppMessageDialogResult) -> Unit,
    )
}
