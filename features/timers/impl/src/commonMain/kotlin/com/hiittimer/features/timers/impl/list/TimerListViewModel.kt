package com.dangerfield.hiittimer.features.timers.impl.list

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.ExampleTimerSeededPref
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
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
            name = "Classic HIIT",
            cycleCount = 6,
            blocks = listOf(
                Block(
                    id = Uuid.random().toString(),
                    name = "Warm up",
                    duration = 40.seconds,
                    colorArgb = ColorPalette.warmupArgb,
                    role = BlockRole.Warmup,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = "High intensity",
                    duration = 30.seconds,
                    colorArgb = ColorPalette.defaultWorkArgb,
                    role = BlockRole.Cycle,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = "Rest",
                    duration = 5.seconds,
                    colorArgb = ColorPalette.defaultRestArgb,
                    role = BlockRole.Cycle,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = "Low intensity",
                    duration = 60.seconds,
                    colorArgb = ColorPalette.lowIntensityArgb,
                    role = BlockRole.Cycle,
                ),
                Block(
                    id = Uuid.random().toString(),
                    name = "Cool down",
                    duration = 45.seconds,
                    colorArgb = ColorPalette.lowIntensityArgb,
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
            TimerListAction.CreateNew -> {
                val timer = repository.create(name = "")
                sendEvent(TimerListEvent.OpenDetail(timer.id, isNew = true))
            }
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
