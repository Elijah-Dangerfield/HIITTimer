package com.dangerfield.hiittimer.features.timers

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Timer(
    val id: String,
    val name: String,
    val cycleCount: Int,
    val blocks: List<Block>,
) {
    val cycleDuration: Duration = blocks.fold(0.seconds) { acc, b -> acc + b.duration }
    val totalDuration: Duration = cycleDuration * cycleCount
}

data class Block(
    val id: String,
    val name: String,
    val duration: Duration,
    val colorArgb: Int,
)
