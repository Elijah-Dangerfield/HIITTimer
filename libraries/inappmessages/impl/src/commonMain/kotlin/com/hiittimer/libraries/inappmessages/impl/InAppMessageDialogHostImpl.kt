package com.dangerfield.hiittimer.libraries.inappmessages.impl

import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialog
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogHost
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.coroutines.resume

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class InAppMessageDialogHostImpl : InAppMessageDialogHost {

    private val _active = MutableStateFlow<InAppMessageDialogHost.Active?>(null)
    override val active: StateFlow<InAppMessageDialogHost.Active?> = _active.asStateFlow()

    override suspend fun present(dialog: InAppMessageDialog): InAppMessageDialogResult =
        suspendCancellableCoroutine { continuation ->
            var resumed = false
            val callback = { result: InAppMessageDialogResult ->
                if (!resumed) {
                    resumed = true
                    _active.value = null
                    if (continuation.isActive) continuation.resume(result)
                }
            }
            _active.value = InAppMessageDialogHost.Active(dialog, callback)
            continuation.invokeOnCancellation {
                if (!resumed) {
                    _active.value = null
                }
            }
        }
}
