@file:Suppress("DEPRECATION")
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dangerfield.hiittimer.features.timers.impl.runner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.seconds
import com.dangerfield.hiittimer.libraries.ui.Elevation
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.Surface
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonDanger
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonGhost
import com.dangerfield.hiittimer.libraries.ui.components.dialog.Dialog
import com.dangerfield.hiittimer.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.hiittimer.libraries.ui.components.icon.CircleIcon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.system.color.ColorResource
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import com.dangerfield.hiittimer.system.Radii
import kotlin.time.Duration
import androidx.compose.ui.backhandler.BackHandler

@Composable
fun RunnerScreen(
    viewModel: RunnerViewModel,
    onExit: () -> Unit,
    onSupportClick: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is RunnerEvent.Exit) onExit()
        }
    }

    RunnerContent(
        state = state,
        onExit = onExit,
        onToggleSound = { viewModel.takeAction(RunnerAction.ToggleSound) },
        onPause = { viewModel.takeAction(RunnerAction.Pause) },
        onResume = { viewModel.takeAction(RunnerAction.Resume) },
        onSkip = { viewModel.takeAction(RunnerAction.Skip) },
        onStop = { viewModel.takeAction(RunnerAction.Stop) },
        onSupportClick = onSupportClick,
    )
}

@Composable
private fun RunnerContent(
    state: RunnerUiState,
    onExit: () -> Unit,
    onToggleSound: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    onSupportClick: () -> Unit,
) {
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

    var showExitConfirm by remember { mutableStateOf(false) }
    val running = engine is RunnerState.Running
    BackHandler(enabled = running) {
        showExitConfirm = true
    }

    val requestExit: () -> Unit = {
        if (running) showExitConfirm = true else onStop()
    }

    Screen(
        modifier = Modifier.fillMaxSize(),
        containerColor = bg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimension.D700, vertical = Dimension.D500),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                title = state.timer?.name.orEmpty(),
                onExit = requestExit,
                onBg = onBg,
            )

            Spacer(modifier = Modifier.height(Dimension.D500))

            when (engine) {
                is RunnerState.Running -> RunningBody(engine, onBg = onBg, paused = false)
                is RunnerState.Paused -> RunningBody(engine.snapshot, onBg = onBg, paused = true)
                is RunnerState.Finished -> FinishedBody(
                    engine = engine,
                    onBg = onBg,
                    onExit = onExit,
                    onSupportClick = onSupportClick,
                )
                RunnerState.Idle -> Spacer(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.weight(1f))

            BottomControls(
                paused = engine is RunnerState.Paused,
                finished = engine is RunnerState.Finished,
                soundMode = state.soundMode,
                onToggleSound = onToggleSound,
                onPauseResume = { if (engine is RunnerState.Paused) onResume() else onPause() },
                onSkip = onSkip,
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

    if (showExitConfirm) {
        ExitConfirmDialog(
            onDismiss = { showExitConfirm = false },
            onConfirm = {
                showExitConfirm = false
                onStop()
            },
        )
    }
}

@Composable
private fun ExitConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val dialogState = rememberDialogState()
    Dialog(state = dialogState, onDismissRequest = onDismiss) {
        Surface(
            color = AppTheme.colors.surfacePrimary,
            contentColor = AppTheme.colors.onSurfacePrimary,
            radius = Radii.Card,
            contentPadding = PaddingValues(Dimension.D700),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Stop workout?",
                    typography = AppTheme.typography.Heading.H700,
                )
                Spacer(modifier = Modifier.height(Dimension.D400))
                Text(
                    text = "You'll lose your progress for this run.",
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(Dimension.D700))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
                ) {
                    ButtonGhost(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Keep going")
                    }
                    ButtonDanger(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                }
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
        typography = AppTheme.typography.Display.D1500,
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
private fun FinishedBody(
    engine: RunnerState.Finished,
    onBg: ColorResource,
    onExit: () -> Unit,
    onSupportClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(Dimension.D1300))
    Text(
        text = "Crushed it.",
        typography = AppTheme.typography.Display.D1500,
        color = onBg,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Dimension.D300))
    Text(
        text = "Nice work on \"${engine.timer.name.ifBlank { "Untitled" }}\".",
        typography = AppTheme.typography.Body.B500,
        color = onBg.withAlpha(0.8f),
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(Dimension.D1200))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        RecapStat(
            label = "Time",
            value = formatClockLong(engine.elapsed.inWholeSeconds.toInt()),
            onBg = onBg,
            modifier = Modifier.weight(1f),
        )
        RecapStat(
            label = "Rounds",
            value = "${engine.completedRounds}x",
            onBg = onBg,
            modifier = Modifier.weight(1f),
        )
        RecapStat(
            label = "Blocks",
            value = engine.completedBlockCount.toString(),
            onBg = onBg,
            modifier = Modifier.weight(1f),
        )
    }

    if (engine.timer.blocks.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Dimension.D900))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val total = engine.timer.blocks.sumOf { it.duration.inWholeSeconds.toInt() }.coerceAtLeast(1)
            engine.timer.blocks.forEach { block ->
                val weight = (block.duration.inWholeSeconds.toFloat() / total.toFloat()).coerceAtLeast(0.02f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(androidx.compose.ui.graphics.Color(block.colorArgb)),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(Dimension.D900))

    SupportCard(onBg = onBg, onClick = onSupportClick)

    Spacer(modifier = Modifier.height(Dimension.D900))

    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(onBg.color)
            .clickable { onExit() }
            .padding(horizontal = Dimension.D1100, vertical = Dimension.D500),
    ) {
        Text(
            text = "Done",
            typography = AppTheme.typography.Label.L600,
            color = ColorResource.FromColor(
                androidx.compose.ui.graphics.Color.Black.takeIf { onBg.color == androidx.compose.ui.graphics.Color.White }
                    ?: androidx.compose.ui.graphics.Color.White,
                "done-fg",
            ),
        )
    }
}

@Composable
private fun SupportCard(onBg: ColorResource, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = onBg.withAlpha(0.08f),
        contentColor = onBg,
        radius = Radii.Card,
        elevation = Elevation.None,
        contentPadding = PaddingValues(Dimension.D600),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Enjoying HIIT Timer?",
                typography = AppTheme.typography.Label.L500,
                color = onBg,
            )
            Spacer(modifier = Modifier.height(Dimension.D300))
            Text(
                text = "I absolutely refuse to throw in ads or make people pay. If you like the app, please consider buying me a coffee.",
                typography = AppTheme.typography.Body.B400,
                color = onBg.withAlpha(0.7f),
            )
            Spacer(modifier = Modifier.height(Dimension.D400))
            Text(
                text = "Support →",
                typography = AppTheme.typography.Label.L400,
                color = onBg,
            )
        }
    }
}

@Composable
private fun RecapStat(
    label: String,
    value: String,
    onBg: ColorResource,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = onBg.withAlpha(0.14f),
        contentColor = onBg,
        radius = Radii.Card,
        elevation = Elevation.None,
        contentPadding = PaddingValues(vertical = Dimension.D600, horizontal = Dimension.D500),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label.uppercase(),
                typography = AppTheme.typography.Label.L300,
                color = onBg.withAlpha(0.7f),
            )
            Spacer(modifier = Modifier.height(Dimension.D200))
            Text(
                text = value,
                typography = AppTheme.typography.Heading.H700,
                color = onBg,
            )
        }
    }
}

private fun formatClockLong(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        "${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    } else {
        "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }
}

@Composable
private fun SummaryCards(engine: RunnerState.Running, onBg: ColorResource) {
    val timer = engine.timer
    val warmupCount = timer.warmupBlocks.size
    val cycleBlockCount = timer.cycleBlocks.size
    val cooldownCount = timer.cooldownBlocks.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        when (engine.phase) {
            com.dangerfield.hiittimer.features.timers.BlockRole.Warmup -> {
                val position = engine.blockIndex  // 0-based within warmup
                SummaryPill(
                    label = "Warm up",
                    value = if (warmupCount > 1) "${position + 1}/$warmupCount" else "",
                    onBg = onBg,
                    modifier = Modifier.weight(1f),
                )
            }
            com.dangerfield.hiittimer.features.timers.BlockRole.Cycle -> {
                val cycleBlockIdx = (engine.blockIndex - warmupCount) % cycleBlockCount.coerceAtLeast(1)
                SummaryPill(
                    label = "Block",
                    value = "${cycleBlockIdx + 1}/$cycleBlockCount",
                    onBg = onBg,
                    modifier = Modifier.weight(1f),
                )
                SummaryPill(
                    label = "Round",
                    value = "${engine.cycleRoundIndex + 1}/${timer.cycleCount}",
                    onBg = onBg,
                    modifier = Modifier.weight(1f),
                )
            }
            com.dangerfield.hiittimer.features.timers.BlockRole.Cooldown -> {
                val cooldownStart = warmupCount + cycleBlockCount * timer.cycleCount
                val position = engine.blockIndex - cooldownStart
                SummaryPill(
                    label = "Cool down",
                    value = if (cooldownCount > 1) "${position + 1}/$cooldownCount" else "",
                    onBg = onBg,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, onBg: ColorResource, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = onBg.withAlpha(0.14f),
        contentColor = onBg,
        radius = Radii.Card,
        elevation = Elevation.None,
        contentPadding = PaddingValues(Dimension.D600),
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

    var showLabel by remember { mutableStateOf(false) }
    var labelKey by remember { mutableStateOf(0) }

    LaunchedEffect(soundMode) {
        showLabel = true
        labelKey++
    }

    LaunchedEffect(labelKey) {
        if (showLabel) {
            kotlinx.coroutines.delay(1500)
            showLabel = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val soundIcon = when (soundMode) {
                SoundMode.Off -> Icons.VolumeOff("Sound off")
                SoundMode.Beeps -> Icons.VolumeUp("Sound on - Beeps")
                SoundMode.Voice -> Icons.RecordVoice("Sound on - Voice")
            }
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

        Spacer(modifier = Modifier.height(Dimension.D300))

        val labelText = when (soundMode) {
            SoundMode.Off -> "Sound off"
            SoundMode.Beeps -> "Beeps"
            SoundMode.Voice -> "Voice"
        }

        Box(modifier = Modifier.height(Dimension.D500)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showLabel,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
            ) {
                Text(
                    text = labelText,
                    typography = AppTheme.typography.Label.L400,
                    color = onBg.withAlpha(0.7f),
                )
            }
        }
    }
}

@Composable
private fun TotalProgress(elapsed: Duration, total: Duration, onBg: ColorResource) {
    val totalMs = total.inWholeMilliseconds.coerceAtLeast(1L)
    val elapsedMs = elapsed.inWholeMilliseconds.coerceAtLeast(0L)
    val fraction = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = fraction, label = "total-progress")

    val elapsedSeconds = (elapsedMs / 1000L).toInt()
    val remainingSeconds = ((totalMs - elapsedMs) / 1000L).toInt().coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatSeconds(elapsedSeconds) + " elapsed",
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

private val sampleWork = Block("w", "Work", 30.seconds, ColorPalette.defaultWorkArgb, BlockRole.Cycle)
private val sampleRest = Block("r", "Rest", 15.seconds, ColorPalette.defaultRestArgb, BlockRole.Cycle)
private val sampleRunnerTimer = Timer(
    id = "t",
    name = "Tabata",
    cycleCount = 8,
    blocks = listOf(sampleWork, sampleRest),
)
private val sampleRunning = RunnerState.Running(
    timer = sampleRunnerTimer,
    blockIndex = 0,
    currentBlock = sampleWork,
    nextBlock = sampleRest,
    phase = BlockRole.Cycle,
    cycleRoundIndex = 1,
    remaining = 12.seconds,
    elapsedTotal = 78.seconds,
)

@Composable
@Preview
private fun RunnerContentRunningPreview() {
    PreviewContent(backgroundColor = null) {
        RunnerContent(
            state = RunnerUiState(timer = sampleRunnerTimer, engineState = sampleRunning),
            onExit = {}, onToggleSound = {}, onPause = {}, onResume = {}, onSkip = {}, onStop = {}, onSupportClick = {},
        )
    }
}

@Composable
@Preview
private fun RunnerContentPausedPreview() {
    PreviewContent(backgroundColor = null) {
        RunnerContent(
            state = RunnerUiState(
                timer = sampleRunnerTimer,
                engineState = RunnerState.Paused(sampleRunning),
            ),
            onExit = {}, onToggleSound = {}, onPause = {}, onResume = {}, onSkip = {}, onStop = {}, onSupportClick = {},
        )
    }
}

@Composable
@Preview
private fun RunnerContentFinishedPreview() {
    PreviewContent(backgroundColor = null) {
        RunnerContent(
            state = RunnerUiState(
                timer = sampleRunnerTimer,
                engineState = RunnerState.Finished(
                    timer = sampleRunnerTimer,
                    elapsed = 360.seconds,
                    completedBlockCount = 16,
                    completedRounds = 8,
                ),
            ),
            onExit = {}, onToggleSound = {}, onPause = {}, onResume = {}, onSkip = {}, onStop = {}, onSupportClick = {},
        )
    }
}
