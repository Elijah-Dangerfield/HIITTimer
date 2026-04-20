package com.dangerfield.hiittimer.features.timers.impl.runner

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.CompletedWorkoutsPref
import com.dangerfield.hiittimer.features.timers.CueVolumePref
import com.dangerfield.hiittimer.features.timers.HalfwayCalloutsPref
import com.dangerfield.hiittimer.features.timers.HapticsEnabledPref
import com.dangerfield.hiittimer.features.timers.ShowProgressBarPref
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundModePref
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.SoundPackPref
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.features.timers.impl.audio.AudioCuePlayer
import com.dangerfield.hiittimer.features.timers.impl.audio.AudioCuePlayerFactory
import com.dangerfield.hiittimer.features.timers.impl.haptics.HapticsCuePlayer
import com.dangerfield.hiittimer.features.timers.impl.haptics.HapticsCuePlayerFactory
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
                val next = when (state.soundMode) {
                    SoundMode.Off -> SoundMode.Beeps
                    SoundMode.Beeps -> SoundMode.Voice
                    SoundMode.Voice -> SoundMode.Off
                }
                preferences.set(SoundModePref, next.name)
                audio?.setMode(next)
                action.updateState { it.copy(soundMode = next) }
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
        val soundMode = runCatching { SoundMode.valueOf(preferences.get(SoundModePref)) }
            .getOrDefault(SoundMode.Beeps)
        val soundPack = runCatching { SoundPack.valueOf(preferences.get(SoundPackPref)) }
            .getOrDefault(SoundPack.Classic)
        val cueVolume = preferences.get(CueVolumePref)
        val hapticsEnabled = preferences.get(HapticsEnabledPref)
        updateState {
            it.copy(
                timer = timer,
                showProgressBar = showProgress,
                halfwayEnabled = halfway,
                soundMode = soundMode,
                soundPack = soundPack,
                cueVolume = cueVolume,
                hapticsEnabled = hapticsEnabled,
            )
        }
        takeAction(RunnerAction.Start)
    }

    private suspend fun RunnerAction.start() {
        val timer = state.timer ?: return
        val e = RunnerEngine(timer).also { engine = it }
        val player = audioFactory.create().also { audio = it }
        player.setMode(state.soundMode)
        player.setSoundPack(state.soundPack)
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
                if (state.soundMode != SoundMode.Off) player.play(cue)
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
    val soundMode: SoundMode = SoundMode.Beeps,
    val soundPack: SoundPack = SoundPack.Classic,
    val cueVolume: Float = 0.8f,
    val hapticsEnabled: Boolean = true,
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
    data object Stop : RunnerAction
}
