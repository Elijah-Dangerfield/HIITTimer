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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.ListItem
import com.dangerfield.hiittimer.libraries.ui.components.ListItemAccessory
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
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
                contentPadding = PaddingValues(top = Dimension.D700, bottom = Dimension.D1800),
            ) {
                item("name") {
                    Box(modifier = Modifier.padding(horizontal = Dimension.D700)) {
                        OutlinedTextField(
                            value = state.nameField,
                            onValueChange = { viewModel.takeAction(TimerBuilderAction.RenameTimer(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.Words,
                            ),
                            typographyToken = AppTheme.typography.Heading.H700,
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimension.D1000))
                }

                item("cycles") {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Rounds",
                                typography = AppTheme.typography.Body.B600,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Repeat the blocks this many times.",
                                typography = AppTheme.typography.Body.B400,
                            )
                        },
                        accessory = ListItemAccessory.Custom {
                            CycleStepper(
                                count = timer.cycleCount,
                                onDecrement = { viewModel.takeAction(TimerBuilderAction.ChangeCycleCount(timer.cycleCount - 1)) },
                                onIncrement = { viewModel.takeAction(TimerBuilderAction.ChangeCycleCount(timer.cycleCount + 1)) },
                            )
                        },
                        contentPadding = PaddingValues(horizontal = Dimension.D700, vertical = Dimension.D300),
                    )
                    Spacer(modifier = Modifier.height(Dimension.D500))
                }

                item("blocks-header") {
                    HorizontalDivider()
                    Text(
                        text = "BLOCKS",
                        typography = AppTheme.typography.Label.L400,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(
                            start = Dimension.D700,
                            end = Dimension.D700,
                            top = Dimension.D700,
                            bottom = Dimension.D400,
                        ),
                    )
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
            .padding(horizontal = Dimension.D400, vertical = Dimension.D400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(icon = Icons.ArrowBack("Back"), onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Edit timer",
            typography = AppTheme.typography.Label.L500,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            icon = Icons.Play("Start"),
            onClick = onStart,
            enabled = canStart,
        )
    }
    HorizontalDivider()
}

@Composable
private fun CycleStepper(count: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            icon = Icons.ChevronLeft("Decrease"),
            onClick = onDecrement,
            enabled = count > 1,
        )
        Text(
            text = "${count}x",
            typography = AppTheme.typography.Heading.H700,
            modifier = Modifier.padding(horizontal = Dimension.D300),
        )
        IconButton(
            icon = Icons.ChevronRight("Increase"),
            onClick = onIncrement,
            enabled = count < 99,
        )
    }
}

@Composable
private fun BlockRow(block: Block, onTap: () -> Unit) {
    ListItem(
        onClick = onTap,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(Dimension.D900)
                    .clip(CircleShape)
                    .background(Color(block.colorArgb)),
            )
        },
        headlineContent = {
            Text(text = block.name, typography = AppTheme.typography.Body.B600)
        },
        accessory = ListItemAccessory.Text(
            text = formatDuration(block.duration.inWholeSeconds.toInt()),
            color = AppTheme.colors.textSecondary,
        ),
        showDivider = true,
        contentPadding = PaddingValues(
            horizontal = Dimension.D700,
            vertical = Dimension.D400,
        ),
    )
}

@Composable
private fun AddBlockRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimension.D700, vertical = Dimension.D600),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.dangerfield.hiittimer.libraries.ui.components.icon.Icon(
            icon = Icons.Add("Add block"),
            color = AppTheme.colors.accentPrimary,
        )
        Spacer(modifier = Modifier.size(Dimension.D400))
        Text(
            text = "Add block",
            typography = AppTheme.typography.Body.B600,
            color = AppTheme.colors.accentPrimary,
        )
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
