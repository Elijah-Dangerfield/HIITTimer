package com.dangerfield.hiittimer.features.settings.impl

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.HalfwayCalloutsPref
import com.dangerfield.hiittimer.features.timers.ShowProgressBarPref
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundModePref
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.hiittimer.AppInfo
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsViewModel(
    private val preferences: Preferences,
    private val appInfo: AppInfo,
) : SEAViewModel<SettingsState, SettingsEvent, SettingsAction>(
    initialStateArg = SettingsState(
        versionName = appInfo.versionName,
        buildNumber = appInfo.buildNumber,
    ),
) {

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
            SettingsAction.CycleSoundMode -> {
                val next = when (state.soundMode) {
                    SoundMode.Off -> SoundMode.Beeps
                    SoundMode.Beeps -> SoundMode.Voice
                    SoundMode.Voice -> SoundMode.Off
                }
                preferences.set(SoundModePref, next.name)
            }
            is SettingsAction.SetHalfwayCallouts -> preferences.set(HalfwayCalloutsPref, action.enabled)
            is SettingsAction.SetShowProgressBar -> preferences.set(ShowProgressBarPref, action.enabled)
            SettingsAction.RateApp -> sendEvent(SettingsEvent.OpenUrl(APP_STORE_URL))
            SettingsAction.OpenTipJar -> sendEvent(SettingsEvent.OpenUrl(TIP_JAR_URL))
            SettingsAction.ReportBug -> sendEvent(SettingsEvent.NavigateToBugReport)
            SettingsAction.LeaveFeedback -> sendEvent(SettingsEvent.NavigateToFeedback)
        }
    }

    companion object {
        const val TIP_JAR_URL = "https://buymeacoffee.com/elidangerfield"
        private const val APP_STORE_URL = "https://apps.apple.com/app/id6762529965"
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
    val versionName: String = "",
    val buildNumber: Int = 0,
)

sealed interface SettingsEvent {
    data class OpenUrl(val url: String) : SettingsEvent
    data object NavigateToFeedback : SettingsEvent
    data object NavigateToBugReport : SettingsEvent
}

sealed interface SettingsAction {
    data class Receive(val snapshot: SettingsSnapshot) : SettingsAction
    data class SetSoundMode(val mode: SoundMode) : SettingsAction
    data object CycleSoundMode : SettingsAction
    data class SetHalfwayCallouts(val enabled: Boolean) : SettingsAction
    data class SetShowProgressBar(val enabled: Boolean) : SettingsAction
    data object RateApp : SettingsAction
    data object OpenTipJar : SettingsAction
    data object ReportBug : SettingsAction
    data object LeaveFeedback : SettingsAction
}
