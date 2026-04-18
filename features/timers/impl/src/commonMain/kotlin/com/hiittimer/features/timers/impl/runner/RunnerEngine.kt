package com.dangerfield.hiittimer.features.timers.impl.runner

import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.Timer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Duration

sealed interface RunnerState {
    data object Idle : RunnerState

    data class Running(
        val timer: Timer,
        val blockIndex: Int,
        val cycleIndex: Int,
        val remaining: Duration,
        val elapsedTotal: Duration,
    ) : RunnerState {
        val currentBlock: Block get() = timer.blocks[blockIndex]
        val nextBlock: Block?
            get() {
                val nextIdx = blockIndex + 1
                return when {
                    nextIdx < timer.blocks.size -> timer.blocks[nextIdx]
                    cycleIndex + 1 < timer.cycleCount -> timer.blocks.firstOrNull()
                    else -> null
                }
            }
        val totalDuration: Duration get() = timer.totalDuration
    }

    data class Paused(val snapshot: Running) : RunnerState

    data class Finished(val timer: Timer) : RunnerState
}

sealed interface RunnerCue {
    data class BlockStart(val block: Block) : RunnerCue
    data class Countdown(val remainingSeconds: Int) : RunnerCue
    data class Halfway(val block: Block) : RunnerCue
    data object Finish : RunnerCue
}

class RunnerEngine(private val timer: Timer) {

    private val _state = MutableStateFlow<RunnerState>(RunnerState.Idle)
    val state: StateFlow<RunnerState> = _state.asStateFlow()

    private val _cues = MutableSharedFlow<RunnerCue>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val cues: Flow<RunnerCue> = _cues.asSharedFlow()

    private val tickIntervalMs = 50L
    private var lastSecondEmitted = -1
    private var halfwayEmitted = false
    private var running = false

    suspend fun start() {
        if (timer.blocks.isEmpty()) {
            _state.value = RunnerState.Finished(timer)
            return
        }
        _state.value = RunnerState.Running(
            timer = timer,
            blockIndex = 0,
            cycleIndex = 0,
            remaining = timer.blocks.first().duration,
            elapsedTotal = Duration.ZERO,
        )
        emitCue(RunnerCue.BlockStart(timer.blocks.first()))
        loop()
    }

    fun pause() {
        running = false
        _state.update { current ->
            when (current) {
                is RunnerState.Running -> RunnerState.Paused(current)
                else -> current
            }
        }
    }

    suspend fun resume() {
        val paused = (_state.value as? RunnerState.Paused) ?: return
        _state.value = paused.snapshot
        loop()
    }

    fun skip() {
        val running = _state.value as? RunnerState.Running ?: return
        advanceBlock(running, forcedAdvance = true)
    }

    fun stop() {
        running = false
        _state.value = RunnerState.Idle
    }

    private suspend fun loop() {
        running = true
        var last = Clock.System.now()
        lastSecondEmitted = -1
        halfwayEmitted = false
        while (running) {
            delay(tickIntervalMs)
            val now = Clock.System.now()
            val delta = (now - last)
            last = now
            tick(delta)
            val current = _state.value
            if (current is RunnerState.Finished || current is RunnerState.Paused || current is RunnerState.Idle) {
                running = false
            }
        }
    }

    private fun tick(delta: Duration) {
        val current = _state.value as? RunnerState.Running ?: return
        val newRemaining = current.remaining - delta
        val newElapsed = current.elapsedTotal + delta
        if (newRemaining <= Duration.ZERO) {
            advanceBlock(current, forcedAdvance = false)
            return
        }
        val remainingSecs = newRemaining.inWholeSeconds.toInt()
        if (remainingSecs != lastSecondEmitted) {
            lastSecondEmitted = remainingSecs
            if (remainingSecs in 1..3) emitCue(RunnerCue.Countdown(remainingSecs))
        }
        val halfwayMark = current.currentBlock.duration / 2
        if (!halfwayEmitted && (current.currentBlock.duration - newRemaining) >= halfwayMark) {
            halfwayEmitted = true
            emitCue(RunnerCue.Halfway(current.currentBlock))
        }
        _state.value = current.copy(
            remaining = newRemaining,
            elapsedTotal = newElapsed,
        )
    }

    private fun advanceBlock(current: RunnerState.Running, forcedAdvance: Boolean) {
        lastSecondEmitted = -1
        halfwayEmitted = false
        val nextBlockIdx = current.blockIndex + 1
        val elapsedAfter = if (forcedAdvance) {
            current.elapsedTotal + current.remaining
        } else current.elapsedTotal
        if (nextBlockIdx < current.timer.blocks.size) {
            val next = current.timer.blocks[nextBlockIdx]
            _state.value = current.copy(
                blockIndex = nextBlockIdx,
                remaining = next.duration,
                elapsedTotal = elapsedAfter,
            )
            emitCue(RunnerCue.BlockStart(next))
            return
        }
        val nextCycleIdx = current.cycleIndex + 1
        if (nextCycleIdx < current.timer.cycleCount) {
            val next = current.timer.blocks.first()
            _state.value = current.copy(
                blockIndex = 0,
                cycleIndex = nextCycleIdx,
                remaining = next.duration,
                elapsedTotal = elapsedAfter,
            )
            emitCue(RunnerCue.BlockStart(next))
            return
        }
        _state.value = RunnerState.Finished(current.timer)
        emitCue(RunnerCue.Finish)
    }

    private fun emitCue(cue: RunnerCue) {
        _cues.tryEmit(cue)
    }
}
