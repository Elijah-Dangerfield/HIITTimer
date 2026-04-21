package com.dangerfield.hiittimer.features.timers.impl.runner

import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
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
import kotlin.time.Duration.Companion.seconds

sealed interface RunnerState {
    data object Idle : RunnerState

    /**
     * 3-2-1 "Get Ready / Get Set / Go!" countdown before the first block.
     * Not counted in the workout's elapsed time and not tied to a [Block].
     */
    data class PreRoll(
        val timer: Timer,
        val firstBlock: Block,
        val remaining: Duration,
        val phase: Int,
    ) : RunnerState

    data class Running(
        val timer: Timer,
        val blockIndex: Int,
        val currentBlock: Block,
        val nextBlock: Block?,
        val phase: BlockRole,
        val cycleRoundIndex: Int,
        val remaining: Duration,
        val elapsedTotal: Duration,
    ) : RunnerState {
        val totalDuration: Duration get() = timer.totalDuration
    }

    data class Paused(val snapshot: Running) : RunnerState

    data class Finished(
        val timer: Timer,
        val elapsed: Duration,
        val completedBlockCount: Int,
        val completedRounds: Int,
    ) : RunnerState
}

sealed interface RunnerCue {
    data class BlockStart(val block: Block) : RunnerCue
    data class Countdown(val remainingSeconds: Int) : RunnerCue
    data class Prepare(val phase: Int) : RunnerCue
    data class Halfway(val block: Block) : RunnerCue
    data object Finish : RunnerCue
}

class RunnerEngine(private val timer: Timer) {

    private val sequence: List<SequenceEntry> = buildSequence(timer)

    private val _state = MutableStateFlow<RunnerState>(RunnerState.Idle)
    val state: StateFlow<RunnerState> = _state.asStateFlow()

    private val _cues = MutableSharedFlow<RunnerCue>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val cues: Flow<RunnerCue> = _cues.asSharedFlow()

    private val tickIntervalMs = 50L
    private val preRollDuration = 3.seconds
    private var lastSecondEmitted = -1
    private var lastPreRollPhaseEmitted = -1
    private var halfwayEmitted = false
    private var running = false

    suspend fun start() {
        if (sequence.isEmpty()) {
            _state.value = RunnerState.Finished(
                timer = timer,
                elapsed = Duration.ZERO,
                completedBlockCount = 0,
                completedRounds = 0,
            )
            return
        }
        _state.value = RunnerState.PreRoll(
            timer = timer,
            firstBlock = sequence.first().block,
            remaining = preRollDuration,
            phase = preRollDuration.inWholeSeconds.toInt(),
        )
        lastPreRollPhaseEmitted = preRollDuration.inWholeSeconds.toInt()
        emitCue(RunnerCue.Prepare(lastPreRollPhaseEmitted))
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

    fun resetCurrentBlock() {
        val current = _state.value as? RunnerState.Running ?: return
        lastSecondEmitted = -1
        halfwayEmitted = false
        val elapsedDelta = current.currentBlock.duration - current.remaining
        _state.value = current.copy(
            remaining = current.currentBlock.duration,
            elapsedTotal = (current.elapsedTotal - elapsedDelta).coerceAtLeast(Duration.ZERO),
        )
        emitCue(RunnerCue.BlockStart(current.currentBlock))
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
            when (val current = _state.value) {
                is RunnerState.PreRoll -> tickPreRoll(current, delta)
                is RunnerState.Running -> tick(delta)
                else -> Unit
            }
            val current = _state.value
            if (current is RunnerState.Finished || current is RunnerState.Paused || current is RunnerState.Idle) {
                running = false
            }
        }
    }

    private fun tickPreRoll(current: RunnerState.PreRoll, delta: Duration) {
        val newRemaining = current.remaining - delta
        if (newRemaining <= Duration.ZERO) {
            lastSecondEmitted = -1
            halfwayEmitted = false
            _state.value = runningFor(
                index = 0,
                elapsed = Duration.ZERO,
                remaining = sequence.first().block.duration,
            )
            emitCue(RunnerCue.BlockStart(sequence.first().block))
            return
        }
        val phase = ((newRemaining.inWholeMilliseconds + 999L) / 1000L).toInt()
        if (phase != lastPreRollPhaseEmitted) {
            lastPreRollPhaseEmitted = phase
            if (phase in 1..3) emitCue(RunnerCue.Prepare(phase))
        }
        _state.value = current.copy(remaining = newRemaining, phase = phase)
    }

    private fun tick(delta: Duration) {
        val current = _state.value as? RunnerState.Running ?: return
        val newRemaining = current.remaining - delta
        val newElapsed = current.elapsedTotal + delta
        if (newRemaining <= Duration.ZERO) {
            advanceBlock(current, forcedAdvance = false)
            return
        }
        val remainingSecs = ((newRemaining.inWholeMilliseconds + 999L) / 1000L).toInt()
        if (remainingSecs != lastSecondEmitted) {
            lastSecondEmitted = remainingSecs
            if (remainingSecs in 1..3) emitCue(RunnerCue.Countdown(remainingSecs))
        }
        val blockDuration = current.currentBlock.duration
        if (blockDuration >= MIN_HALFWAY_BLOCK_DURATION) {
            val halfwayMark = blockDuration / 2
            if (!halfwayEmitted && (blockDuration - newRemaining) >= halfwayMark) {
                halfwayEmitted = true
                emitCue(RunnerCue.Halfway(current.currentBlock))
            }
        }
        _state.value = current.copy(
            remaining = newRemaining,
            elapsedTotal = newElapsed,
        )
    }

    private fun advanceBlock(current: RunnerState.Running, forcedAdvance: Boolean) {
        lastSecondEmitted = -1
        halfwayEmitted = false
        val elapsedAfter = if (forcedAdvance) current.elapsedTotal + current.remaining else current.elapsedTotal
        val nextIdx = current.blockIndex + 1
        if (nextIdx < sequence.size) {
            _state.value = runningFor(
                index = nextIdx,
                elapsed = elapsedAfter,
                remaining = sequence[nextIdx].block.duration,
            )
            emitCue(RunnerCue.BlockStart(sequence[nextIdx].block))
            return
        }
        _state.value = RunnerState.Finished(
            timer = timer,
            elapsed = elapsedAfter,
            completedBlockCount = sequence.size,
            completedRounds = timer.cycleCount,
        )
        emitCue(RunnerCue.Finish)
    }

    private fun runningFor(index: Int, elapsed: Duration, remaining: Duration): RunnerState.Running {
        val entry = sequence[index]
        val next = sequence.getOrNull(index + 1)?.block
        return RunnerState.Running(
            timer = timer,
            blockIndex = index,
            currentBlock = entry.block,
            nextBlock = next,
            phase = entry.phase,
            cycleRoundIndex = entry.cycleRoundIndex,
            remaining = remaining,
            elapsedTotal = elapsed,
        )
    }

    private fun emitCue(cue: RunnerCue) {
        _cues.tryEmit(cue)
    }

    private data class SequenceEntry(
        val block: Block,
        val phase: BlockRole,
        /** 0-based round within cycle phase; 0 for warmup and cooldown. */
        val cycleRoundIndex: Int,
    )

    private companion object {
        /** Skip the halfway cue for blocks shorter than this. Below 10s the halfway mark
         *  collides with or is immediately chased by the 3-2-1 countdown, making it noise. */
        val MIN_HALFWAY_BLOCK_DURATION: Duration = 10.seconds

        fun buildSequence(timer: Timer): List<SequenceEntry> = buildList {
            timer.warmupBlocks.forEach { add(SequenceEntry(it, BlockRole.Warmup, 0)) }
            repeat(timer.cycleCount) { round ->
                timer.cycleBlocks.forEach { add(SequenceEntry(it, BlockRole.Cycle, round)) }
            }
            timer.cooldownBlocks.forEach { add(SequenceEntry(it, BlockRole.Cooldown, 0)) }
        }
    }
}
