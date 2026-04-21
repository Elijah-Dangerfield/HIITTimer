package com.dangerfield.hiittimer.features.timers.impl.list

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.ExampleTimerSeededPref
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import com.dangerfield.hiittimer.system.color.defaultRunnerColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.getString
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.starter_block_cooldown
import rounds.libraries.resources.generated.resources.starter_block_high_intensity
import rounds.libraries.resources.generated.resources.starter_block_low_intensity
import rounds.libraries.resources.generated.resources.starter_block_rest
import rounds.libraries.resources.generated.resources.starter_block_warmup
import rounds.libraries.resources.generated.resources.starter_timer_classic_hiit
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Inject
class TimerListViewModel(
    private val repository: TimerRepository,
    private val preferences: Preferences,
) : SEAViewModel<TimerListState, TimerListEvent, TimerListAction>(initialStateArg = TimerListState()) {

    init {
        viewModelScope.launch { seedExampleIfNeeded() }
        viewModelScope.launch {
            repository.observeAll().collect { timers ->
                takeAction(TimerListAction.Receive(timers))
            }
        }
    }

    private suspend fun seedExampleIfNeeded() {
        if (preferences.get(ExampleTimerSeededPref)) return
        val existing = repository.observeAll().first()
        if (existing.isNotEmpty()) {
            // Mark as seeded so we don't try again later if a user deletes everything.
            preferences.set(ExampleTimerSeededPref, true)
            return
        }
        repository.createWithBlocks(
            name = getString(AppRes.string.starter_timer_classic_hiit),
            cycleCount = 6,
            blocks = listOf(
                Block(
                    id = Uuid.random().toString(),
                    name = getString(AppRes.string.starter_block_warmup),
                    duration = 40.seconds,
                    colorArgb = defaultRunnerColors.warmupArgb,
                    role = BlockRole.Warmup,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = getString(AppRes.string.starter_block_high_intensity),
                    duration = 30.seconds,
                    colorArgb = defaultRunnerColors.defaultWorkArgb,
                    role = BlockRole.Cycle,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = getString(AppRes.string.starter_block_rest),
                    duration = 5.seconds,
                    colorArgb = defaultRunnerColors.defaultRestArgb,
                    role = BlockRole.Cycle,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = getString(AppRes.string.starter_block_low_intensity),
                    duration = 60.seconds,
                    colorArgb = defaultRunnerColors.lowIntensityArgb,
                    role = BlockRole.Cycle,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = getString(AppRes.string.starter_block_cooldown),
                    duration = 45.seconds,
                    colorArgb = defaultRunnerColors.lowIntensityArgb,
                    role = BlockRole.Cooldown,
                ),
            ),
        )
        preferences.set(ExampleTimerSeededPref, true)
    }

    override suspend fun handleAction(action: TimerListAction) {
        when (action) {
            is TimerListAction.Receive -> action.updateState {
                it.copy(timers = action.timers, loading = false)
            }
            TimerListAction.CreateNew -> sendEvent(TimerListEvent.OpenPresets)
            is TimerListAction.Open -> sendEvent(TimerListEvent.OpenDetail(action.timerId, isNew = false))
            is TimerListAction.Start -> sendEvent(TimerListEvent.StartRunner(action.timerId))
            TimerListAction.OpenSettings -> sendEvent(TimerListEvent.OpenSettings)
            is TimerListAction.RequestDelete -> action.updateState {
                it.copy(pendingDeleteTimerId = action.timerId)
            }
            TimerListAction.DismissDelete -> action.updateState {
                it.copy(pendingDeleteTimerId = null)
            }
            TimerListAction.ConfirmDelete -> {
                val id = state.pendingDeleteTimerId ?: return
                action.updateState { it.copy(pendingDeleteTimerId = null) }
                repository.delete(id)
            }
        }
    }
}

data class TimerListState(
    val timers: List<Timer> = emptyList(),
    val loading: Boolean = true,
    val pendingDeleteTimerId: String? = null,
)

sealed interface TimerListEvent {
    data class OpenDetail(val timerId: String, val isNew: Boolean) : TimerListEvent
    data class StartRunner(val timerId: String) : TimerListEvent
    data object OpenSettings : TimerListEvent
    data object OpenPresets : TimerListEvent
}

sealed interface TimerListAction {
    data class Receive(val timers: List<Timer>) : TimerListAction
    data object CreateNew : TimerListAction
    data class Open(val timerId: String) : TimerListAction
    data class Start(val timerId: String) : TimerListAction
    data object OpenSettings : TimerListAction
    data class RequestDelete(val timerId: String) : TimerListAction
    data object ConfirmDelete : TimerListAction
    data object DismissDelete : TimerListAction
}
