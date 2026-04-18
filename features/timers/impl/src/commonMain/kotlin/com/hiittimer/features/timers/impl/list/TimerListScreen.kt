@file:Suppress("DEPRECATION")

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.ListItem
import com.dangerfield.hiittimer.libraries.ui.components.ListItemAccessory
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
    onOpenDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TimerListEvent.OpenDetail -> onOpenDetail(event.timerId)
                TimerListEvent.OpenSettings -> onOpenSettings()
            }
        }
    }

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(
                onOpenSettings = { viewModel.takeAction(TimerListAction.OpenSettings) },
                onCreate = { viewModel.takeAction(TimerListAction.CreateNew) },
            )

            if (state.timers.isEmpty() && !state.loading) {
                EmptyState(onCreate = { viewModel.takeAction(TimerListAction.CreateNew) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = Dimension.D500,
                        bottom = Dimension.D1800,
                    ),
                ) {
                    itemsIndexed(
                        items = state.timers,
                        key = { _, item -> item.id },
                    ) { index, timer ->
                        TimerRow(
                            timer = timer,
                            onTap = { viewModel.takeAction(TimerListAction.Open(timer.id)) },
                            showDivider = index != state.timers.lastIndex,
                        )
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
            typography = AppTheme.typography.Heading.H900,
            modifier = Modifier.weight(1f),
        )
        IconButton(icon = Icons.Settings("Settings"), onClick = onOpenSettings)
        IconButton(icon = Icons.Add("New timer"), onClick = onCreate)
    }
    HorizontalDivider()
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
private fun TimerRow(timer: Timer, onTap: () -> Unit, showDivider: Boolean) {
    ListItem(
        onClick = onTap,
        leadingContent = { BlockDotCluster(timer.blocks) },
        headlineContent = {
            Text(
                text = timer.name.ifBlank { "Untitled" },
                typography = AppTheme.typography.Body.B700.SemiBold,
                maxLines = 1,
            )
        },
        supportingContent = {
            Text(
                text = timer.subtitle(),
                typography = AppTheme.typography.Body.B400,
            )
        },
        accessory = ListItemAccessory.Chevron,
        showDivider = showDivider,
        contentPadding = PaddingValues(
            horizontal = Dimension.D700,
            vertical = Dimension.D500,
        ),
    )
}

@Composable
private fun BlockDotCluster(blocks: List<Block>) {
    if (blocks.isEmpty()) {
        Box(
            modifier = Modifier
                .size(Dimension.D1000)
                .clip(CircleShape)
                .background(AppTheme.colors.surfaceSecondary.color),
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        blocks.take(3).forEach { block ->
            Box(
                modifier = Modifier
                    .size(Dimension.D900)
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
