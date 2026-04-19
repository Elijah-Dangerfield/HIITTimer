package com.dangerfield.hiittimer.features.timers.impl.detail

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.SkipBlockDeleteConfirmationPref
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Inject
class TimerDetailViewModel(
    private val repository: TimerRepository,
    private val preferences: Preferences,
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
            is TimerDetailAction.Receive -> action.updateState { current ->
                val loadedName = action.timer?.name
                val nameField = if (!current.nameInitialized && loadedName != null) loadedName else current.nameField
                current.copy(
                    timer = action.timer,
                    nameField = nameField,
                    nameInitialized = current.nameInitialized || loadedName != null,
                    loading = false,
                )
            }
            is TimerDetailAction.RenameTimer -> {
                action.updateState { it.copy(nameField = action.name) }
                val current = state.timer ?: return
                val nameToSave = action.name.ifBlank { "Untitled" }
                if (current.name != nameToSave) {
                    repository.updateTimer(current.copy(name = nameToSave))
                }
            }
            is TimerDetailAction.ChangeCycleCount -> {
                val current = state.timer ?: return
                repository.updateTimer(current.copy(cycleCount = action.count.coerceIn(1, 99)))
            }
            is TimerDetailAction.AddBlock -> {
                val timer = state.timer ?: return
                val block = when (action.role) {
                    BlockRole.Warmup -> Block(
                        id = Uuid.random().toString(),
                        name = "Warm up",
                        duration = 30.seconds,
                        colorArgb = ColorPalette.warmupArgb,
                        role = BlockRole.Warmup,
                    )
                    BlockRole.Cooldown -> Block(
                        id = Uuid.random().toString(),
                        name = "Cool down",
                        duration = 30.seconds,
                        colorArgb = ColorPalette.lowIntensityArgb,
                        role = BlockRole.Cooldown,
                    )
                    BlockRole.Cycle -> {
                        val alternatesRest = (timer.cycleBlocks.size % 2 == 1)
                        Block(
                            id = Uuid.random().toString(),
                            name = if (alternatesRest) "Rest" else "Work",
                            duration = if (alternatesRest) 15.seconds else 30.seconds,
                            colorArgb = if (alternatesRest) ColorPalette.defaultRestArgb else ColorPalette.defaultWorkArgb,
                            role = BlockRole.Cycle,
                        )
                    }
                }
                repository.addBlock(timerId, block)
            }
            is TimerDetailAction.AdjustBlockDuration -> {
                val timer = state.timer ?: return
                val block = timer.blocks.firstOrNull { it.id == action.blockId } ?: return
                val newSeconds = (block.duration.inWholeSeconds.toInt() + action.deltaSeconds).coerceAtLeast(1)
                repository.updateBlock(timerId, block.copy(duration = newSeconds.seconds))
            }
            is TimerDetailAction.RequestDeleteBlock -> {
                val skip = preferences.get(SkipBlockDeleteConfirmationPref)
                if (skip) {
                    repository.deleteBlock(timerId, action.blockId)
                } else {
                    action.updateState {
                        it.copy(
                            pendingDeleteBlockId = action.blockId,
                            dontAskAgainChecked = false,
                        )
                    }
                }
            }
            is TimerDetailAction.ToggleDontAskAgain -> action.updateState {
                it.copy(dontAskAgainChecked = action.checked)
            }
            TimerDetailAction.ConfirmDeleteBlock -> {
                val id = state.pendingDeleteBlockId ?: return
                val dontAsk = state.dontAskAgainChecked
                action.updateState {
                    it.copy(pendingDeleteBlockId = null, dontAskAgainChecked = false)
                }
                if (dontAsk) {
                    preferences.set(SkipBlockDeleteConfirmationPref, true)
                }
                repository.deleteBlock(timerId, id)
            }
            TimerDetailAction.DismissDeleteBlock -> action.updateState {
                it.copy(pendingDeleteBlockId = null, dontAskAgainChecked = false)
            }
            is TimerDetailAction.ReorderBlocks -> repository.reorderBlocks(timerId, action.orderedIds)
            is TimerDetailAction.OpenBlock -> sendEvent(TimerDetailEvent.OpenBlock(timerId, action.blockId))
            TimerDetailAction.Start -> sendEvent(TimerDetailEvent.Start(timerId))
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
    val nameField: String = "",
    val nameInitialized: Boolean = false,
    val loading: Boolean = true,
    val pendingDeleteBlockId: String? = null,
    val dontAskAgainChecked: Boolean = false,
)

sealed interface TimerDetailEvent {
    data class Start(val timerId: String) : TimerDetailEvent
    data class OpenBlock(val timerId: String, val blockId: String) : TimerDetailEvent
    data class OpenDuplicate(val newTimerId: String) : TimerDetailEvent
    data object Close : TimerDetailEvent
}

sealed interface TimerDetailAction {
    data class Receive(val timer: Timer?) : TimerDetailAction
    data class RenameTimer(val name: String) : TimerDetailAction
    data class ChangeCycleCount(val count: Int) : TimerDetailAction
    data class AddBlock(val role: BlockRole) : TimerDetailAction
    data class AdjustBlockDuration(val blockId: String, val deltaSeconds: Int) : TimerDetailAction
    data class RequestDeleteBlock(val blockId: String) : TimerDetailAction
    data class ToggleDontAskAgain(val checked: Boolean) : TimerDetailAction
    data object ConfirmDeleteBlock : TimerDetailAction
    data object DismissDeleteBlock : TimerDetailAction
    data class ReorderBlocks(val orderedIds: List<String>) : TimerDetailAction
    data class OpenBlock(val blockId: String) : TimerDetailAction
    data object Start : TimerDetailAction
    data object Duplicate : TimerDetailAction
    data object Delete : TimerDetailAction
}
