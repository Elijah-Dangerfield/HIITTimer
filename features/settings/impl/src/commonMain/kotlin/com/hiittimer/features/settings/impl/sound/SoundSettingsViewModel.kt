package com.dangerfield.hiittimer.features.settings.impl.sound

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.CueRole
import com.dangerfield.hiittimer.features.timers.CueVolumePref
import com.dangerfield.hiittimer.features.timers.HalfwayCalloutsPref
import com.dangerfield.hiittimer.features.timers.SoundCueType
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.SoundPreviewPlayer
import com.dangerfield.hiittimer.features.timers.SoundsEnabledPref
import com.dangerfield.hiittimer.features.timers.VoiceCatalog
import com.dangerfield.hiittimer.features.timers.VoiceIdPref
import com.dangerfield.hiittimer.features.timers.VoiceOption
import com.dangerfield.hiittimer.features.timers.modePref
import com.dangerfield.hiittimer.features.timers.pref
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class SoundSettingsViewModel(
    private val preferences: Preferences,
    private val previewPlayer: SoundPreviewPlayer,
    private val voiceCatalog: VoiceCatalog,
) : SEAViewModel<SoundSettingsState, SoundSettingsEvent, SoundSettingsAction>(
    initialStateArg = SoundSettingsState()
) {

    init {
        viewModelScope.launch {
            val voices = runCatching { voiceCatalog.availableVoices() }.getOrDefault(emptyList())
            takeAction(SoundSettingsAction.ReceiveVoices(voices))
        }
        viewModelScope.launch {
            preferences.flow(VoiceIdPref).collect { id ->
                takeAction(SoundSettingsAction.ReceiveVoiceId(id))
            }
        }
        viewModelScope.launch {
            val coreFlow = combine(
                preferences.flow(SoundsEnabledPref),
                preferences.flow(CueVolumePref),
                preferences.flow(HalfwayCalloutsPref),
            ) { enabled, volume, halfway ->
                Triple(enabled, volume, halfway)
            }
            val modesFlow = combine(
                preferences.flow(CueRole.Countdown.modePref()),
                preferences.flow(CueRole.BlockStart.modePref()),
                preferences.flow(CueRole.Halfway.modePref()),
                preferences.flow(CueRole.Finish.modePref()),
            ) { countdown, blockStart, halfway, finish ->
                mapOf(
                    CueRole.Countdown to parseMode(countdown),
                    CueRole.BlockStart to parseMode(blockStart),
                    CueRole.Halfway to parseMode(halfway),
                    CueRole.Finish to parseMode(finish),
                )
            }
            val packsFlow = combine(
                preferences.flow(CueRole.Countdown.pref()),
                preferences.flow(CueRole.BlockStart.pref()),
                preferences.flow(CueRole.Halfway.pref()),
                preferences.flow(CueRole.Finish.pref()),
            ) { countdown, blockStart, halfwayPack, finish ->
                mapOf(
                    CueRole.Countdown to parsePack(countdown),
                    CueRole.BlockStart to parsePack(blockStart),
                    CueRole.Halfway to parsePack(halfwayPack),
                    CueRole.Finish to parsePack(finish),
                )
            }
            combine(coreFlow, modesFlow, packsFlow) { (enabled, volume, halfway), modes, packs ->
                SoundSettingsSnapshot(
                    soundsEnabled = enabled,
                    cueModes = modes,
                    cueVolume = volume,
                    halfwayCallouts = halfway,
                    cuePacks = packs,
                )
            }.collect { snapshot ->
                takeAction(SoundSettingsAction.Receive(snapshot))
            }
        }
    }

    private fun parsePack(raw: String): SoundPack =
        runCatching { SoundPack.valueOf(raw) }.getOrDefault(SoundPack.Classic)

    private fun parseMode(raw: String): SoundMode =
        runCatching { SoundMode.valueOf(raw) }.getOrDefault(SoundMode.Beeps)

    override suspend fun handleAction(action: SoundSettingsAction) {
        when (action) {
            is SoundSettingsAction.Receive -> action.updateState {
                it.copy(
                    soundsEnabled = action.snapshot.soundsEnabled,
                    cueModes = action.snapshot.cueModes,
                    cuePacks = action.snapshot.cuePacks,
                    cueVolume = action.snapshot.cueVolume,
                    halfwayCallouts = action.snapshot.halfwayCallouts,
                )
            }
            is SoundSettingsAction.ReceiveVoices -> action.updateState {
                it.copy(availableVoices = action.voices)
            }
            is SoundSettingsAction.ReceiveVoiceId -> action.updateState {
                it.copy(voiceId = action.voiceId)
            }
            is SoundSettingsAction.SetSoundsEnabled -> {
                preferences.set(SoundsEnabledPref, action.enabled)
            }
            is SoundSettingsAction.SetCueMode -> {
                preferences.set(action.role.modePref(), action.mode.name)
                if (action.mode == SoundMode.Beeps) {
                    val pack = state.cuePacks[action.role] ?: SoundPack.Classic
                    previewPlayer.preview(pack, action.role.previewCueType(), state.cueVolume)
                }
            }
            is SoundSettingsAction.SetCuePack -> {
                preferences.set(action.role.pref(), action.pack.name)
                val cueType = action.role.previewCueType()
                previewPlayer.preview(action.pack, cueType, state.cueVolume)
            }
            is SoundSettingsAction.PreviewCue -> {
                val pack = state.cuePacks[action.role] ?: SoundPack.Classic
                previewPlayer.preview(pack, action.role.previewCueType(), state.cueVolume)
            }
            is SoundSettingsAction.SetCueVolume -> {
                preferences.set(CueVolumePref, action.volume.coerceIn(0f, 1f))
            }
            is SoundSettingsAction.SetVoice -> {
                preferences.set(VoiceIdPref, action.voiceId)
                voiceCatalog.preview(action.voiceId, volume = state.cueVolume)
            }
            is SoundSettingsAction.PreviewVoice -> {
                voiceCatalog.preview(action.voiceId, volume = state.cueVolume)
            }
            is SoundSettingsAction.SetHalfwayCallouts -> {
                preferences.set(HalfwayCalloutsPref, action.enabled)
            }
            SoundSettingsAction.GoBack -> {
                previewPlayer.stop()
                voiceCatalog.stopPreview()
                sendEvent(SoundSettingsEvent.Close)
            }
        }
    }
}

private fun CueRole.previewCueType(): SoundCueType = when (this) {
    CueRole.Countdown -> SoundCueType.Short
    CueRole.BlockStart -> SoundCueType.Long
    CueRole.Halfway -> SoundCueType.Short
    CueRole.Finish -> SoundCueType.Finish
}

data class SoundSettingsSnapshot(
    val soundsEnabled: Boolean,
    val cueModes: Map<CueRole, SoundMode>,
    val cuePacks: Map<CueRole, SoundPack>,
    val cueVolume: Float,
    val halfwayCallouts: Boolean,
)

data class SoundSettingsState(
    val soundsEnabled: Boolean = true,
    val cueModes: Map<CueRole, SoundMode> = CueRole.entries.associateWith { SoundMode.Beeps },
    val cuePacks: Map<CueRole, SoundPack> = CueRole.entries.associateWith { SoundPack.Classic },
    val cueVolume: Float = 0.8f,
    val halfwayCallouts: Boolean = false,
    val voiceId: String = "",
    val availableVoices: List<VoiceOption> = emptyList(),
) {
    val selectedVoice: VoiceOption?
        get() = availableVoices.firstOrNull { it.id == voiceId }
            ?: availableVoices.firstOrNull { it.isDefault }

    val anyRoleUsesVoice: Boolean
        get() = cueModes.values.any { it == SoundMode.Voice }
}

sealed interface SoundSettingsEvent {
    data object Close : SoundSettingsEvent
}

sealed interface SoundSettingsAction {
    data class Receive(val snapshot: SoundSettingsSnapshot) : SoundSettingsAction
    data class ReceiveVoices(val voices: List<VoiceOption>) : SoundSettingsAction
    data class ReceiveVoiceId(val voiceId: String) : SoundSettingsAction
    data class SetSoundsEnabled(val enabled: Boolean) : SoundSettingsAction
    data class SetCueMode(val role: CueRole, val mode: SoundMode) : SoundSettingsAction
    data class SetCuePack(val role: CueRole, val pack: SoundPack) : SoundSettingsAction
    data class PreviewCue(val role: CueRole) : SoundSettingsAction
    data class SetCueVolume(val volume: Float) : SoundSettingsAction
    data class SetVoice(val voiceId: String) : SoundSettingsAction
    data class PreviewVoice(val voiceId: String) : SoundSettingsAction
    data class SetHalfwayCallouts(val enabled: Boolean) : SoundSettingsAction
    data object GoBack : SoundSettingsAction
}
