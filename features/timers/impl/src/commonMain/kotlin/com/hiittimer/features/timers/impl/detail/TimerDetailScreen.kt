@file:Suppress("DEPRECATION")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dangerfield.hiittimer.features.timers.impl.detail

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.impl.BlockStrip
import com.dangerfield.hiittimer.libraries.flowroutines.ObserveEvents
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.system.color.defaultRunnerColors
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.seconds
import com.dangerfield.hiittimer.libraries.ui.components.DropdownMenu
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonDanger
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonGhost
import com.dangerfield.hiittimer.libraries.ui.components.checkbox.Checkbox
import com.dangerfield.hiittimer.libraries.ui.components.dialog.BasicDialog
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.BasicTextField
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.system.color.ColorResource
import com.dangerfield.hiittimer.libraries.ui.fadingEdge
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.action_delete
import rounds.libraries.resources.generated.resources.action_dont_ask_again
import rounds.libraries.resources.generated.resources.cd_add
import rounds.libraries.resources.generated.resources.cd_back
import rounds.libraries.resources.generated.resources.cd_decrease_rounds
import rounds.libraries.resources.generated.resources.cd_delete
import rounds.libraries.resources.generated.resources.cd_drag_to_reorder
import rounds.libraries.resources.generated.resources.cd_duplicate
import rounds.libraries.resources.generated.resources.cd_increase_rounds
import rounds.libraries.resources.generated.resources.cd_more
import rounds.libraries.resources.generated.resources.cd_start_workout
import rounds.libraries.resources.generated.resources.common_cancel
import rounds.libraries.resources.generated.resources.common_untitled
import rounds.libraries.resources.generated.resources.timer_detail_add_block
import rounds.libraries.resources.generated.resources.timer_detail_add_cooldown
import rounds.libraries.resources.generated.resources.timer_detail_add_warmup
import rounds.libraries.resources.generated.resources.timer_detail_block_untitled
import rounds.libraries.resources.generated.resources.timer_detail_blocks_header
import rounds.libraries.resources.generated.resources.timer_detail_cooldown_header
import rounds.libraries.resources.generated.resources.timer_detail_delete_block_body
import rounds.libraries.resources.generated.resources.timer_detail_delete_block_title
import rounds.libraries.resources.generated.resources.timer_detail_menu_delete_timer
import rounds.libraries.resources.generated.resources.timer_detail_menu_duplicate
import rounds.libraries.resources.generated.resources.timer_detail_rounds
import rounds.libraries.resources.generated.resources.timer_detail_rounds_count
import rounds.libraries.resources.generated.resources.timer_detail_total_time
import rounds.libraries.resources.generated.resources.timer_detail_warmup_header
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val QUICK_ADJUST_SECONDS = 15

private sealed interface DetailItem {
    data object Hero : DetailItem
    data object Rounds : DetailItem
    data class SectionHeader(val title: String) : DetailItem
    data class BlockRow(val block: Block) : DetailItem
    data class AddRow(val role: BlockRole, val label: String) : DetailItem
}

@Composable
fun TimerDetailScreen(
    viewModel: TimerDetailViewModel,
    isNew: Boolean = false,
    onBack: () -> Unit,
    onStart: (String) -> Unit,
    onOpenBlock: (timerId: String, blockId: String) -> Unit,
    onOpenDuplicate: (String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    viewModel.ObserveEvents { event ->
        when (event) {
            is TimerDetailEvent.Start -> onStart(event.timerId)
            is TimerDetailEvent.OpenBlock -> onOpenBlock(event.timerId, event.blockId)
            is TimerDetailEvent.OpenDuplicate -> onOpenDuplicate(event.newTimerId)
            TimerDetailEvent.Close -> onBack()
        }
    }

    TimerDetailContent(
        state = state,
        isNew = isNew,
        onBack = onBack,
        onDuplicate = { viewModel.takeAction(TimerDetailAction.Duplicate) },
        onDelete = { viewModel.takeAction(TimerDetailAction.Delete) },
        onRename = { viewModel.takeAction(TimerDetailAction.RenameTimer(it)) },
        onCycleChange = { viewModel.takeAction(TimerDetailAction.ChangeCycleCount(it)) },
        onStart = { viewModel.takeAction(TimerDetailAction.Start) },
        onOpenBlock = { viewModel.takeAction(TimerDetailAction.OpenBlock(it)) },
        onAdjust = { id, delta -> viewModel.takeAction(TimerDetailAction.AdjustBlockDuration(id, delta)) },
        onRequestDeleteBlock = { viewModel.takeAction(TimerDetailAction.RequestDeleteBlock(it)) },
        onReorder = { viewModel.takeAction(TimerDetailAction.ReorderBlocks(it)) },
        onAddBlock = { role -> viewModel.takeAction(TimerDetailAction.AddBlock(role)) },
        onToggleDontAsk = { viewModel.takeAction(TimerDetailAction.ToggleDontAskAgain(it)) },
        onConfirmDeleteBlock = { viewModel.takeAction(TimerDetailAction.ConfirmDeleteBlock) },
        onDismissDeleteBlock = { viewModel.takeAction(TimerDetailAction.DismissDeleteBlock) },
    )
}

@Composable
private fun TimerDetailContent(
    state: TimerDetailState,
    isNew: Boolean,
    onBack: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onCycleChange: (Int) -> Unit,
    onStart: () -> Unit,
    onOpenBlock: (String) -> Unit,
    onAdjust: (String, Int) -> Unit,
    onRequestDeleteBlock: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onAddBlock: (BlockRole) -> Unit,
    onToggleDontAsk: (Boolean) -> Unit,
    onConfirmDeleteBlock: () -> Unit,
    onDismissDeleteBlock: () -> Unit,
    initialMenuOpen: Boolean = false,
) {
    val timer = state.timer
    val lazyListState = rememberLazyListState()

    // Track the name text's bottom edge vs the list's top edge in window coords.
    // Show the header title once the name has scrolled above the list viewport,
    // so it flips when the name itself is gone — not when the whole hero card is.
    var nameBottomInWindow by remember { mutableStateOf(Float.POSITIVE_INFINITY) }
    var listTopInWindow by remember { mutableStateOf(0f) }
    val titleInHeader by remember {
        derivedStateOf {
            val heroOnScreen = lazyListState.layoutInfo.visibleItemsInfo.any { it.index == 0 }
            !heroOnScreen || nameBottomInWindow <= listTopInWindow
        }
    }
    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val untitled = stringResource(AppRes.string.common_untitled)
            Header(
                onBack = onBack,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
                enabled = timer != null,
                title = timer?.name?.ifBlank { untitled }.orEmpty(),
                titleVisible = titleInHeader,
                initialMenuOpen = initialMenuOpen,
            )

            if (timer == null) return@Column

            DetailList(
                timer = timer,
                nameField = state.nameField,
                isNew = isNew,
                lazyListState = lazyListState,
                onRename = onRename,
                onCycleChange = onCycleChange,
                onStart = onStart,
                onOpenBlock = onOpenBlock,
                onAdjust = onAdjust,
                onRequestDeleteBlock = onRequestDeleteBlock,
                onReorder = onReorder,
                onAddBlock = onAddBlock,
                onNameBottomChanged = { nameBottomInWindow = it },
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { listTopInWindow = it.boundsInWindow().top },
            )
        }
    }

    if (state.pendingDeleteBlockId != null) {
        DeleteBlockConfirmationDialog(
            dontAskAgain = state.dontAskAgainChecked,
            onToggleDontAsk = onToggleDontAsk,
            onConfirm = onConfirmDeleteBlock,
            onDismiss = onDismissDeleteBlock,
        )
    }
}

@Composable
private fun Header(
    onBack: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    title: String,
    titleVisible: Boolean,
    initialMenuOpen: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(initialMenuOpen) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D400, vertical = Dimension.D400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(icon = Icons.ArrowBack(stringResource(AppRes.string.cd_back)), onClick = onBack)
        Box(modifier = Modifier.weight(1f).padding(horizontal = Dimension.D400)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Text(
                    text = title,
                    typography = AppTheme.typography.Heading.H900,
                    color = AppTheme.colors.text,
                    maxLines = 1,
                )
            }
        }
        Box {
            IconButton(
                icon = Icons.MoreVert(stringResource(AppRes.string.cd_more)),
                onClick = { menuOpen = true },
                enabled = enabled,
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier
                    .background(AppTheme.colors.surfacePrimary.color)
                    .padding(Dimension.D400),
            ) {
                MenuRow(
                    icon = Icons.Copy(stringResource(AppRes.string.cd_duplicate)),
                    label = stringResource(AppRes.string.timer_detail_menu_duplicate),
                    onClick = { menuOpen = false; onDuplicate() },
                )
                MenuRow(
                    icon = Icons.Delete(stringResource(AppRes.string.cd_delete)),
                    label = stringResource(AppRes.string.timer_detail_menu_delete_timer),
                    onClick = { menuOpen = false; onDelete() },
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: com.dangerfield.hiittimer.libraries.ui.components.icon.IconResource,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimension.D600, vertical = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon = icon,
            size = IconSize.Medium,
            color = if (destructive) AppTheme.colors.danger else AppTheme.colors.onSurfacePrimary,
        )
        Spacer(modifier = Modifier.size(Dimension.D500))
        Text(
            text = label,
            typography = AppTheme.typography.Body.B600,
            color = if (destructive) AppTheme.colors.danger else AppTheme.colors.onSurfacePrimary,
        )
    }
}

@Composable
private fun DetailList(
    timer: Timer,
    nameField: String,
    isNew: Boolean,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    onRename: (String) -> Unit,
    onCycleChange: (Int) -> Unit,
    onStart: () -> Unit,
    onOpenBlock: (String) -> Unit,
    onAdjust: (String, Int) -> Unit,
    onRequestDeleteBlock: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onAddBlock: (BlockRole) -> Unit,
    onNameBottomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val warmupLabel = stringResource(AppRes.string.timer_detail_warmup_header)
    val blocksLabel = stringResource(AppRes.string.timer_detail_blocks_header)
    val cooldownLabel = stringResource(AppRes.string.timer_detail_cooldown_header)
    val addWarmupLabel = stringResource(AppRes.string.timer_detail_add_warmup)
    val addBlockLabel = stringResource(AppRes.string.timer_detail_add_block)
    val addCooldownLabel = stringResource(AppRes.string.timer_detail_add_cooldown)
    val items: List<DetailItem> = remember(timer, warmupLabel, blocksLabel, cooldownLabel, addWarmupLabel, addBlockLabel, addCooldownLabel) {
        buildDetailItems(timer, warmupLabel, blocksLabel, cooldownLabel, addWarmupLabel, addBlockLabel, addCooldownLabel)
    }
    val keyFor: (Int) -> Any = { idx -> keyForItem(items[idx], idx) }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromItem = items.getOrNull(from.index) as? DetailItem.BlockRow ?: return@rememberReorderableLazyListState
        val toItem = items.getOrNull(to.index) as? DetailItem.BlockRow ?: return@rememberReorderableLazyListState
        if (fromItem.block.role != toItem.block.role) return@rememberReorderableLazyListState

        val ids = timer.blocks.map { it.id }.toMutableList()
        val fromGlobal = ids.indexOf(fromItem.block.id)
        val toGlobal = ids.indexOf(toItem.block.id)
        if (fromGlobal < 0 || toGlobal < 0) return@rememberReorderableLazyListState
        val moved = ids.removeAt(fromGlobal)
        ids.add(toGlobal, moved)
        onReorder(ids)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .fadingEdge(lazyListState),
        state = lazyListState,
        contentPadding = PaddingValues(
            start = Dimension.D700,
            end = Dimension.D700,
            top = Dimension.D400,
            bottom = Dimension.D1500,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        items.forEachIndexed { index, item ->
            when (item) {
                DetailItem.Hero -> item(key = "hero") {
                    Hero(
                        timer = timer,
                        nameField = nameField,
                        isNew = isNew,
                        onRename = onRename,
                        onStart = onStart,
                        onNameBottomChanged = onNameBottomChanged,
                    )
                }
                DetailItem.Rounds -> item(key = "rounds") {
                    RoundsStepper(
                        count = timer.cycleCount,
                        onDecrement = { onCycleChange(timer.cycleCount - 1) },
                        onIncrement = { onCycleChange(timer.cycleCount + 1) },
                    )
                }
                is DetailItem.SectionHeader -> item(key = "hdr-${item.title}") {
                    Text(
                        text = item.title,
                        typography = AppTheme.typography.Label.L600,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = Dimension.D400, bottom = Dimension.D100),
                    )
                }
                is DetailItem.BlockRow -> item(key = "block-${item.block.id}") {
                    val siblingCount = when (item.block.role) {
                        BlockRole.Warmup -> timer.warmupBlocks.size
                        BlockRole.Cycle -> timer.cycleBlocks.size
                        BlockRole.Cooldown -> timer.cooldownBlocks.size
                    }
                    val reorderable = siblingCount > 1
                    ReorderableItem(state = reorderableState, key = "block-${item.block.id}") { dragging ->
                        BlockCard(
                            block = item.block,
                            dragging = dragging,
                            reorderable = reorderable,
                            onTap = { onOpenBlock(item.block.id) },
                            onAdjust = { delta -> onAdjust(item.block.id, delta) },
                            onRequestDelete = { onRequestDeleteBlock(item.block.id) },
                            dragHandleModifier = if (reorderable) Modifier.longPressReorderable(this) else Modifier,
                        )
                    }
                }
                is DetailItem.AddRow -> item(key = "add-${item.role.name}") {
                    AddBlockRow(label = item.label, onClick = { onAddBlock(item.role) })
                }
            }
        }
    }
}

private fun buildDetailItems(
    timer: Timer,
    warmupHeader: String,
    blocksHeader: String,
    cooldownHeader: String,
    addWarmup: String,
    addBlock: String,
    addCooldown: String,
): List<DetailItem> = buildList {
    add(DetailItem.Hero)

    add(DetailItem.SectionHeader(warmupHeader))
    timer.warmupBlocks.forEach { add(DetailItem.BlockRow(it)) }
    add(DetailItem.AddRow(BlockRole.Warmup, addWarmup))

    add(DetailItem.Rounds)
    add(DetailItem.SectionHeader(blocksHeader))
    timer.cycleBlocks.forEach { add(DetailItem.BlockRow(it)) }
    add(DetailItem.AddRow(BlockRole.Cycle, addBlock))

    add(DetailItem.SectionHeader(cooldownHeader))
    timer.cooldownBlocks.forEach { add(DetailItem.BlockRow(it)) }
    add(DetailItem.AddRow(BlockRole.Cooldown, addCooldown))
}

private fun keyForItem(item: DetailItem, index: Int): Any = when (item) {
    DetailItem.Hero -> "hero"
    DetailItem.Rounds -> "rounds"
    is DetailItem.SectionHeader -> "hdr-${item.title}"
    is DetailItem.BlockRow -> "block-${item.block.id}"
    is DetailItem.AddRow -> "add-${item.role.name}"
}

@Composable
private fun Hero(
    timer: Timer,
    nameField: String,
    isNew: Boolean,
    onRename: (String) -> Unit,
    onStart: () -> Unit,
    onNameBottomChanged: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppTheme.colors.surfacePrimary.color)
            .padding(horizontal = Dimension.D900, vertical = Dimension.D1000),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onNameBottomChanged(it.boundsInWindow().bottom) },
        ) {
            InlineTitleField(
                value = nameField,
                onValueChange = onRename,
                placeholder = stringResource(AppRes.string.common_untitled),
                requestFocus = isNew,
            )
        }

        Spacer(modifier = Modifier.height(Dimension.D700))

        Text(
            text = stringResource(AppRes.string.timer_detail_total_time),
            typography = AppTheme.typography.Label.L400,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(Dimension.D200))
        Text(
            text = formatClock(timer.totalDuration.inWholeSeconds.toInt()),
            typography = AppTheme.typography.Display.D1500,
            color = AppTheme.colors.onSurfacePrimary,
        )

        if (timer.blocks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimension.D500))
            BlockStrip(blocks = timer.orderedBlocks)
        }

        Spacer(modifier = Modifier.height(Dimension.D900))

        PlayButton(
            enabled = timer.cycleBlocks.isNotEmpty() || timer.warmupBlocks.isNotEmpty() || timer.cooldownBlocks.isNotEmpty(),
            onClick = onStart,
        )
    }
}

@Composable
private fun PlayButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) AppTheme.colors.accentPrimary else AppTheme.colors.surfaceDisabled
    val fg = if (enabled) AppTheme.colors.onAccentPrimary else AppTheme.colors.textDisabled
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(bg.color)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = Icons.Play(stringResource(AppRes.string.cd_start_workout)),
            size = IconSize.Largest,
            color = fg,
        )
    }
}

@Composable
private fun InlineTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    requestFocus: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var hasAutoFocused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(requestFocus) {
        if (requestFocus && !hasAutoFocused) {
            hasAutoFocused = true
            focusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                typography = AppTheme.typography.Heading.H800,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = true,
            typographyToken = AppTheme.typography.Heading.H900,
            color = AppTheme.colors.onSurfacePrimary.color,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
            ),
        )
    }
}

@Composable
private fun RoundsStepper(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimension.D500),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(AppRes.string.timer_detail_rounds),
            typography = AppTheme.typography.Label.L400,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(Dimension.D300))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimension.D700),
        ) {
            StepperCircle(
                icon = Icons.ChevronLeft(stringResource(AppRes.string.cd_decrease_rounds)),
                onClick = onDecrement,
                enabled = count > 1,
            )
            Text(
                text = stringResource(AppRes.string.timer_detail_rounds_count, count),
                typography = AppTheme.typography.Display.D1100,
                color = AppTheme.colors.text,
            )
            StepperCircle(
                icon = Icons.ChevronRight(stringResource(AppRes.string.cd_increase_rounds)),
                onClick = onIncrement,
                enabled = count < 99,
            )
        }
    }
}

@Composable
private fun StepperCircle(
    icon: com.dangerfield.hiittimer.libraries.ui.components.icon.IconResource,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.surfacePrimary.color)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = icon,
            size = IconSize.Small,
            color = if (enabled) AppTheme.colors.onSurfacePrimary else AppTheme.colors.onSurfaceDisabled,
        )
    }
}


@Composable
private fun BlockCard(
    block: Block,
    dragging: Boolean,
    reorderable: Boolean,
    onTap: () -> Unit,
    onAdjust: (Int) -> Unit,
    onRequestDelete: () -> Unit,
    dragHandleModifier: Modifier,
) {
    val color = Color(block.colorArgb).copy(alpha = 1f)
    val cardBg = lerp(
        color,
        Color.White,
        if (dragging) 0.15f else 0.28f,
    ).copy(alpha = 1f)

    val onCardColor = defaultRunnerColors.onColorFor(cardBg)
    val onCardResource = ColorResource.FromColor(onCardColor, "on-block-card")

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.background.color)
                .background(cardBg)
                .clickable(enabled = !dragging) { onTap() }
                .padding(horizontal = Dimension.D700, vertical = Dimension.D700),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = block.name.ifBlank { stringResource(AppRes.string.timer_detail_block_untitled) },
                    typography = AppTheme.typography.Heading.H800,
                    color = onCardResource,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (reorderable) {
                    Box(modifier = dragHandleModifier) {
                        Icon(
                            icon = Icons.DragHandle(stringResource(AppRes.string.cd_drag_to_reorder)),
                            size = IconSize.Small,
                            color = ColorResource.FromColor(onCardColor.copy(alpha = 0.6f), "drag-handle"),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimension.D400))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuickAdjustButton(
                    label = "−",
                    onClick = { onAdjust(-QUICK_ADJUST_SECONDS) },
                    enabled = block.duration.inWholeSeconds.toInt() > QUICK_ADJUST_SECONDS,
                )
                Text(
                    text = formatClock(block.duration.inWholeSeconds.toInt()),
                    typography = AppTheme.typography.Display.D1100,
                    color = onCardResource,
                )
                QuickAdjustButton(
                    label = "+",
                    onClick = { onAdjust(QUICK_ADJUST_SECONDS) },
                )
            }
        }
    }

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
                        icon = Icons.Delete(stringResource(AppRes.string.cd_delete)),
                        size = IconSize.Medium,
                        color = AppTheme.colors.onAccentPrimary,
                    )
                }
            },
        ) {
            content()
        }
    }
}

@Composable
private fun QuickAdjustButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val bg = if (enabled) AppTheme.colors.surfacePrimary else AppTheme.colors.surfaceDisabled
    val fg = if (enabled) AppTheme.colors.onSurfacePrimary else AppTheme.colors.textDisabled
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg.color)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            typography = AppTheme.typography.Heading.H700,
            color = fg,
        )
    }
}

@Composable
private fun AddBlockRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.surfacePrimary.color)
            .clickable { onClick() }
            .padding(horizontal = Dimension.D700, vertical = Dimension.D700),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon = Icons.Add(stringResource(AppRes.string.cd_add)),
            size = IconSize.Small,
            color = AppTheme.colors.accentPrimary,
        )
        Spacer(modifier = Modifier.size(Dimension.D400))
        Text(
            text = label,
            typography = AppTheme.typography.Body.B700.Bold,
            color = AppTheme.colors.accentPrimary,
        )
    }
}

@Composable
private fun DeleteBlockConfirmationDialog(
    dontAskAgain: Boolean,
    onToggleDontAsk: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        onDismissRequest = onDismiss,
        topContent = {
            Text(
                text = stringResource(AppRes.string.timer_detail_delete_block_title),
                typography = AppTheme.typography.Heading.H700,
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(AppRes.string.timer_detail_delete_block_body),
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(Dimension.D500))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDontAsk(!dontAskAgain) }
                        .padding(vertical = Dimension.D300),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = dontAskAgain, onCheckedChange = { onToggleDontAsk(it) })
                    Spacer(modifier = Modifier.size(Dimension.D400))
                    Text(
                        text = stringResource(AppRes.string.action_dont_ask_again),
                        typography = AppTheme.typography.Body.B500,
                    )
                }
            }
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

private fun formatClock(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

@Composable
private fun Modifier.longPressReorderable(
    scope: sh.calvin.reorderable.ReorderableCollectionItemScope,
): Modifier = with(scope) {
    this@longPressReorderable.draggableHandle()
}

private val sampleTimer: Timer = Timer(
    id = "sample",
    name = "Tabata",
    cycleCount = 8,
    blocks = listOf(
        Block("w1", "Warm up", 60.seconds, defaultRunnerColors.warmupArgb, BlockRole.Warmup),
        Block("c1", "Work", 30.seconds, defaultRunnerColors.defaultWorkArgb, BlockRole.Cycle),
        Block("c2", "Rest", 15.seconds, defaultRunnerColors.defaultRestArgb, BlockRole.Cycle),
        Block("d1", "Cool down", 90.seconds, defaultRunnerColors.lowIntensityArgb, BlockRole.Cooldown),
    ),
)

@Composable
@Preview
private fun TimerDetailContentPreview() {
    PreviewContent {
        TimerDetailContent(
            state = TimerDetailState(timer = sampleTimer, nameField = sampleTimer.name, loading = false),
            isNew = false,
            onBack = {}, onDuplicate = {}, onDelete = {},
            onRename = {}, onCycleChange = {}, onStart = {},
            onOpenBlock = {}, onAdjust = { _, _ -> }, onRequestDeleteBlock = {},
            onReorder = {}, onAddBlock = {},
            onToggleDontAsk = {}, onConfirmDeleteBlock = {}, onDismissDeleteBlock = {},
        )
    }
}

@Composable
@Preview
private fun TimerDetailContentMenuOpenPreview() {
    PreviewContent {
        TimerDetailContent(
            state = TimerDetailState(timer = sampleTimer, nameField = sampleTimer.name, loading = false),
            isNew = false,
            onBack = {}, onDuplicate = {}, onDelete = {},
            onRename = {}, onCycleChange = {}, onStart = {},
            onOpenBlock = {}, onAdjust = { _, _ -> }, onRequestDeleteBlock = {},
            onReorder = {}, onAddBlock = {},
            onToggleDontAsk = {}, onConfirmDeleteBlock = {}, onDismissDeleteBlock = {},
            initialMenuOpen = true,
        )
    }
}

@Composable
@Preview
private fun TimerDetailContentEmptyPreview() {
    val empty = sampleTimer.copy(name = "", blocks = emptyList())
    PreviewContent {
        TimerDetailContent(
            state = TimerDetailState(timer = empty, nameField = "", loading = false),
            isNew = true,
            onBack = {}, onDuplicate = {}, onDelete = {},
            onRename = {}, onCycleChange = {}, onStart = {},
            onOpenBlock = {}, onAdjust = { _, _ -> }, onRequestDeleteBlock = {},
            onReorder = {}, onAddBlock = {},
            onToggleDontAsk = {}, onConfirmDeleteBlock = {}, onDismissDeleteBlock = {},
        )
    }
}

@Composable
@Preview
private fun TimerDetailContentDeletePromptPreview() {
    PreviewContent {
        TimerDetailContent(
            state = TimerDetailState(
                timer = sampleTimer,
                nameField = sampleTimer.name,
                loading = false,
                pendingDeleteBlockId = "c1",
            ),
            isNew = false,
            onBack = {}, onDuplicate = {}, onDelete = {},
            onRename = {}, onCycleChange = {}, onStart = {},
            onOpenBlock = {}, onAdjust = { _, _ -> }, onRequestDeleteBlock = {},
            onReorder = {}, onAddBlock = {},
            onToggleDontAsk = {}, onConfirmDeleteBlock = {}, onDismissDeleteBlock = {},
        )
    }
}

@Composable
@Preview
private fun BlockCardSwatchesPreview() {
    PreviewContent {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimension.D500),
            verticalArrangement = Arrangement.spacedBy(Dimension.D400),
        ) {
            defaultRunnerColors.swatches.take(6).forEachIndexed { i, color ->
                BlockCard(
                    block = Block("p$i", "Block ${i + 1}", 45.seconds, color.hashCode()),
                    dragging = false,
                    reorderable = true,
                    onTap = {},
                    onAdjust = {},
                    onRequestDelete = {},
                    dragHandleModifier = Modifier,
                )
            }
        }
    }
}
