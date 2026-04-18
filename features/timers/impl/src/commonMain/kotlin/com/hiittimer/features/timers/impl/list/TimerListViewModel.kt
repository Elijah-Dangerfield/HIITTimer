package com.dangerfield.hiittimer.features.timers.impl.list

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class TimerListViewModel(
    private val repository: TimerRepository,
) : SEAViewModel<TimerListState, TimerListEvent, TimerListAction>(initialStateArg = TimerListState()) {

    init {
        viewModelScope.launch {
            repository.observeAll().collect { timers ->
                takeAction(TimerListAction.Receive(timers))
            }
        }
    }

    override suspend fun handleAction(action: TimerListAction) {
        when (action) {
            is TimerListAction.Receive -> action.updateState { it.copy(timers = action.timers, loading = false) }
            TimerListAction.CreateNew -> {
                val timer = repository.create(name = "New Timer")
                sendEvent(TimerListEvent.OpenBuilder(timer.id))
            }
            is TimerListAction.Duplicate -> repository.duplicate(action.timerId)
            is TimerListAction.Delete -> repository.delete(action.timerId)
            TimerListAction.OpenSettings -> sendEvent(TimerListEvent.OpenSettings)
            is TimerListAction.Open -> sendEvent(TimerListEvent.OpenRunner(action.timerId))
            is TimerListAction.Edit -> sendEvent(TimerListEvent.OpenBuilder(action.timerId))
        }
    }
}

data class TimerListState(
    val timers: List<Timer> = emptyList(),
    val loading: Boolean = true,
)

sealed interface TimerListEvent {
    data class OpenBuilder(val timerId: String) : TimerListEvent
    data class OpenRunner(val timerId: String) : TimerListEvent
    data object OpenSettings : TimerListEvent
}

sealed interface TimerListAction {
    data class Receive(val timers: List<Timer>) : TimerListAction
    data object CreateNew : TimerListAction
    data class Duplicate(val timerId: String) : TimerListAction
    data class Delete(val timerId: String) : TimerListAction
    data object OpenSettings : TimerListAction
    data class Open(val timerId: String) : TimerListAction
    data class Edit(val timerId: String) : TimerListAction
}
