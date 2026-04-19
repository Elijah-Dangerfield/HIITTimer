@file:Suppress("DEPRECATION")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dangerfield.hiittimer.features.timers.impl.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.seconds
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.Surface
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonDanger
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonGhost
import com.dangerfield.hiittimer.libraries.ui.components.dialog.Dialog
import com.dangerfield.hiittimer.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.hiittimer.libraries.ui.components.icon.CircleIcon
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import com.dangerfield.hiittimer.system.Radii

@Composable
fun TimerListScreen(
    viewModel: TimerListViewModel,
    onOpenDetail: (timerId: String, isNew: Boolean) -> Unit,
    onStart: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TimerListEvent.OpenDetail -> onOpenDetail(event.timerId, event.isNew)
                is TimerListEvent.StartRunner -> onStart(event.timerId)
                TimerListEvent.OpenSettings -> onOpenSettings()
            }
        }
    }

    TimerListContent(
        state = state,
        onOpenSettings = { viewModel.takeAction(TimerListAction.OpenSettings) },
        onCreate = { viewModel.takeAction(TimerListAction.CreateNew) },
        onOpenTimer = { viewModel.takeAction(TimerListAction.Open(it)) },
        onStartTimer = { viewModel.takeAction(TimerListAction.Start(it)) },
        onRequestDelete = { viewModel.takeAction(TimerListAction.RequestDelete(it)) },
        onConfirmDelete = { viewModel.takeAction(TimerListAction.ConfirmDelete) },
        onDismissDelete = { viewModel.takeAction(TimerListAction.DismissDelete) },
    )
}

@Composable
private fun TimerListContent(
    state: TimerListState,
    onOpenSettings: () -> Unit,
    onCreate: () -> Unit,
    onOpenTimer: (String) -> Unit,
    onStartTimer: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
) {
    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(onOpenSettings = onOpenSettings, onCreate = onCreate)

            if (state.timers.isEmpty() && !state.loading) {
                EmptyState(onCreate = onCreate)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimension.D700,
                        end = Dimension.D700,
                        top = Dimension.D700,
                        bottom = Dimension.D1800,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimension.D500),
                ) {
                    items(state.timers, key = { it.id }) { timer ->
                        SwipeToDeleteTimerCard(
                            timer = timer,
                            onTap = { onOpenTimer(timer.id) },
                            onStart = { onStartTimer(timer.id) },
                            onRequestDelete = { onRequestDelete(timer.id) },
                        )
                    }
                }
            }
        }
    }

    if (state.pendingDeleteTimerId != null) {
        val pending = state.timers.firstOrNull { it.id == state.pendingDeleteTimerId }
        DeleteTimerConfirmationDialog(
            timerName = pending?.name?.ifBlank { "Untitled" } ?: "this timer",
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}

@Composable
private fun SwipeToDeleteTimerCard(
    timer: Timer,
    onTap: () -> Unit,
    onStart: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRequestDelete()
            false
        },
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.danger.color),
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.danger.color)
                        .padding(horizontal = Dimension.D900),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        icon = Icons.Delete("Delete"),
                        size = IconSize.Medium,
                        color = AppTheme.colors.onAccentPrimary,
                    )
                }
            },
        ) {
            TimerCard(timer = timer, onTap = onTap, onStart = onStart)
        }
    }
}

@Composable
private fun DeleteTimerConfirmationDialog(
    timerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                    text = "Delete \"$timerName\"?",
                    typography = AppTheme.typography.Heading.H700,
                )
                Spacer(modifier = Modifier.height(Dimension.D400))
                Text(
                    text = "This will remove the timer and its blocks. You can't undo this.",
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(Dimension.D700))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
                ) {
                    ButtonGhost(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    ButtonDanger(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit, onCreate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D700, vertical = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Timers",
            typography = AppTheme.typography.Heading.H1000,
            modifier = Modifier.weight(1f),
        )
        IconButton(icon = Icons.Settings("Settings"), onClick = onOpenSettings)
        IconButton(icon = Icons.Add("New timer"), onClick = onCreate)
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(Dimension.D1200),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No timers yet",
                typography = AppTheme.typography.Heading.H700,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimension.D500))
            Text(
                text = "Tap the button below to build your first interval timer.",
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimension.D1200))
            CircleIcon(
                icon = Icons.Add("Create timer"),
                iconSize = IconSize.Largest,
                padding = Dimension.D800,
                backgroundColor = AppTheme.colors.accentPrimary,
                contentColor = AppTheme.colors.onAccentPrimary,
                onClick = onCreate,
            )
        }
    }
}

@Composable
private fun TimerCard(timer: Timer, onTap: () -> Unit, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surfacePrimary.color)
            .clickable { onTap() }
            .padding(Dimension.D700),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.name.ifBlank { "Untitled" },
                    typography = AppTheme.typography.Heading.H800,
                    color = AppTheme.colors.onSurfacePrimary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(Dimension.D200))
                Text(
                    text = timer.subtitle(),
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                )
            }
            CircleIcon(
                icon = Icons.Play("Start"),
                iconSize = IconSize.Medium,
                padding = Dimension.D400,
                backgroundColor = AppTheme.colors.accentPrimary,
                contentColor = AppTheme.colors.onAccentPrimary,
                onClick = onStart.takeIf { timer.blocks.isNotEmpty() },
            )
        }
        if (timer.blocks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimension.D600))
            BlockStrip(blocks = timer.orderedBlocks)
        }
    }
}

@Composable
private fun BlockStrip(blocks: List<Block>) {
    val totalSeconds = blocks.sumOf { it.duration.inWholeSeconds.toInt() }.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        blocks.forEach { block ->
            val weight = (block.duration.inWholeSeconds.toFloat() / totalSeconds.toFloat())
                .coerceAtLeast(0.02f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(block.colorArgb)),
            )
        }
    }
}

private fun Timer.subtitle(): String {
    val secs = totalDuration.inWholeSeconds.toInt()
    val mins = secs / 60
    val rem = secs % 60
    val durText = when {
        mins > 0 && rem > 0 -> "${mins}m ${rem}s"
        mins > 0 -> "${mins}m"
        else -> "${secs}s"
    }
    val cycles = if (cycleCount > 1) " · ${cycleCount}x" else ""
    return "${blocks.size} blocks · $durText$cycles"
}

private val sampleTimers: List<Timer> = listOf(
    Timer(
        id = "t1",
        name = "Tabata",
        cycleCount = 8,
        blocks = listOf(
            Block("w", "Warm up", 60.seconds, ColorPalette.warmupArgb, BlockRole.Warmup),
            Block("c1", "Work", 20.seconds, ColorPalette.defaultWorkArgb, BlockRole.Cycle),
            Block("c2", "Rest", 10.seconds, ColorPalette.defaultRestArgb, BlockRole.Cycle),
        ),
    ),
    Timer(
        id = "t2",
        name = "EMOM 10",
        cycleCount = 10,
        blocks = listOf(
            Block("c", "Work", 60.seconds, ColorPalette.defaultWorkArgb, BlockRole.Cycle),
        ),
    ),
    Timer(id = "t3", name = "", cycleCount = 1, blocks = emptyList()),
)

@Composable
@Preview
private fun TimerListContentPreview() {
    PreviewContent {
        TimerListContent(
            state = TimerListState(timers = sampleTimers, loading = false),
            onOpenSettings = {}, onCreate = {},
            onOpenTimer = {}, onStartTimer = {},
            onRequestDelete = {}, onConfirmDelete = {}, onDismissDelete = {},
        )
    }
}

@Composable
@Preview
private fun TimerListContentEmptyPreview() {
    PreviewContent {
        TimerListContent(
            state = TimerListState(timers = emptyList(), loading = false),
            onOpenSettings = {}, onCreate = {},
            onOpenTimer = {}, onStartTimer = {},
            onRequestDelete = {}, onConfirmDelete = {}, onDismissDelete = {},
        )
    }
}
