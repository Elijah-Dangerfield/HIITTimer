@file:Suppress("DEPRECATION")

package com.dangerfield.hiittimer.features.timers.impl.runner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.libraries.ui.components.Card
import com.dangerfield.hiittimer.libraries.ui.components.icon.CircleIcon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.system.color.ColorResource
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import kotlin.time.Duration

@Composable
fun RunnerScreen(
    viewModel: RunnerViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is RunnerEvent.Exit) onExit()
        }
    }

    val engine = state.engineState
    val backgroundArgb = when (engine) {
        is RunnerState.Running -> engine.currentBlock.colorArgb
        is RunnerState.Paused -> engine.snapshot.currentBlock.colorArgb
        is RunnerState.Finished -> 0xFF1B1B1F.toInt()
        else -> 0xFF000000.toInt()
    }
    val bg = Color(backgroundArgb)
    val onBgColor = ColorPalette.onColorFor(backgroundArgb)
    val onBg: ColorResource = ColorResource.FromColor(onBgColor, "on-block")

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimension.D700, vertical = Dimension.D900),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                title = state.timer?.name.orEmpty(),
                onExit = { viewModel.takeAction(RunnerAction.Stop) },
                onBg = onBg,
            )

            Spacer(modifier = Modifier.height(Dimension.D500))

            when (engine) {
                is RunnerState.Running -> RunningBody(engine, onBg = onBg, paused = false)
                is RunnerState.Paused -> RunningBody(engine.snapshot, onBg = onBg, paused = true)
                is RunnerState.Finished -> FinishedBody(onBg = onBg)
                RunnerState.Idle -> Spacer(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.weight(1f))

            BottomControls(
                paused = engine is RunnerState.Paused,
                finished = engine is RunnerState.Finished,
                soundMode = state.soundMode,
                onToggleSound = { viewModel.takeAction(RunnerAction.ToggleSound) },
                onPauseResume = {
                    if (engine is RunnerState.Paused) viewModel.takeAction(RunnerAction.Resume)
                    else viewModel.takeAction(RunnerAction.Pause)
                },
                onSkip = { viewModel.takeAction(RunnerAction.Skip) },
                onBg = onBg,
                blockColor = bg,
            )

            if (state.showProgressBar && engine is RunnerState.Running) {
                Spacer(modifier = Modifier.height(Dimension.D700))
                TotalProgress(
                    elapsed = engine.elapsedTotal,
                    total = engine.totalDuration,
                    onBg = onBg,
                )
            } else {
                Spacer(modifier = Modifier.height(Dimension.D700))
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onExit: () -> Unit, onBg: ColorResource) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(
            icon = Icons.Close("Exit runner"),
            iconSize = IconSize.Medium,
            padding = Dimension.D300,
            backgroundColor = onBg.withAlpha(0.12f),
            contentColor = onBg,
            onClick = onExit,
        )
        Text(
            text = title,
            typography = AppTheme.typography.Label.L600,
            color = onBg,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.size(Dimension.D1200))
    }
}

@Composable
private fun RunningBody(engine: RunnerState.Running, onBg: ColorResource, paused: Boolean) {
    SummaryCards(engine = engine, onBg = onBg)

    Spacer(modifier = Modifier.height(Dimension.D1300))

    ProgressRing(
        remaining = engine.remaining,
        blockDuration = engine.currentBlock.duration,
        onBg = onBg,
    )

    Spacer(modifier = Modifier.height(Dimension.D900))

    Text(
        text = if (paused) "Paused" else engine.currentBlock.name,
        typography = AppTheme.typography.Heading.H700,
        color = onBg,
        textAlign = TextAlign.Center,
    )

    if (!paused) {
        engine.nextBlock?.let { next ->
            Spacer(modifier = Modifier.height(Dimension.D300))
            Text(
                text = "Up next: ${next.name}",
                typography = AppTheme.typography.Label.L400,
                color = onBg.withAlpha(0.7f),
            )
        }
    }
}

@Composable
private fun FinishedBody(onBg: ColorResource) {
    Spacer(modifier = Modifier.height(Dimension.D1500))
    Text(
        text = "Done",
        typography = AppTheme.typography.Display.D1500,
        color = onBg,
    )
    Spacer(modifier = Modifier.height(Dimension.D500))
    Text(
        text = "Great work.",
        typography = AppTheme.typography.Body.B500,
        color = onBg.withAlpha(0.75f),
    )
}

@Composable
private fun SummaryCards(engine: RunnerState.Running, onBg: ColorResource) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        SummaryPill(
            label = "Block",
            value = "${engine.blockIndex + 1}/${engine.timer.blocks.size}",
            onBg = onBg,
            modifier = Modifier.weight(1f),
        )
        SummaryPill(
            label = "Round",
            value = "${engine.cycleIndex + 1}/${engine.timer.cycleCount}",
            onBg = onBg,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryPill(label: String, value: String, onBg: ColorResource, modifier: Modifier) {
    Card(
        modifier = modifier,
        color = onBg.withAlpha(0.14f),
        contentColor = onBg,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                typography = AppTheme.typography.Label.L500,
                color = onBg,
            )
            Text(
                text = value,
                typography = AppTheme.typography.Heading.H600,
                color = onBg,
            )
        }
    }
}

@Composable
private fun ProgressRing(
    remaining: Duration,
    blockDuration: Duration,
    onBg: ColorResource,
) {
    val totalMs = blockDuration.inWholeMilliseconds.coerceAtLeast(1L)
    val remainingMs = remaining.inWholeMilliseconds.coerceAtLeast(0L)
    val fraction = (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = fraction, label = "ring-progress")

    val displaySeconds = ((remainingMs + 999L) / 1000L).toInt()
    val timeText = formatSeconds(displaySeconds)
    val trackColor = onBg.color.copy(alpha = 0.18f)
    val progressColor = onBg.color

    BoxWithConstraints(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = timeText,
            typography = AppTheme.typography.Display.D1500,
            color = onBg,
        )
    }
}

@Composable
private fun BottomControls(
    paused: Boolean,
    finished: Boolean,
    soundMode: SoundMode,
    onToggleSound: () -> Unit,
    onPauseResume: () -> Unit,
    onSkip: () -> Unit,
    onBg: ColorResource,
    blockColor: Color,
) {
    if (finished) {
        Spacer(modifier = Modifier.height(Dimension.D1500))
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val soundIcon = if (soundMode == SoundMode.Off) Icons.VolumeOff("Enable sound") else Icons.VolumeUp("Mute")
        CircleIcon(
            icon = soundIcon,
            iconSize = IconSize.Medium,
            padding = Dimension.D500,
            backgroundColor = onBg.withAlpha(0.14f),
            contentColor = onBg,
            onClick = onToggleSound,
        )
        CircleIcon(
            icon = if (paused) Icons.Play("Resume") else Icons.Pause("Pause"),
            iconSize = IconSize.Largest,
            padding = Dimension.D800,
            backgroundColor = onBg,
            contentColor = ColorResource.FromColor(blockColor, "play-fg"),
            onClick = onPauseResume,
        )
        CircleIcon(
            icon = Icons.SkipNext("Skip block"),
            iconSize = IconSize.Medium,
            padding = Dimension.D500,
            backgroundColor = onBg.withAlpha(0.14f),
            contentColor = onBg,
            onClick = onSkip,
        )
    }
}

@Composable
private fun TotalProgress(elapsed: Duration, total: Duration, onBg: ColorResource) {
    val totalMs = total.inWholeMilliseconds.coerceAtLeast(1L)
    val elapsedMs = elapsed.inWholeMilliseconds.coerceAtLeast(0L)
    val fraction = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = fraction, label = "total-progress")

    val remainingSeconds = ((totalMs - elapsedMs) / 1000L).toInt().coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Total",
                typography = AppTheme.typography.Label.L400,
                color = onBg.withAlpha(0.7f),
            )
            Text(
                text = formatSeconds(remainingSeconds) + " left",
                typography = AppTheme.typography.Label.L400,
                color = onBg.withAlpha(0.7f),
            )
        }
        Spacer(modifier = Modifier.height(Dimension.D300))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(onBg.color.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(6.dp)
                    .background(onBg.color),
            )
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}
