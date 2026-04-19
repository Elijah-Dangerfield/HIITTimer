package com.dangerfield.hiittimer.features.timers.impl.blockedit

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.SkipBlockDeleteConfirmationPref
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@Inject
class BlockEditViewModel(
    private val repository: TimerRepository,
    private val preferences: Preferences,
    @Assisted private val timerId: String,
    @Assisted private val blockId: String,
) : SEAViewModel<BlockEditState, BlockEditEvent, BlockEditAction>(initialStateArg = BlockEditState()) {

    init {
        viewModelScope.launch {
            val timer = repository.observe(timerId).first() ?: run {
                sendEvent(BlockEditEvent.Close); return@launch
            }
            val block = timer.blocks.firstOrNull { it.id == blockId } ?: run {
                sendEvent(BlockEditEvent.Close); return@launch
            }
            takeAction(BlockEditAction.Receive(block))
        }
    }

    override suspend fun handleAction(action: BlockEditAction) {
        when (action) {
            is BlockEditAction.Receive -> action.updateState { it.copy(block = action.block) }

            is BlockEditAction.SetDurationSeconds -> action.updateBlock {
                it.copy(duration = action.seconds.coerceAtLeast(1).seconds)
            }

            is BlockEditAction.SetColor -> action.updateBlock { it.copy(colorArgb = action.argb) }

            BlockEditAction.OpenColorPicker -> action.updateState { it.copy(colorPickerOpen = true) }
            BlockEditAction.DismissColorPicker -> action.updateState { it.copy(colorPickerOpen = false) }

            BlockEditAction.OpenRename -> action.updateState { it.copy(renameOpen = true) }
            is BlockEditAction.CommitRename -> {
                val newName = action.name.trim()
                action.updateState { it.copy(renameOpen = false) }
                val current = state.block ?: return
                if (newName.isNotEmpty() && current.name != newName) {
                    val updated = current.copy(name = newName)
                    action.updateState { it.copy(block = updated) }
                    repository.updateBlock(timerId, updated)
                }
            }
            BlockEditAction.DismissRename -> action.updateState { it.copy(renameOpen = false) }

            BlockEditAction.RequestDelete -> {
                val skip = preferences.get(SkipBlockDeleteConfirmationPref)
                if (skip) {
                    repository.deleteBlock(timerId, blockId)
                    sendEvent(BlockEditEvent.Close)
                } else {
                    action.updateState {
                        it.copy(deleteConfirmationOpen = true, dontAskAgainChecked = false)
                    }
                }
            }
            is BlockEditAction.ToggleDontAskAgain -> action.updateState {
                it.copy(dontAskAgainChecked = action.checked)
            }
            BlockEditAction.ConfirmDelete -> {
                val dontAsk = state.dontAskAgainChecked
                action.updateState { it.copy(deleteConfirmationOpen = false) }
                if (dontAsk) {
                    preferences.set(SkipBlockDeleteConfirmationPref, true)
                }
                repository.deleteBlock(timerId, blockId)
                sendEvent(BlockEditEvent.Close)
            }
            BlockEditAction.DismissDelete -> action.updateState {
                it.copy(deleteConfirmationOpen = false, dontAskAgainChecked = false)
            }
        }
    }

    private suspend fun BlockEditAction.updateBlock(transform: (Block) -> Block) {
        val current = state.block ?: return
        val updated = transform(current)
        updateState { it.copy(block = updated) }
        repository.updateBlock(timerId, updated)
    }
}

data class BlockEditState(
    val block: Block? = null,
    val colorPickerOpen: Boolean = false,
    val renameOpen: Boolean = false,
    val deleteConfirmationOpen: Boolean = false,
    val dontAskAgainChecked: Boolean = false,
)

sealed interface BlockEditEvent {
    data object Close : BlockEditEvent
}

sealed interface BlockEditAction {
    data class Receive(val block: Block) : BlockEditAction
    data class SetDurationSeconds(val seconds: Int) : BlockEditAction
    data class SetColor(val argb: Int) : BlockEditAction
    data object OpenColorPicker : BlockEditAction
    data object DismissColorPicker : BlockEditAction
    data object OpenRename : BlockEditAction
    data class CommitRename(val name: String) : BlockEditAction
    data object DismissRename : BlockEditAction
    data object RequestDelete : BlockEditAction
    data class ToggleDontAskAgain(val checked: Boolean) : BlockEditAction
    data object ConfirmDelete : BlockEditAction
    data object DismissDelete : BlockEditAction
}
