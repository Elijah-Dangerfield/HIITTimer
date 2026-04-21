@file:Suppress("DEPRECATION")
@file:OptIn(ExperimentalMaterial3Api::class)

package com.dangerfield.hiittimer.features.timers.impl.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.BlockStrip
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.system.color.defaultRunnerColors
import androidx.compose.ui.tooling.preview.Preview
import com.dangerfield.hiittimer.libraries.flowroutines.ObserveEvents
import kotlin.time.Duration.Companion.seconds
import com.dangerfield.hiittimer.libraries.ui.Elevation
import com.dangerfield.hiittimer.libraries.ui.bounceClick
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonDanger
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonGhost
import com.dangerfield.hiittimer.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.hiittimer.libraries.ui.components.icon.CircleIcon
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.elevation
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import com.dangerfield.hiittimer.system.Radii
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import rounds.libraries.resources.generated.resources.Res
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.action_delete
import rounds.libraries.resources.generated.resources.cd_create_timer
import rounds.libraries.resources.generated.resources.cd_delete
import rounds.libraries.resources.generated.resources.cd_new_timer
import rounds.libraries.resources.generated.resources.cd_rounds_logo
import rounds.libraries.resources.generated.resources.cd_settings
import rounds.libraries.resources.generated.resources.cd_start
import rounds.libraries.resources.generated.resources.common_cancel
import rounds.libraries.resources.generated.resources.common_untitled
import rounds.libraries.resources.generated.resources.logo_rounds_word
import rounds.libraries.resources.generated.resources.timer_list_delete_body
import rounds.libraries.resources.generated.resources.timer_list_delete_title
import rounds.libraries.resources.generated.resources.timer_list_empty_subtitle
import rounds.libraries.resources.generated.resources.timer_list_empty_title
import rounds.libraries.resources.generated.resources.timer_list_new
import rounds.libraries.resources.generated.resources.timer_list_subtitle_minutes
import rounds.libraries.resources.generated.resources.timer_list_subtitle_minutes_seconds
import rounds.libraries.resources.generated.resources.timer_list_subtitle_seconds
import rounds.libraries.resources.generated.resources.timer_list_subtitle_summary
import rounds.libraries.resources.generated.resources.timer_list_subtitle_summary_with_cycles
import rounds.libraries.resources.generated.resources.timer_list_this_timer

@Composable
fun TimerListScreen(
    viewModel: TimerListViewModel,
    onOpenDetail: (timerId: String, isNew: Boolean) -> Unit,
    onStart: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateNew: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    viewModel.ObserveEvents { event ->
        when (event) {
            is TimerListEvent.OpenDetail -> onOpenDetail(event.timerId, event.isNew)
            is TimerListEvent.StartRunner -> onStart(event.timerId)
            TimerListEvent.OpenSettings -> onOpenSettings()
            TimerListEvent.OpenPresets -> onCreateNew()
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

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 0
        }
    }

    Screen(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            CreateTimerFab(
                onClick = onCreate,
                expanded = isScrolled
            )

        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(onOpenSettings = onOpenSettings)

            if (state.timers.isEmpty() && !state.loading) {
                EmptyState(onCreate = onCreate)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = Dimension.D700,
                        end = Dimension.D700,
                        top = Dimension.D700,
                        bottom = Dimension.D1900,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimension.D500),
                ) {
                    items(state.timers, key = { it.id }) { timer ->
                        SwipeToDeleteTimerCard(
                            modifier = Modifier.animateItem(),
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
        val untitled = stringResource(AppRes.string.common_untitled)
        val fallbackName = stringResource(AppRes.string.timer_list_this_timer)
        DeleteTimerConfirmationDialog(
            timerName = pending?.name?.ifBlank { untitled } ?: fallbackName,
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
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRequestDelete()
            false
        },
    )
    Box(
        modifier = modifier
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
                        icon = Icons.Delete(stringResource(AppRes.string.cd_delete)),
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
    BasicDialog(
        onDismissRequest = onDismiss,
        topContent = {
            Text(
                text = stringResource(AppRes.string.timer_list_delete_title, timerName),
                typography = AppTheme.typography.Heading.H700,
            )
        },
        content = {
            Text(
                text = stringResource(AppRes.string.timer_list_delete_body),
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
            )
        },
        bottomContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
            ) {
                ButtonGhost(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppRes.string.common_cancel))
                }
                ButtonDanger(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(AppRes.string.action_delete))
                }
            }
        },
    )
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D700, vertical = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo_rounds_word),
            contentDescription = stringResource(AppRes.string.cd_rounds_logo),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(30.dp)
        )

        Spacer(Modifier.weight(1f))
        IconButton(icon = Icons.Settings(stringResource(AppRes.string.cd_settings)), onClick = onOpenSettings)
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
                text = stringResource(AppRes.string.timer_list_empty_title),
                typography = AppTheme.typography.Heading.H700,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimension.D500))
            Text(
                text = stringResource(AppRes.string.timer_list_empty_subtitle),
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimension.D1200))
            CircleIcon(
                icon = Icons.Add(stringResource(AppRes.string.cd_create_timer)),
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
private fun CreateTimerFab(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = AppTheme.colors.accentSecondary
    val content = AppTheme.colors.onAccentSecondary
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
        modifier = modifier
            .bounceClick(
                mutableInteractionSource = interactionSource,
                onClick = onClick,
            )
            .elevation(
                elevation = Elevation.Button,
                shape = Radii.Round.shape,
            )
            .clip(Radii.Round.shape)
            .background(background.color)
            .animateContentSize()
            .padding(
                horizontal = if (expanded) Dimension.D800 else Dimension.D500,
                vertical = Dimension.D500,
            ),
    ) {
        Icon(
            icon = Icons.Add(if (expanded) null else stringResource(AppRes.string.cd_new_timer)),
            size = IconSize.Large,
            color = content,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Text(
                text = stringResource(AppRes.string.timer_list_new),
                typography = AppTheme.typography.Label.L600.SemiBold,
                color = content,
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
                    text = timer.name.ifBlank { stringResource(AppRes.string.common_untitled) },
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
                icon = Icons.Play(stringResource(AppRes.string.cd_start)),
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
private fun Timer.subtitle(): String {
    val secs = totalDuration.inWholeSeconds.toInt()
    val mins = secs / 60
    val rem = secs % 60
    val durText = when {
        mins > 0 && rem > 0 -> stringResource(AppRes.string.timer_list_subtitle_minutes_seconds, mins, rem)
        mins > 0 -> stringResource(AppRes.string.timer_list_subtitle_minutes, mins)
        else -> stringResource(AppRes.string.timer_list_subtitle_seconds, secs)
    }
    return if (cycleCount > 1) {
        stringResource(AppRes.string.timer_list_subtitle_summary_with_cycles, blocks.size, durText, cycleCount)
    } else {
        stringResource(AppRes.string.timer_list_subtitle_summary, blocks.size, durText)
    }
}

private val sampleTimers: List<Timer> = listOf(
    Timer(
        id = "t1",
        name = "Tabata",
        cycleCount = 8,
        blocks = listOf(
            Block("w", "Warm up", 60.seconds, defaultRunnerColors.warmupArgb, BlockRole.Warmup),
            Block("c1", "Work", 20.seconds, defaultRunnerColors.defaultWorkArgb, BlockRole.Cycle),
            Block("c2", "Rest", 10.seconds, defaultRunnerColors.defaultRestArgb, BlockRole.Cycle),
        ),
    ),
    Timer(
        id = "t2",
        name = "EMOM 10",
        cycleCount = 10,
        blocks = listOf(
            Block("c", "Work", 60.seconds, defaultRunnerColors.defaultWorkArgb, BlockRole.Cycle),
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
