package com.dangerfield.hiittimer

import com.dangerfield.hiittimer.features.timers.TimerListRoute
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.hiittimer.AppCache
import com.dangerfield.hiittimer.libraries.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * App-level ViewModel that decides the initial route and tracks whether the
 * iOS typewriter splash has already played this process.
 *
 * Scoped as singleton so Android's splash-screen API and the Compose overlay
 * share the same instance.
 */
@SingleIn(AppScope::class)
@Inject
class AppViewModel(
    private val appCache: AppCache,
) : SEAViewModel<AppState, AppEvent, AppAction>(AppState(startDestination = TimerListRoute())) {

    private val _isReady = MutableStateFlow(false)

    /**
     * Exposed to Android's splash screen API for keepOnScreenCondition.
     * True once we've determined where to navigate.
     */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        takeAction(AppAction.DetermineStartDestination)
    }

    override suspend fun handleAction(action: AppAction) {
        when (action) {
            AppAction.DetermineStartDestination -> {
                val destination: Route = TimerListRoute()

                action.updateState { it.copy(startDestination = destination) }
                _isReady.value = true
            }
            AppAction.MarkSplashShown -> {
                action.updateState { it.copy(hasShownSplash = true) }
            }
        }
    }
}

data class AppState(
    val startDestination: Route,
    val hasShownSplash: Boolean = false,
)

sealed class AppEvent

sealed class AppAction {
    data object DetermineStartDestination : AppAction()
    data object MarkSplashShown : AppAction()
}