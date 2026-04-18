@file:Suppress("DEPRECATION")

package com.dangerfield.hiittimer.features.timers.impl.blockedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.impl.ColorPalette
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonDanger
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

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is BlockEditEvent.Close) onBack()
        }
    }

    val block = state.block

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(onBack = onBack)

            if (block == null) return@Column

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimension.D700),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(Dimension.D900))

                OutlinedTextField(
                    value = state.nameField,
                    onValueChange = { viewModel.takeAction(BlockEditAction.Rename(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Words,
                    ),
                    typographyToken = AppTheme.typography.Heading.H600,
                )

                Spacer(modifier = Modifier.height(Dimension.D1500))

                DurationDisplay(
                    seconds = block.duration.inWholeSeconds.toInt(),
                    color = Color(block.colorArgb),
                )

                Spacer(modifier = Modifier.height(Dimension.D900))

                AdjustRow(
                    onAdjust = { viewModel.takeAction(BlockEditAction.AdjustSeconds(it)) },
                )

                Spacer(modifier = Modifier.height(Dimension.D1300))

                Text(
                    text = "COLOR",
                    typography = AppTheme.typography.Label.L400,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(Dimension.D500))
                ColorStrip(
                    selectedArgb = block.colorArgb,
                    onSelect = { viewModel.takeAction(BlockEditAction.SetColor(it)) },
                )

                Spacer(modifier = Modifier.weight(1f))

                ButtonDanger(
                    onClick = { viewModel.takeAction(BlockEditAction.Delete) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete block") }

                Spacer(modifier = Modifier.height(Dimension.D900))
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D500, vertical = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(icon = Icons.ArrowBack("Back"), onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Edit Block",
            typography = AppTheme.typography.Label.L500,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(Dimension.D1200))
    }
    HorizontalDivider()
}

@Composable
private fun DurationDisplay(seconds: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatDuration(seconds),
            typography = AppTheme.typography.Display.D1500,
            color = ColorResource.FromColor(ColorPalette.onColorFor(color.toArgb()), "duration"),
        )
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

@Composable
private fun AdjustRow(onAdjust: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        AdjustTile(label = "−10s", modifier = Modifier.weight(1f), onClick = { onAdjust(-10) })
        AdjustTile(label = "−1s", modifier = Modifier.weight(1f), onClick = { onAdjust(-1) })
        AdjustTile(label = "+1s", modifier = Modifier.weight(1f), onClick = { onAdjust(1) })
        AdjustTile(label = "+10s", modifier = Modifier.weight(1f), onClick = { onAdjust(10) })
    }
}

@Composable
private fun AdjustTile(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(AppTheme.colors.surfaceSecondary.color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            typography = AppTheme.typography.Label.L600,
            color = AppTheme.colors.onSurfaceSecondary,
        )
    }
}

@Composable
private fun ColorStrip(selectedArgb: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(ColorPalette.swatches) { swatch ->
            val argb = swatch.toInt()
            val selected = selectedArgb == argb
            Box(
                modifier = Modifier
                    .size(if (selected) 48.dp else 40.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (selected) 3.dp else 0.dp,
                        color = AppTheme.colors.text.color,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(argb) },
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}
