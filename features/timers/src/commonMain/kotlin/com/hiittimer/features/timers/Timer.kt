package com.dangerfield.hiittimer.features.timers

import kotlin.time.Duration

enum class BlockRole { Warmup, Cycle, Cooldown }

data class Timer(
    val id: String,
    val name: String,
    val cycleCount: Int,
    val blocks: List<Block>,
) {
    val warmupBlocks: List<Block> get() = blocks.filter { it.role == BlockRole.Warmup }
    val cycleBlocks: List<Block> get() = blocks.filter { it.role == BlockRole.Cycle }
    val cooldownBlocks: List<Block> get() = blocks.filter { it.role == BlockRole.Cooldown }

    val cycleDuration: Duration
        get() = cycleBlocks.fold(Duration.ZERO) { acc, b -> acc + b.duration }

    val totalDuration: Duration
        get() = warmupBlocks.fold(Duration.ZERO) { acc, b -> acc + b.duration } +
            cycleDuration * cycleCount +
            cooldownBlocks.fold(Duration.ZERO) { acc, b -> acc + b.duration }
}

data class Block(
    val id: String,
    val name: String,
    val duration: Duration,
    val colorArgb: Int,
    val role: BlockRole = BlockRole.Cycle,
)
