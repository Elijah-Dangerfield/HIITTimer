package com.dangerfield.hiittimer.libraries.core

import kotlinx.coroutines.flow.Flow

interface ShakeDetector {
    val shakeEvents: Flow<ShakeEvent>

    fun start()
    fun stop()
}

data class ShakeEvent(
    val timestampMs: Long,
)
