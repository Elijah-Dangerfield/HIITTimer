@file:Suppress("DEPRECATION")

package com.dangerfield.hiittimer.features.timers.impl.builder

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.Timer
import androidx.compose.foundation.text.KeyboardOptions
import com.dangerfield.hiittimer.libraries.ui.components.Card
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.icon.CircleIcon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.system.color.ColorResource
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension

@Composable
fun TimerBuilderScreen(
    viewModel: TimerBuilderViewModel,
    onBack: () -> Unit,
    onOpenBlock: (timerId: String, blockId: String) -> Unit,
    onStart: (timerId: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TimerBuilderEvent.OpenBlock -> onOpenBlock(event.timerId, event.blockId)
                is TimerBuilderEvent.Start -> onStart(event.timerId)
            }
        }
    }

    val timer = state.timer

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(
                onBack = onBack,
                canStart = timer != null && timer.blocks.isNotEmpty(),
                onStart = { viewModel.takeAction(TimerBuilderAction.Start) },
            )

            if (timer == null) return@Column

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimension.D700,
                    end = Dimension.D700,
                    top = Dimension.D500,
                    bottom = Dimension.D1500,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimension.D500),
            ) {
                item("name") {
                    OutlinedTextField(
                        value = state.nameField,
                        onValueChange = { viewModel.takeAction(TimerBuilderAction.RenameTimer(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                        ),
                        typographyToken = AppTheme.typography.Heading.H600,
                    )
                }

                item("summary") {
                    SummaryRow(timer)
                }

                item("cycles-header") {
                    SectionLabel(text = "Rounds")
                }

                item("cycles") {
                    CycleRow(
                        count = timer.cycleCount,
                        onDecrement = { viewModel.takeAction(TimerBuilderAction.ChangeCycleCount(timer.cycleCount - 1)) },
                        onIncrement = { viewModel.takeAction(TimerBuilderAction.ChangeCycleCount(timer.cycleCount + 1)) },
                    )
                }

                item("blocks-header") {
                    Spacer(modifier = Modifier.height(Dimension.D500))
                    SectionLabel(text = "Blocks")
                }

                items(timer.blocks, key = { it.id }) { block ->
                    BlockRow(
                        block = block,
                        onTap = { viewModel.takeAction(TimerBuilderAction.OpenBlock(block.id)) },
                    )
                }

                item("add") {
                    AddBlockRow(onClick = { viewModel.takeAction(TimerBuilderAction.AddBlock) })
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit, canStart: Boolean, onStart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D500, vertical = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            icon = Icons.ArrowBack("Back"),
            onClick = onBack,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Edit Timer",
            typography = AppTheme.typography.Label.L500,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        CircleIcon(
            icon = Icons.Play("Start"),
            iconSize = IconSize.Medium,
            padding = Dimension.D400,
            backgroundColor = if (canStart) AppTheme.colors.accentPrimary else AppTheme.colors.surfaceDisabled,
            contentColor = if (canStart) AppTheme.colors.onAccentPrimary else AppTheme.colors.textDisabled,
            onClick = onStart.takeIf { canStart },
        )
    }
    HorizontalDivider()
}

@Composable
private fun SummaryRow(timer: Timer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        StatTile(
            label = "Blocks",
            value = timer.blocks.size.toString(),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Total time",
            value = formatDuration(timer.totalDuration.inWholeSeconds.toInt()),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        color = AppTheme.colors.surfaceSecondary,
        contentColor = AppTheme.colors.onSurfaceSecondary,
    ) {
        Column {
            Text(
                text = label,
                typography = AppTheme.typography.Label.L400,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(Dimension.D200))
            Text(text = value, typography = AppTheme.typography.Heading.H700)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        typography = AppTheme.typography.Label.L400,
        color = AppTheme.colors.textSecondary,
    )
}

@Composable
private fun CycleRow(count: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Card(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Repeat cycle", typography = AppTheme.typography.Body.B500)
                Text(
                    text = "Run the blocks $count time${if (count == 1) "" else "s"}",
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            IconButton(
                icon = Icons.ChevronLeft("Decrease cycles"),
                onClick = onDecrement,
                enabled = count > 1,
            )
            Text(
                text = "${count}x",
                typography = AppTheme.typography.Heading.H600,
                modifier = Modifier.padding(horizontal = Dimension.D500),
            )
            IconButton(
                icon = Icons.ChevronRight("Increase cycles"),
                onClick = onIncrement,
                enabled = count < 99,
            )
        }
    }
}

@Composable
private fun BlockRow(block: Block, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimension.D1000)
                    .clip(CircleShape)
                    .background(Color(block.colorArgb)),
            )
            Spacer(modifier = Modifier.size(Dimension.D500))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = block.name, typography = AppTheme.typography.Body.B500)
                Text(
                    text = formatDuration(block.duration.inWholeSeconds.toInt()),
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            IconButton(
                icon = Icons.ChevronRight("Edit block"),
                onClick = onTap,
            )
        }
    }
}

@Composable
private fun AddBlockRow(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        color = AppTheme.colors.surfaceSecondary,
        contentColor = AppTheme.colors.onSurfaceSecondary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            com.dangerfield.hiittimer.libraries.ui.components.icon.Icon(
                icon = Icons.Add("Add block"),
                size = IconSize.Small,
            )
            Spacer(modifier = Modifier.size(Dimension.D300))
            Text(text = "Add block", typography = AppTheme.typography.Label.L500)
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return when {
        mins > 0 && secs > 0 -> "${mins}m ${secs}s"
        mins > 0 -> "${mins}m"
        else -> "${secs}s"
    }
}
