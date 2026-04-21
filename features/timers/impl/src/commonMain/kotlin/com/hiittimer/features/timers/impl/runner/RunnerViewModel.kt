package com.dangerfield.hiittimer.features.timers.impl.runner

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.CompletedWorkoutsPref
import com.dangerfield.hiittimer.features.timers.CueRole
import com.dangerfield.hiittimer.features.timers.CueVolumePref
import com.dangerfield.hiittimer.features.timers.HalfwayCalloutsPref
import com.dangerfield.hiittimer.features.timers.HapticsEnabledPref
import com.dangerfield.hiittimer.features.timers.HasSeenRunnerVolumeTooltipPref
import com.dangerfield.hiittimer.features.timers.ShowProgressBarPref
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.SoundsEnabledPref
import com.dangerfield.hiittimer.features.timers.VoiceIdPref
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.features.timers.impl.audio.AudioCuePlayer
import com.dangerfield.hiittimer.features.timers.impl.audio.AudioCuePlayerFactory
import com.dangerfield.hiittimer.features.timers.impl.haptics.HapticsCuePlayer
import com.dangerfield.hiittimer.features.timers.impl.haptics.HapticsCuePlayerFactory
import com.dangerfield.hiittimer.features.timers.modePref
import com.dangerfield.hiittimer.features.timers.pref
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageCoordinator
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageTrigger
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class RunnerViewModel(
    private val repository: TimerRepository,
    private val preferences: Preferences,
    private val audioFactory: AudioCuePlayerFactory,
    private val hapticsFactory: HapticsCuePlayerFactory,
    private val inAppMessageCoordinator: InAppMessageCoordinator,
    private val foregroundController: RunnerForegroundController,
    @Assisted private val timerId: String,
) : SEAViewModel<RunnerUiState, RunnerEvent, RunnerAction>(initialStateArg = RunnerUiState()) {

    private var engine: RunnerEngine? = null
    private var audio: AudioCuePlayer? = null
    private var haptics: HapticsCuePlayer? = null
    private var foregroundStarted: Boolean = false

    init {
        takeAction(RunnerAction.Load)
    }

    override suspend fun handleAction(action: RunnerAction) {
        when (action) {
            RunnerAction.Load -> action.load()
            RunnerAction.Start -> action.start()
            RunnerAction.Pause -> engine?.pause()
            RunnerAction.Resume -> viewModelScope.launch { engine?.resume() }
            RunnerAction.Skip -> engine?.skip()
            RunnerAction.ResetBlock -> engine?.resetCurrentBlock()
            RunnerAction.ToggleSound -> {
                val next = !state.soundsEnabled
                preferences.set(SoundsEnabledPref, next)
                audio?.setMasterEnabled(next)
                action.updateState { it.copy(soundsEnabled = next) }
            }
            is RunnerAction.SetCueVolume -> {
                val clamped = action.volume.coerceIn(0f, 1f)
                preferences.set(CueVolumePref, clamped)
                audio?.setVolume(clamped)
                action.updateState { it.copy(cueVolume = clamped) }
            }
            RunnerAction.DismissVolumeTooltip -> {
                preferences.set(HasSeenRunnerVolumeTooltipPref, true)
                action.updateState { it.copy(showVolumeTooltip = false) }
            }
            RunnerAction.Stop -> {
                engine?.stop()
                audio?.release()
                audio = null
                haptics?.release()
                haptics = null
                stopForegroundIfNeeded()
                sendEvent(RunnerEvent.Exit)
            }
        }
    }

    private suspend fun RunnerAction.load() {
        val timer = repository.observe(timerId).firstOrNull() ?: run {
            sendEvent(RunnerEvent.Exit)
            return
        }
        val showProgress = preferences.get(ShowProgressBarPref)
        val halfway = preferences.get(HalfwayCalloutsPref)
        val soundsEnabled = preferences.get(SoundsEnabledPref)
        val cueModes: Map<CueRole, SoundMode> = CueRole.entries.associateWith { role ->
            runCatching { SoundMode.valueOf(preferences.get(role.modePref())) }
                .getOrDefault(SoundMode.Beeps)
        }
        val cuePacks: Map<CueRole, SoundPack> = CueRole.entries.associateWith { role ->
            runCatching { SoundPack.valueOf(preferences.get(role.pref())) }
                .getOrDefault(SoundPack.Classic)
        }
        val cueVolume = preferences.get(CueVolumePref)
        val voiceId = preferences.get(VoiceIdPref)
        val hapticsEnabled = preferences.get(HapticsEnabledPref)
        val hasSeenVolumeTooltip = preferences.get(HasSeenRunnerVolumeTooltipPref)
        updateState {
            it.copy(
                timer = timer,
                showProgressBar = showProgress,
                halfwayEnabled = halfway,
                soundsEnabled = soundsEnabled,
                cueModes = cueModes,
                cuePacks = cuePacks,
                cueVolume = cueVolume,
                voiceId = voiceId,
                hapticsEnabled = hapticsEnabled,
                showVolumeTooltip = !hasSeenVolumeTooltip,
            )
        }
        takeAction(RunnerAction.Start)
    }

    private suspend fun RunnerAction.start() {
        val timer = state.timer ?: return
        val e = RunnerEngine(timer).also { engine = it }
        val player = audioFactory.create().also { audio = it }
        player.setMasterEnabled(state.soundsEnabled)
        state.cueModes.forEach { (role, mode) -> player.setMode(role, mode) }
        state.cuePacks.forEach { (role, pack) -> player.setCuePack(role, pack) }
        player.setVoice(state.voiceId)
        player.setVolume(state.cueVolume)

        val hapticsPlayer = hapticsFactory.create().also { haptics = it }
        hapticsPlayer.setEnabled(state.hapticsEnabled)

        if (!foregroundStarted) {
            foregroundController.start()
            foregroundStarted = true
        }

        viewModelScope.launch {
            e.state.collect { engineState ->
                updateState { it.copy(engineState = engineState) }
                if (engineState is RunnerState.Finished) {
                    stopForegroundIfNeeded()
                    val completed = preferences.get(CompletedWorkoutsPref) + 1
                    preferences.set(CompletedWorkoutsPref, completed)
                    inAppMessageCoordinator.tryShow(InAppMessageTrigger.WorkoutCompleted)
                }
            }
        }
        viewModelScope.launch {
            e.cues.collect { cue ->
                if (cue is RunnerCue.Halfway && !state.halfwayEnabled) return@collect
                if (state.soundsEnabled) player.play(cue)
                if (state.hapticsEnabled) hapticsPlayer.play(cue)
            }
        }
        viewModelScope.launch { e.start() }
    }

    private fun stopForegroundIfNeeded() {
        if (foregroundStarted) {
            foregroundController.stop()
            foregroundStarted = false
        }
    }

    override fun onCleared() {
        audio?.release()
        haptics?.release()
        stopForegroundIfNeeded()
        super.onCleared()
    }
}

data class RunnerUiState(
    val timer: Timer? = null,
    val engineState: RunnerState = RunnerState.Idle,
    val showProgressBar: Boolean = true,
    val halfwayEnabled: Boolean = false,
    val soundsEnabled: Boolean = true,
    val cueModes: Map<CueRole, SoundMode> = CueRole.entries.associateWith { SoundMode.Beeps },
    val cuePacks: Map<CueRole, SoundPack> = CueRole.entries.associateWith { SoundPack.Classic },
    val cueVolume: Float = 0.8f,
    val voiceId: String = "",
    val hapticsEnabled: Boolean = true,
    val showVolumeTooltip: Boolean = false,
)

sealed interface RunnerEvent {
    data object Exit : RunnerEvent
}

sealed interface RunnerAction {
    data object Load : RunnerAction
    data object Start : RunnerAction
    data object Pause : RunnerAction
    data object Resume : RunnerAction
    data object Skip : RunnerAction
    data object ResetBlock : RunnerAction
    data object ToggleSound : RunnerAction
    data class SetCueVolume(val volume: Float) : RunnerAction
    data object DismissVolumeTooltip : RunnerAction
    data object Stop : RunnerAction
}
