package com.dangerfield.hiittimer.features.timers.impl.detail

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class TimerDetailViewModel(
    private val repository: TimerRepository,
    @Assisted private val timerId: String,
) : SEAViewModel<TimerDetailState, TimerDetailEvent, TimerDetailAction>(initialStateArg = TimerDetailState()) {

    init {
        viewModelScope.launch {
            repository.observe(timerId).collect { timer ->
                takeAction(TimerDetailAction.Receive(timer))
            }
        }
    }

    override suspend fun handleAction(action: TimerDetailAction) {
        when (action) {
            is TimerDetailAction.Receive -> action.updateState { it.copy(timer = action.timer, loading = false) }
            TimerDetailAction.Start -> sendEvent(TimerDetailEvent.Start(timerId))
            TimerDetailAction.Edit -> sendEvent(TimerDetailEvent.Edit(timerId))
            TimerDetailAction.Duplicate -> {
                val newId = repository.duplicate(timerId) ?: return
                sendEvent(TimerDetailEvent.OpenDuplicate(newId))
            }
            TimerDetailAction.Delete -> {
                repository.delete(timerId)
                sendEvent(TimerDetailEvent.Close)
            }
        }
    }
}

data class TimerDetailState(
    val timer: Timer? = null,
    val loading: Boolean = true,
)

sealed interface TimerDetailEvent {
    data class Start(val timerId: String) : TimerDetailEvent
    data class Edit(val timerId: String) : TimerDetailEvent
    data class OpenDuplicate(val newTimerId: String) : TimerDetailEvent
    data object Close : TimerDetailEvent
}

sealed interface TimerDetailAction {
    data class Receive(val timer: Timer?) : TimerDetailAction
    data object Start : TimerDetailAction
    data object Edit : TimerDetailAction
    data object Duplicate : TimerDetailAction
    data object Delete : TimerDetailAction
}
