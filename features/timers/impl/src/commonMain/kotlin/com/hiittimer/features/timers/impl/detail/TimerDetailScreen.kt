@file:Suppress("DEPRECATION")

package com.dangerfield.hiittimer.features.timers.impl.detail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.ListItem
import com.dangerfield.hiittimer.libraries.ui.components.ListItemAccessory
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonPrimary
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.system.color.ColorResource
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension

@Composable
fun TimerDetailScreen(
    viewModel: TimerDetailViewModel,
    onBack: () -> Unit,
    onStart: (String) -> Unit,
    onEdit: (String) -> Unit,
    onOpenDuplicate: (String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TimerDetailEvent.Start -> onStart(event.timerId)
                is TimerDetailEvent.Edit -> onEdit(event.timerId)
                is TimerDetailEvent.OpenDuplicate -> onOpenDuplicate(event.newTimerId)
                TimerDetailEvent.Close -> onBack()
            }
        }
    }

    val timer = state.timer

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(
                onBack = onBack,
                onEdit = { viewModel.takeAction(TimerDetailAction.Edit) },
                onDuplicate = { viewModel.takeAction(TimerDetailAction.Duplicate) },
                onDelete = { viewModel.takeAction(TimerDetailAction.Delete) },
                enabled = timer != null,
            )

            if (timer == null) return@Column

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                TitleBlock(timer)

                Spacer(modifier = Modifier.height(Dimension.D900))

                BlocksSection(blocks = timer.blocks)

                Spacer(modifier = Modifier.height(Dimension.D1500))
            }

            StartFooter(
                enabled = timer.blocks.isNotEmpty(),
                onStart = { viewModel.takeAction(TimerDetailAction.Start) },
            )
        }
    }
}

@Composable
private fun Header(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D400, vertical = Dimension.D400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(icon = Icons.ArrowBack("Back"), onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            icon = Icons.Copy("Duplicate"),
            onClick = onDuplicate,
            enabled = enabled,
        )
        IconButton(
            icon = Icons.Delete("Delete"),
            onClick = onDelete,
            enabled = enabled,
        )
        IconButton(
            icon = Icons.Pencil("Edit"),
            onClick = onEdit,
            enabled = enabled,
        )
    }
    HorizontalDivider()
}

@Composable
private fun TitleBlock(timer: Timer) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D700, vertical = Dimension.D900),
    ) {
        Text(
            text = timer.name.ifBlank { "Untitled" },
            typography = AppTheme.typography.Heading.H1000,
        )
        Spacer(modifier = Modifier.height(Dimension.D500))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D700)) {
            Stat(label = "Blocks", value = timer.blocks.size.toString())
            Stat(label = "Rounds", value = "${timer.cycleCount}x")
            Stat(label = "Total", value = formatDuration(timer.totalDuration.inWholeSeconds.toInt()))
        }
    }
    HorizontalDivider()
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            typography = AppTheme.typography.Label.L400,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(Dimension.D200))
        Text(text = value, typography = AppTheme.typography.Heading.H700)
    }
}

@Composable
private fun BlocksSection(blocks: List<Block>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "BLOCKS",
            typography = AppTheme.typography.Label.L400,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(
                start = Dimension.D700,
                end = Dimension.D700,
                bottom = Dimension.D400,
            ),
        )
        if (blocks.isEmpty()) {
            Text(
                text = "No blocks yet. Tap the pencil to add some.",
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = Dimension.D700),
            )
            return
        }
        blocks.forEachIndexed { index, block ->
            ListItem(
                leadingContent = { ColorDot(argb = block.colorArgb) },
                headlineContent = {
                    Text(text = block.name, typography = AppTheme.typography.Body.B600)
                },
                accessory = ListItemAccessory.Text(
                    text = formatDuration(block.duration.inWholeSeconds.toInt()),
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                ),
                showDivider = index != blocks.lastIndex,
                contentPadding = PaddingValues(
                    horizontal = Dimension.D700,
                    vertical = Dimension.D200,
                ),
            )
        }
    }
}

@Composable
private fun ColorDot(argb: Int) {
    Box(
        modifier = Modifier
            .size(Dimension.D900)
            .clip(CircleShape)
            .background(Color(argb)),
    )
}

@Composable
private fun StartFooter(enabled: Boolean, onStart: () -> Unit) {
    HorizontalDivider()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimension.D700,
                vertical = Dimension.D600,
            ),
    ) {
        ButtonPrimary(
            onClick = onStart,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Start workout")
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
