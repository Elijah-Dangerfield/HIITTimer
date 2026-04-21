package com.dangerfield.hiittimer.features.timers.impl.preset

import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.impl.TimerRepository
import com.dangerfield.hiittimer.libraries.flowroutines.SEAViewModel
import com.dangerfield.hiittimer.system.color.defaultRunnerColors
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.getString
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.preset_amrap_name
import rounds.libraries.resources.generated.resources.preset_emom_name
import rounds.libraries.resources.generated.resources.preset_intervals_name
import rounds.libraries.resources.generated.resources.preset_tabata_name
import rounds.libraries.resources.generated.resources.starter_block_cooldown
import rounds.libraries.resources.generated.resources.starter_block_rest
import rounds.libraries.resources.generated.resources.starter_block_warmup
import rounds.libraries.resources.generated.resources.starter_block_work
import rounds.libraries.resources.generated.resources.starter_block_workout
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

enum class TimerPreset {
    Tabata,
    EMOM,
    AMRAP,
    Intervals,
    Custom,
}

@Inject
class TimerPresetViewModel(
    private val repository: TimerRepository,
) : SEAViewModel<TimerPresetState, TimerPresetEvent, TimerPresetAction>(
    initialStateArg = TimerPresetState()
) {

    override suspend fun handleAction(action: TimerPresetAction) {
        when (action) {
            is TimerPresetAction.SelectPreset -> {
                val warmup = getString(AppRes.string.starter_block_warmup)
                val work = getString(AppRes.string.starter_block_work)
                val rest = getString(AppRes.string.starter_block_rest)
                val cooldown = getString(AppRes.string.starter_block_cooldown)
                val workout = getString(AppRes.string.starter_block_workout)
                val timer = when (action.preset) {
                    TimerPreset.Tabata -> repository.createWithBlocks(
                        name = getString(AppRes.string.preset_tabata_name),
                        cycleCount = 8,
                        blocks = listOf(
                            Block(
                                id = Uuid.random().toString(),
                                name = warmup,
                                duration = 60.seconds,
                                colorArgb = defaultRunnerColors.warmupArgb,
                                role = BlockRole.Warmup,
                            ),
                            Block(
                                id = Uuid.random().toString(),
                                name = work,
                                duration = 20.seconds,
                                colorArgb = defaultRunnerColors.defaultWorkArgb,
                                role = BlockRole.Cycle,
                            ),
                            Block(
                                id = Uuid.random().toString(),
                                name = rest,
                                duration = 10.seconds,
                                colorArgb = defaultRunnerColors.defaultRestArgb,
                                role = BlockRole.Cycle,
                            ),
                            Block(
                                id = Uuid.random().toString(),
                                name = cooldown,
                                duration = 60.seconds,
                                colorArgb = defaultRunnerColors.lowIntensityArgb,
                                role = BlockRole.Cooldown,
                            ),
                        ),
                    )
                    TimerPreset.EMOM -> repository.createWithBlocks(
                        name = getString(AppRes.string.preset_emom_name),
                        cycleCount = 10,
                        blocks = listOf(
                            Block(
                                id = Uuid.random().toString(),
                                name = work,
                                duration = 60.seconds,
                                colorArgb = defaultRunnerColors.defaultWorkArgb,
                                role = BlockRole.Cycle,
                            ),
                        ),
                    )
                    TimerPreset.AMRAP -> repository.createWithBlocks(
                        name = getString(AppRes.string.preset_amrap_name),
                        cycleCount = 1,
                        blocks = listOf(
                            Block(
                                id = Uuid.random().toString(),
                                name = workout,
                                duration = 600.seconds,
                                colorArgb = defaultRunnerColors.defaultWorkArgb,
                                role = BlockRole.Cycle,
                            ),
                        ),
                    )
                    TimerPreset.Intervals -> repository.createWithBlocks(
                        name = getString(AppRes.string.preset_intervals_name),
                        cycleCount = 5,
                        blocks = listOf(
                            Block(
                                id = Uuid.random().toString(),
                                name = warmup,
                                duration = 60.seconds,
                                colorArgb = defaultRunnerColors.warmupArgb,
                                role = BlockRole.Warmup,
                            ),
                            Block(
                                id = Uuid.random().toString(),
                                name = work,
                                duration = 45.seconds,
                                colorArgb = defaultRunnerColors.defaultWorkArgb,
                                role = BlockRole.Cycle,
                            ),
                            Block(
                                id = Uuid.random().toString(),
                                name = rest,
                                duration = 15.seconds,
                                colorArgb = defaultRunnerColors.defaultRestArgb,
                                role = BlockRole.Cycle,
                            ),
                            Block(
                                id = Uuid.random().toString(),
                                name = cooldown,
                                duration = 60.seconds,
                                colorArgb = defaultRunnerColors.lowIntensityArgb,
                                role = BlockRole.Cooldown,
                            ),
                        ),
                    )
                    TimerPreset.Custom -> repository.create(name = "")
                }
                sendEvent(TimerPresetEvent.OpenTimer(timer.id))
            }
            TimerPresetAction.GoBack -> sendEvent(TimerPresetEvent.Close)
        }
    }
}

data class TimerPresetState(
    val presets: List<TimerPreset> = TimerPreset.entries,
)

sealed interface TimerPresetEvent {
    data class OpenTimer(val timerId: String) : TimerPresetEvent
    data object Close : TimerPresetEvent
}

sealed interface TimerPresetAction {
    data class SelectPreset(val preset: TimerPreset) : TimerPresetAction
    data object GoBack : TimerPresetAction
}

