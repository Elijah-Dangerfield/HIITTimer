package com.dangerfield.hiittimer.features.settings.impl

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.CueVolumePref
import com.dangerfield.hiittimer.features.timers.HalfwayCalloutsPref
import com.dangerfield.hiittimer.features.timers.HapticsEnabledPref
import com.dangerfield.hiittimer.features.timers.ShowProgressBarPref
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundModePref
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.SoundPackPref
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
            val audio = combine(
                preferences.flow(SoundModePref),
                preferences.flow(SoundPackPref),
                preferences.flow(CueVolumePref),
                preferences.flow(HapticsEnabledPref),
            ) { modeStr, packStr, volume, haptics ->
                AudioSettings(
                    soundMode = runCatching { SoundMode.valueOf(modeStr) }.getOrDefault(SoundMode.Beeps),
                    soundPack = runCatching { SoundPack.valueOf(packStr) }.getOrDefault(SoundPack.Classic),
                    cueVolume = volume,
                    hapticsEnabled = haptics,
                )
            }

            combine(
                audio,
                preferences.flow(HalfwayCalloutsPref),
                preferences.flow(ShowProgressBarPref),
            ) { a, halfway, showProgress ->
                SettingsSnapshot(
                    audio = a,
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
                    soundMode = action.snapshot.audio.soundMode,
                    soundPack = action.snapshot.audio.soundPack,
                    cueVolume = action.snapshot.audio.cueVolume,
                    hapticsEnabled = action.snapshot.audio.hapticsEnabled,
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
            SettingsAction.CycleSoundPack -> {
                val packs = SoundPack.entries
                val next = packs[(packs.indexOf(state.soundPack) + 1) % packs.size]
                preferences.set(SoundPackPref, next.name)
            }
            is SettingsAction.SetCueVolume -> preferences.set(
                CueVolumePref,
                action.volume.coerceIn(0f, 1f),
            )
            is SettingsAction.SetHapticsEnabled -> preferences.set(
                HapticsEnabledPref,
                action.enabled,
            )
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

data class AudioSettings(
    val soundMode: SoundMode,
    val soundPack: SoundPack,
    val cueVolume: Float,
    val hapticsEnabled: Boolean,
)

data class SettingsSnapshot(
    val audio: AudioSettings,
    val halfwayCallouts: Boolean,
    val showProgressBar: Boolean,
)

data class SettingsState(
    val soundMode: SoundMode = SoundMode.Beeps,
    val soundPack: SoundPack = SoundPack.Classic,
    val cueVolume: Float = 0.8f,
    val hapticsEnabled: Boolean = true,
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
    data object CycleSoundPack : SettingsAction
    data class SetCueVolume(val volume: Float) : SettingsAction
    data class SetHapticsEnabled(val enabled: Boolean) : SettingsAction
    data class SetHalfwayCallouts(val enabled: Boolean) : SettingsAction
    data class SetShowProgressBar(val enabled: Boolean) : SettingsAction
    data object RateApp : SettingsAction
    data object OpenTipJar : SettingsAction
    data object ReportBug : SettingsAction
    data object LeaveFeedback : SettingsAction
}
