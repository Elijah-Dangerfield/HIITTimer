package com.dangerfield.hiittimer.features.timers.impl

import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import com.dangerfield.hiittimer.libraries.hiittimer.AppCache
import com.dangerfield.hiittimer.libraries.hiittimer.AppEvent
import com.dangerfield.hiittimer.libraries.hiittimer.AppEventListener
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
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
                starterTimers().forEach { preset ->
                    repository.createWithBlocks(
                        name = preset.name,
                        cycleCount = preset.cycleCount,
                        blocks = preset.blocks,
                    )
                }
                logger.i("Seeded ${starterTimers().size} starter timers")
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

private fun starterTimers(): List<StarterTimer> = listOf(
    tabata(),
    emom10(),
    hiit4515(),
)

private fun tabata(): StarterTimer = StarterTimer(
    name = "Tabata",
    cycleCount = 8,
    blocks = listOf(
        Block(Uuid.random().toString(), "Work", 20.seconds, ColorPalette.defaultWorkArgb, BlockRole.Cycle),
        Block(Uuid.random().toString(), "Rest", 10.seconds, ColorPalette.defaultRestArgb, BlockRole.Cycle),
    ),
)

private fun emom10(): StarterTimer = StarterTimer(
    name = "EMOM 10",
    cycleCount = 10,
    blocks = listOf(
        Block(Uuid.random().toString(), "Minute", 60.seconds, ColorPalette.defaultWorkArgb, BlockRole.Cycle),
    ),
)

private fun hiit4515(): StarterTimer = StarterTimer(
    name = "45 / 15 HIIT",
    cycleCount = 8,
    blocks = listOf(
        Block(Uuid.random().toString(), "Warm up", 120.seconds, ColorPalette.warmupArgb, BlockRole.Warmup),
        Block(Uuid.random().toString(), "Work", 45.seconds, ColorPalette.defaultWorkArgb, BlockRole.Cycle),
        Block(Uuid.random().toString(), "Rest", 15.seconds, ColorPalette.defaultRestArgb, BlockRole.Cycle),
        Block(Uuid.random().toString(), "Cool down", 120.seconds, ColorPalette.lowIntensityArgb, BlockRole.Cooldown),
    ),
)
