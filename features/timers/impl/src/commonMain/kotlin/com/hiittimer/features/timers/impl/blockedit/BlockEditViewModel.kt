package com.dangerfield.hiittimer.features.timers.impl.blockedit

import androidx.lifecycle.viewModelScope
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@Inject
class BlockEditViewModel(
    private val repository: TimerRepository,
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
            is BlockEditAction.Receive -> action.updateState {
                it.copy(
                    block = action.block,
                    nameField = if (!it.nameInitialized) action.block.name else it.nameField,
                    nameInitialized = true,
                )
            }
            is BlockEditAction.Rename -> {
                action.updateState { it.copy(nameField = action.name) }
                val current = state.block ?: return
                if (current.name != action.name) {
                    val updated = current.copy(name = action.name)
                    action.updateState { it.copy(block = updated) }
                    repository.updateBlock(timerId, updated)
                }
            }
            is BlockEditAction.AdjustSeconds -> action.updateBlock {
                val total = it.duration.inWholeSeconds.toInt() + action.delta
                it.copy(duration = total.coerceAtLeast(1).seconds)
            }
            is BlockEditAction.SetColor -> action.updateBlock { it.copy(colorArgb = action.argb) }
            BlockEditAction.Delete -> {
                repository.deleteBlock(timerId, blockId)
                sendEvent(BlockEditEvent.Close)
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
    val nameField: String = "",
    val nameInitialized: Boolean = false,
)

sealed interface BlockEditEvent {
    data object Close : BlockEditEvent
}

sealed interface BlockEditAction {
    data class Receive(val block: Block) : BlockEditAction
    data class Rename(val name: String) : BlockEditAction
    data class AdjustSeconds(val delta: Int) : BlockEditAction
    data class SetColor(val argb: Int) : BlockEditAction
    data object Delete : BlockEditAction
}
