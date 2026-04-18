package com.dangerfield.hiittimer.features.timers.impl.list

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.libraries.ui.components.Card
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.icon.CircleIcon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension

@Composable
fun TimerListScreen(
    viewModel: TimerListViewModel,
    onOpenRunner: (String) -> Unit,
    onOpenBuilder: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TimerListEvent.OpenBuilder -> onOpenBuilder(event.timerId)
                is TimerListEvent.OpenRunner -> onOpenRunner(event.timerId)
                TimerListEvent.OpenSettings -> onOpenSettings()
            }
        }
    }

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(
                onOpenSettings = { viewModel.takeAction(TimerListAction.OpenSettings) },
            )

            if (state.timers.isEmpty() && !state.loading) {
                EmptyState(onCreate = { viewModel.takeAction(TimerListAction.CreateNew) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimension.D700,
                        end = Dimension.D700,
                        top = Dimension.D500,
                        bottom = Dimension.D1800,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimension.D500),
                ) {
                    items(state.timers, key = { it.id }) { timer ->
                        TimerRow(
                            timer = timer,
                            onRun = { viewModel.takeAction(TimerListAction.Open(timer.id)) },
                            onEdit = { viewModel.takeAction(TimerListAction.Edit(timer.id)) },
                            onDuplicate = { viewModel.takeAction(TimerListAction.Duplicate(timer.id)) },
                            onDelete = { viewModel.takeAction(TimerListAction.Delete(timer.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D700, vertical = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Timers",
            typography = AppTheme.typography.Heading.H800,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            icon = Icons.Settings("Settings"),
            onClick = onOpenSettings,
        )
    }
    HorizontalDivider()
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(Dimension.D1200), contentAlignment = Alignment.Center) {
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
private fun TimerRow(
    timer: Timer,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onEdit,
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timer.name.ifBlank { "Untitled" },
                        typography = AppTheme.typography.Heading.H600,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(Dimension.D200))
                    Text(
                        text = timer.subtitle(),
                        typography = AppTheme.typography.Body.B400,
                        color = AppTheme.colors.textSecondary,
                    )
                }
                CircleIcon(
                    icon = Icons.Play("Start timer"),
                    iconSize = IconSize.Medium,
                    padding = Dimension.D500,
                    backgroundColor = AppTheme.colors.accentPrimary,
                    contentColor = AppTheme.colors.onAccentPrimary,
                    onClick = onRun,
                )
            }
            if (timer.blocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimension.D500))
                BlockPreviewStrip(timer.blocks)
            }
            Spacer(modifier = Modifier.height(Dimension.D300))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    icon = Icons.Copy("Duplicate"),
                    onClick = onDuplicate,
                    size = com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton.Size.Small,
                )
                IconButton(
                    icon = Icons.Delete("Delete"),
                    onClick = onDelete,
                    size = com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton.Size.Small,
                )
            }
        }
    }
}

@Composable
private fun BlockPreviewStrip(blocks: List<Block>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val totalSeconds = blocks.sumOf { it.duration.inWholeSeconds.toInt() }.coerceAtLeast(1)
        blocks.forEach { block ->
            val weight = block.duration.inWholeSeconds.toFloat() / totalSeconds.toFloat()
            Box(
                modifier = Modifier
                    .weight(weight.coerceAtLeast(0.02f))
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(block.colorArgb)),
            )
        }
    }
}

private fun Timer.subtitle(): String {
    val mins = totalDuration.inWholeSeconds.toInt() / 60
    val secs = totalDuration.inWholeSeconds.toInt() % 60
    val durText = when {
        mins > 0 && secs > 0 -> "${mins}m ${secs}s"
        mins > 0 -> "${mins}m"
        else -> "${secs}s"
    }
    val cycles = if (cycleCount > 1) " · ${cycleCount}x" else ""
    return "${blocks.size} blocks · $durText$cycles"
}

