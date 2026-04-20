@file:Suppress("DEPRECATION")

package com.dangerfield.hiittimer.features.timers.impl.blockedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.libraries.flowroutines.ObserveEvents
import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.BlockRole
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.seconds
import com.dangerfield.hiittimer.libraries.ui.components.checkbox.Checkbox
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.Surface
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonDanger
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonGhost
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonPrimary
import com.dangerfield.hiittimer.libraries.ui.components.dialog.Dialog
import com.dangerfield.hiittimer.libraries.ui.components.dialog.rememberDialogState
import com.dangerfield.hiittimer.libraries.ui.components.dialog.bottomsheet.BottomSheet
import com.dangerfield.hiittimer.libraries.ui.components.dialog.bottomsheet.rememberBottomSheetState
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
fun BlockEditScreen(
    viewModel: BlockEditViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    viewModel.ObserveEvents { event ->
        if (event is BlockEditEvent.Close) onBack()
    }

    BlockEditContent(
        state = state,
        onBack = onBack,
        onOpenColor = { viewModel.takeAction(BlockEditAction.OpenColorPicker) },
        onDismissColor = { viewModel.takeAction(BlockEditAction.DismissColorPicker) },
        onSelectColor = { viewModel.takeAction(BlockEditAction.SetColor(it)) },
        onOpenRename = { viewModel.takeAction(BlockEditAction.OpenRename) },
        onCommitRename = { viewModel.takeAction(BlockEditAction.CommitRename(it)) },
        onDismissRename = { viewModel.takeAction(BlockEditAction.DismissRename) },
        onRequestDelete = { viewModel.takeAction(BlockEditAction.RequestDelete) },
        onToggleDontAsk = { viewModel.takeAction(BlockEditAction.ToggleDontAskAgain(it)) },
        onConfirmDelete = { viewModel.takeAction(BlockEditAction.ConfirmDelete) },
        onDismissDelete = { viewModel.takeAction(BlockEditAction.DismissDelete) },
        onDurationChange = { viewModel.takeAction(BlockEditAction.SetDurationSeconds(it)) },
    )
}

@Composable
private fun BlockEditContent(
    state: BlockEditState,
    onBack: () -> Unit,
    onOpenColor: () -> Unit,
    onDismissColor: () -> Unit,
    onSelectColor: (Int) -> Unit,
    onOpenRename: () -> Unit,
    onCommitRename: (String) -> Unit,
    onDismissRename: () -> Unit,
    onRequestDelete: () -> Unit,
    onToggleDontAsk: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onDurationChange: (Int) -> Unit,
) {
    val block = state.block ?: return
    val blockColor = remember(block.colorArgb) { Color(block.colorArgb) }
    val onBlockResource = remember(block.colorArgb) {
        ColorResource.FromColor(ColorPalette.onColorFor(block.colorArgb), "on-block")
    }

    Screen(
        modifier = Modifier.fillMaxSize(),
        containerColor = blockColor,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TopBar(
                onBack = onBack,
                onColor = onBlockResource,
                blockColor = blockColor,
                currentColorArgb = block.colorArgb,
                onOpenColor = onOpenColor,
                onDelete = onRequestDelete,
            )

            Spacer(modifier = Modifier.height(Dimension.D700))

            NameRow(
                name = block.name,
                onColor = onBlockResource,
                onEdit = onOpenRename,
            )

            Spacer(modifier = Modifier.height(Dimension.D400))

            Text(
                text = formatDuration(block.duration.inWholeSeconds.toInt()),
                typography = AppTheme.typography.Display.D1500,
                color = onBlockResource,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimension.D900),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Dimension.D700))

            DurationWheels(
                totalSeconds = block.duration.inWholeSeconds.toInt(),
                onChange = onDurationChange,
                onColor = onBlockResource,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }

    if (state.colorPickerOpen) {
        ColorPickerSheet(
            selectedArgb = block.colorArgb,
            onSelect = onSelectColor,
            onDismiss = onDismissColor,
        )
    }

    if (state.renameOpen) {
        RenameDialog(
            initial = block.name,
            onConfirm = onCommitRename,
            onDismiss = onDismissRename,
        )
    }

    if (state.deleteConfirmationOpen) {
        DeleteConfirmationDialog(
            dontAskAgain = state.dontAskAgainChecked,
            onToggleDontAsk = onToggleDontAsk,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onColor: ColorResource,
    blockColor: Color,
    currentColorArgb: Int,
    onOpenColor: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D400, vertical = Dimension.D400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(
            icon = Icons.ArrowBack("Back"),
            iconSize = IconSize.Medium,
            padding = Dimension.D300,
            backgroundColor = onColor.withAlpha(0.14f),
            contentColor = onColor,
            onClick = onBack,
        )
        Spacer(modifier = Modifier.weight(1f))
        ColorChipButton(
            color = blockColor,
            onColor = onColor,
            onClick = onOpenColor,
        )
        Spacer(modifier = Modifier.size(Dimension.D300))
        CircleIcon(
            icon = Icons.Delete("Delete block"),
            iconSize = IconSize.Medium,
            padding = Dimension.D300,
            backgroundColor = onColor.withAlpha(0.14f),
            contentColor = onColor,
            onClick = onDelete,
        )
    }
}

@Composable
private fun ColorChipButton(color: Color, onColor: ColorResource, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(onColor.color.copy(alpha = 0.14f))
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
                .border(2.dp, onColor.color.copy(alpha = 0.8f), CircleShape),
        )
    }
}

@Composable
private fun NameRow(name: String, onColor: ColorResource, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D900)
            .clickable { onEdit() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = name.ifBlank { "Untitled" },
            typography = AppTheme.typography.Heading.H800,
            color = onColor,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.size(Dimension.D400))
        com.dangerfield.hiittimer.libraries.ui.components.icon.Icon(
            icon = Icons.Pencil("Edit name"),
            size = IconSize.Small,
            color = onColor.withAlpha(0.75f),
        )
    }
}

@Composable
private fun DurationWheels(
    totalSeconds: Int,
    onChange: (Int) -> Unit,
    onColor: ColorResource,
    modifier: Modifier = Modifier,
) {
    val currentMinutes = totalSeconds / 60
    val currentSecs = totalSeconds % 60

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        WheelColumn(
            label = "MIN",
            range = 0..59,
            value = currentMinutes,
            onValueChange = { minutes ->
                val newTotal = minutes * 60 + currentSecs
                onChange(newTotal.coerceAtLeast(1))
            },
            onColor = onColor,
            modifier = Modifier.weight(1f),
        )
        WheelColumn(
            label = "SEC",
            range = 0..59,
            value = currentSecs,
            onValueChange = { secs ->
                val newTotal = currentMinutes * 60 + secs
                onChange(newTotal.coerceAtLeast(1))
            },
            onColor = onColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WheelColumn(
    label: String,
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    onColor: ColorResource,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 56.dp
    val values = remember(range) { range.toList() }
    val initialIndex = remember(range) { (value - range.first).coerceIn(0, values.lastIndex) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)

    // Keep the effect's closure in sync with the latest composition. Without this
    // the LaunchedEffect below captures the first-composition `value`/`onValueChange`
    // forever (its key never changes), so scrolling one wheel would compute the
    // other axis from stale initial state and reset it.
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(state, values) {
        var wasScrolling = false
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (wasScrolling && !scrolling) {
                val layoutInfo = state.layoutInfo
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val centerItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                if (centerItem != null) {
                    val newValue = values.getOrNull(centerItem.index)
                    if (newValue != null && newValue != currentValue) {
                        currentOnValueChange(newValue)
                    }
                }
            }
            wasScrolling = scrolling
        }
    }

    LaunchedEffect(value) {
        val desiredIndex = (value - range.first).coerceIn(0, values.lastIndex)
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != desiredIndex) {
            state.animateScrollToItem(desiredIndex)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            typography = AppTheme.typography.Label.L400,
            color = onColor.withAlpha(0.7f),
        )
        Spacer(modifier = Modifier.height(Dimension.D300))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val padding = (maxHeight - itemHeight) / 2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(14.dp))
                    .background(onColor.color.copy(alpha = 0.14f)),
            )
            LazyColumn(
                state = state,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = padding),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = values, key = { it }) { item ->
                    val isCenter = item == value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.toString().padStart(2, '0'),
                            typography = if (isCenter) {
                                AppTheme.typography.Display.D1000
                            } else {
                                AppTheme.typography.Heading.H700
                            },
                            color = if (isCenter) onColor else onColor.withAlpha(0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerSheet(
    selectedArgb: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState()
    BottomSheet(
        state = sheetState,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimension.D700),
        ) {
            Text(
                text = "Color",
                typography = AppTheme.typography.Heading.H700,
            )
            Spacer(modifier = Modifier.height(Dimension.D600))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(ColorPalette.swatches) { swatch ->
                    val argb = swatch.toInt()
                    val selected = selectedArgb == argb
                    Box(
                        modifier = Modifier
                            .size(if (selected) 52.dp else 44.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = AppTheme.colors.text.color,
                                shape = CircleShape,
                            )
                            .clickable {
                                onSelect(argb)
                                onDismiss()
                            },
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimension.D900))
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogState = rememberDialogState()
    var draft by remember { mutableStateOf(initial) }
    Dialog(state = dialogState, onDismissRequest = onDismiss) {
        Surface(
            color = AppTheme.colors.surfacePrimary,
            contentColor = AppTheme.colors.onSurfacePrimary,
            radius = com.dangerfield.hiittimer.system.Radii.Card,
            contentPadding = PaddingValues(Dimension.D700),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Rename block",
                    typography = AppTheme.typography.Heading.H700,
                )
                Spacer(modifier = Modifier.height(Dimension.D500))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Words,
                    ),
                )
                Spacer(modifier = Modifier.height(Dimension.D700))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
                ) {
                    ButtonGhost(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    ButtonPrimary(
                        onClick = { onConfirm(draft.trim()) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    dontAskAgain: Boolean,
    onToggleDontAsk: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogState = rememberDialogState()
    Dialog(state = dialogState, onDismissRequest = onDismiss) {
        Surface(
            color = AppTheme.colors.surfacePrimary,
            contentColor = AppTheme.colors.onSurfacePrimary,
            radius = com.dangerfield.hiittimer.system.Radii.Card,
            contentPadding = PaddingValues(Dimension.D700),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Delete this block?",
                    typography = AppTheme.typography.Heading.H700,
                )
                Spacer(modifier = Modifier.height(Dimension.D400))
                Text(
                    text = "You can't undo this.",
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(Dimension.D700))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDontAsk(!dontAskAgain) }
                        .padding(vertical = Dimension.D300),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { onToggleDontAsk(it) },
                    )
                    Spacer(modifier = Modifier.size(Dimension.D400))
                    Text(
                        text = "Don't ask again",
                        typography = AppTheme.typography.Body.B500,
                    )
                }
                Spacer(modifier = Modifier.height(Dimension.D500))
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

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private val sampleBlock: Block = Block(
    id = "sample",
    name = "Work",
    duration = 45.seconds,
    colorArgb = ColorPalette.defaultWorkArgb,
    role = BlockRole.Cycle,
)

@Composable
@Preview
private fun BlockEditContentPreview() {
    PreviewContent(backgroundColor = null) {
        BlockEditContent(
            state = BlockEditState(block = sampleBlock),
            onBack = {}, onOpenColor = {}, onDismissColor = {}, onSelectColor = {},
            onOpenRename = {}, onCommitRename = {}, onDismissRename = {},
            onRequestDelete = {}, onToggleDontAsk = {}, onConfirmDelete = {}, onDismissDelete = {},
            onDurationChange = {},
        )
    }
}

@Composable
@Preview
private fun BlockEditContentLightBlockPreview() {
    PreviewContent(backgroundColor = null) {
        BlockEditContent(
            state = BlockEditState(
                block = sampleBlock.copy(
                    name = "Warm up",
                    colorArgb = ColorPalette.warmupArgb,
                    duration = 90.seconds,
                ),
            ),
            onBack = {}, onOpenColor = {}, onDismissColor = {}, onSelectColor = {},
            onOpenRename = {}, onCommitRename = {}, onDismissRename = {},
            onRequestDelete = {}, onToggleDontAsk = {}, onConfirmDelete = {}, onDismissDelete = {},
            onDurationChange = {},
        )
    }
}
