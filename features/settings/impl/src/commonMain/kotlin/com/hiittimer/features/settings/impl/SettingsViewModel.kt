package com.dangerfield.hiittimer.features.settings.impl

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.HalfwayCalloutsPref
import com.dangerfield.hiittimer.features.timers.ShowProgressBarPref
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundModePref
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import com.dangerfield.hiittimer.libraries.review.RequestReviewIfPossible
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsViewModel(
    private val preferences: Preferences,
    private val requestReview: RequestReviewIfPossible,
) : SEAViewModel<SettingsState, SettingsEvent, SettingsAction>(initialStateArg = SettingsState()) {

    init {
        viewModelScope.launch {
            combine(
                preferences.flow(SoundModePref),
                preferences.flow(HalfwayCalloutsPref),
                preferences.flow(ShowProgressBarPref),
            ) { soundModeStr, halfway, showProgress ->
                SettingsSnapshot(
                    soundMode = runCatching { SoundMode.valueOf(soundModeStr) }.getOrDefault(SoundMode.Beeps),
                    halfwayCallouts = halfway,
                    showProgressBar = showProgress,
                )
            }.collect { snapshot ->
                takeAction(SettingsAction.Receive(snapshot))
            }
        }
    }

    override suspend fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.Receive -> action.updateState {
                it.copy(
                    soundMode = action.snapshot.soundMode,
                    halfwayCallouts = action.snapshot.halfwayCallouts,
                    showProgressBar = action.snapshot.showProgressBar,
                )
            }
            is SettingsAction.SetSoundMode -> preferences.set(SoundModePref, action.mode.name)
            is SettingsAction.SetHalfwayCallouts -> preferences.set(HalfwayCalloutsPref, action.enabled)
            is SettingsAction.SetShowProgressBar -> preferences.set(ShowProgressBarPref, action.enabled)
            SettingsAction.RateApp -> requestReview.invoke()
            SettingsAction.OpenTipJar -> sendEvent(SettingsEvent.OpenUrl(TIP_JAR_URL))
        }
    }

    companion object {
        private const val TIP_JAR_URL = "https://www.buymeacoffee.com/"
    }
}

data class SettingsSnapshot(
    val soundMode: SoundMode,
    val halfwayCallouts: Boolean,
    val showProgressBar: Boolean,
)

data class SettingsState(
    val soundMode: SoundMode = SoundMode.Beeps,
    val halfwayCallouts: Boolean = false,
    val showProgressBar: Boolean = true,
)

sealed interface SettingsEvent {
    data class OpenUrl(val url: String) : SettingsEvent
}

sealed interface SettingsAction {
    data class Receive(val snapshot: SettingsSnapshot) : SettingsAction
    data class SetSoundMode(val mode: SoundMode) : SettingsAction
    data class SetHalfwayCallouts(val enabled: Boolean) : SettingsAction
    data class SetShowProgressBar(val enabled: Boolean) : SettingsAction
    data object RateApp : SettingsAction
    data object OpenTipJar : SettingsAction
}
