package com.dangerfield.hiittimer.features.timers.impl

import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import com.dangerfield.hiittimer.libraries.hiittimer.AppCache
import com.dangerfield.hiittimer.libraries.hiittimer.AppEvent
import com.dangerfield.hiittimer.libraries.hiittimer.AppEventListener
import com.dangerfield.hiittimer.system.color.defaultRunnerColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.getString
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.starter_block_cooldown
import rounds.libraries.resources.generated.resources.starter_block_minute
import rounds.libraries.resources.generated.resources.starter_block_rest
import rounds.libraries.resources.generated.resources.starter_block_warmup
import rounds.libraries.resources.generated.resources.starter_block_work
import rounds.libraries.resources.generated.resources.starter_emom10_name
import rounds.libraries.resources.generated.resources.starter_hiit4515_name
import rounds.libraries.resources.generated.resources.starter_tabata_name
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/**
 * Seeds the timer list with a few ready-to-run workouts the first time the app
 * boots. Guards on a flag in [AppCache] so deleting the starters (or adding
 * your own before we ever got to run) doesn't cause them to reappear.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true, boundType = AppEventListener::class)
@Inject
class StarterTimersSeeder(
    private val repository: TimerRepository,
    private val appCache: AppCache,
    private val appScope: AppCoroutineScope,
) : AppEventListener {

    private val logger = KLog.withTag("StarterTimersSeeder")

    override fun onColdBoot(event: AppEvent.ColdBoot) {
        appScope.launch {
            val data = appCache.get()
            if (data.hasCheckedStarterTimers) return@launch
            val existing = repository.observeAll().first()
            if (existing.isEmpty()) {
                val timers = starterTimers()
                timers.forEach { preset ->
                    repository.createWithBlocks(
                        name = preset.name,
                        cycleCount = preset.cycleCount,
                        blocks = preset.blocks,
                    )
                }
                logger.i("Seeded ${timers.size} starter timers")
            }
            appCache.update { it.copy(hasCheckedStarterTimers = true) }
        }
    }
}

private data class StarterTimer(
    val name: String,
    val cycleCount: Int,
    val blocks: List<Block>,
)

private suspend fun starterTimers(): List<StarterTimer> = listOf(
    tabata(),
    emom10(),
    hiit4515(),
)

private suspend fun tabata(): StarterTimer = StarterTimer(
    name = getString(AppRes.string.starter_tabata_name),
    cycleCount = 8,
    blocks = listOf(
        Block(Uuid.random().toString(), getString(AppRes.string.starter_block_work), 20.seconds, defaultRunnerColors.defaultWorkArgb, BlockRole.Cycle),
        Block(Uuid.random().toString(), getString(AppRes.string.starter_block_rest), 10.seconds, defaultRunnerColors.defaultRestArgb, BlockRole.Cycle),
    ),
)

private suspend fun emom10(): StarterTimer = StarterTimer(
    name = getString(AppRes.string.starter_emom10_name),
    cycleCount = 10,
    blocks = listOf(
        Block(Uuid.random().toString(), getString(AppRes.string.starter_block_minute), 60.seconds, defaultRunnerColors.defaultWorkArgb, BlockRole.Cycle),
    ),
)

private suspend fun hiit4515(): StarterTimer = StarterTimer(
    name = getString(AppRes.string.starter_hiit4515_name),
    cycleCount = 8,
    blocks = listOf(
        Block(Uuid.random().toString(), getString(AppRes.string.starter_block_warmup), 120.seconds, defaultRunnerColors.warmupArgb, BlockRole.Warmup),
        Block(Uuid.random().toString(), getString(AppRes.string.starter_block_work), 45.seconds, defaultRunnerColors.defaultWorkArgb, BlockRole.Cycle),
        Block(Uuid.random().toString(), getString(AppRes.string.starter_block_rest), 15.seconds, defaultRunnerColors.defaultRestArgb, BlockRole.Cycle),
        Block(Uuid.random().toString(), getString(AppRes.string.starter_block_cooldown), 120.seconds, defaultRunnerColors.lowIntensityArgb, BlockRole.Cooldown),
    ),
)
