package com.dangerfield.hiittimer

import com.dangerfield.hiittimer.features.settings.ShakeToReportEnabledPref
import com.dangerfield.hiittimer.libraries.core.ShakeDetector
import com.dangerfield.hiittimer.libraries.hiittimer.UserRepository
import com.dangerfield.hiittimer.libraries.navigation.Router
import com.dangerfield.hiittimer.libraries.navigation.ShakeDialogRoute
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.concurrent.Volatile

@Inject
@SingleIn(AppScope::class)
class ShakeHandler(
    private val shakeDetector: ShakeDetector,
    private val userRepository: UserRepository,
    private val router: Router,
    private val preferences: Preferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var isShowingDialog = false

    fun start() {
        shakeDetector.start()
        scope.launch {
            shakeDetector.shakeEvents.collect {
                handleShake()
            }
        }
    }

    fun stop() {
        shakeDetector.stop()
    }

    /** Called by the dialog entry point on every close path (dismiss, report-bug, don't-show-again)
     *  so the next shake can surface the dialog again. Without this, the detector would appear
     *  to fire only once per app session. */
    fun onDialogDismissed() {
        isShowingDialog = false
    }

    /** Disable future shake dialogs. Runs on the handler's app-scoped coroutine so the write
     *  completes even after the dialog composable (and its scope) is torn down. */
    fun onDontShowAgain() {
        isShowingDialog = false
        scope.launch {
            preferences.set(ShakeToReportEnabledPref, false)
        }
    }

    private suspend fun handleShake() {
        if (isShowingDialog) return
        if (!preferences.get(ShakeToReportEnabledPref)) return
        isShowingDialog = true
        router.navigate(ShakeDialogRoute())
        userRepository.onShakeDetected()
    }
}
