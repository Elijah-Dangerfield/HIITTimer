package com.dangerfield.hiittimer.features.timers.impl.builder

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Inject
class TimerBuilderViewModel(
    private val repository: TimerRepository,
    @Assisted private val timerId: String,
) : SEAViewModel<TimerBuilderState, TimerBuilderEvent, TimerBuilderAction>(initialStateArg = TimerBuilderState()) {

    private var renameJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observe(timerId).collect { timer ->
                takeAction(TimerBuilderAction.Receive(timer))
            }
        }
    }

    override suspend fun handleAction(action: TimerBuilderAction) {
        when (action) {
            is TimerBuilderAction.Receive -> action.updateState { current ->
                val loadedName = action.timer?.name
                val nameField = if (!current.nameInitialized && loadedName != null) loadedName else current.nameField
                current.copy(
                    timer = action.timer,
                    nameField = nameField,
                    nameInitialized = current.nameInitialized || loadedName != null,
                )
            }
            is TimerBuilderAction.RenameTimer -> {
                action.updateState { it.copy(nameField = action.name) }
                debouncePersistName(action.name)
            }
            is TimerBuilderAction.ChangeCycleCount -> {
                val current = state.timer ?: return
                repository.updateTimer(current.copy(cycleCount = action.count.coerceIn(1, 99)))
            }
            TimerBuilderAction.AddBlock -> {
                val timer = state.timer ?: return
                val alternatesRest = (timer.blocks.size % 2 == 1)
                val blockName = if (alternatesRest) "Rest" else "Work"
                val color = if (alternatesRest) ColorPalette.defaultRestArgb else ColorPalette.defaultWorkArgb
                val block = Block(
                    id = Uuid.random().toString(),
                    name = blockName,
                    duration = if (alternatesRest) 15.seconds else 30.seconds,
                    colorArgb = color,
                )
                repository.addBlock(timerId, block)
            }
            is TimerBuilderAction.DeleteBlock -> repository.deleteBlock(timerId, action.blockId)
            is TimerBuilderAction.OpenBlock -> sendEvent(TimerBuilderEvent.OpenBlock(timerId, action.blockId))
            TimerBuilderAction.Start -> sendEvent(TimerBuilderEvent.Start(timerId))
            is TimerBuilderAction.Reorder -> repository.reorderBlocks(timerId, action.orderedIds)
        }
    }

    private fun debouncePersistName(name: String) {
        renameJob?.cancel()
        renameJob = viewModelScope.launch {
            delay(RENAME_DEBOUNCE_MS)
            val current = state.timer ?: return@launch
            if (current.name != name) {
                repository.updateTimer(current.copy(name = name))
            }
        }
    }

    override fun onCleared() {
        renameJob?.cancel()
        val pendingName = state.nameField
        val loadedName = state.timer?.name
        if (pendingName.isNotEmpty() && pendingName != loadedName) {
            val current = state.timer
            if (current != null) {
                runCatching {
                    viewModelScope.launch { repository.updateTimer(current.copy(name = pendingName)) }
                }
            }
        }
        super.onCleared()
    }

    companion object {
        private const val RENAME_DEBOUNCE_MS = 400L
    }
}

data class TimerBuilderState(
    val timer: Timer? = null,
    val nameField: String = "",
    val nameInitialized: Boolean = false,
)

sealed interface TimerBuilderEvent {
    data class OpenBlock(val timerId: String, val blockId: String) : TimerBuilderEvent
    data class Start(val timerId: String) : TimerBuilderEvent
}

sealed interface TimerBuilderAction {
    data class Receive(val timer: Timer?) : TimerBuilderAction
    data class RenameTimer(val name: String) : TimerBuilderAction
    data class ChangeCycleCount(val count: Int) : TimerBuilderAction
    data object AddBlock : TimerBuilderAction
    data class DeleteBlock(val blockId: String) : TimerBuilderAction
    data class OpenBlock(val blockId: String) : TimerBuilderAction
    data class Reorder(val orderedIds: List<String>) : TimerBuilderAction
    data object Start : TimerBuilderAction
}
