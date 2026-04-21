package com.dangerfield.hiittimer.features.timers.impl.preset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.libraries.flowroutines.ObserveEvents
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.libraries.ui.bounceClick
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.cd_back
import rounds.libraries.resources.generated.resources.preset_amrap_description
import rounds.libraries.resources.generated.resources.preset_amrap_name
import rounds.libraries.resources.generated.resources.preset_custom_description
import rounds.libraries.resources.generated.resources.preset_custom_name
import rounds.libraries.resources.generated.resources.preset_emom_description
import rounds.libraries.resources.generated.resources.preset_emom_name
import rounds.libraries.resources.generated.resources.preset_intervals_description
import rounds.libraries.resources.generated.resources.preset_intervals_name
import rounds.libraries.resources.generated.resources.preset_tabata_description
import rounds.libraries.resources.generated.resources.preset_tabata_name
import rounds.libraries.resources.generated.resources.presets_subtitle
import rounds.libraries.resources.generated.resources.presets_title

@Composable
fun TimerPresetScreen(
    viewModel: TimerPresetViewModel,
    onOpenTimer: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    viewModel.ObserveEvents { event ->
        when (event) {
            is TimerPresetEvent.OpenTimer -> onOpenTimer(event.timerId)
            TimerPresetEvent.Close -> onBack()
        }
    }

    TimerPresetContent(
        state = state,
        onSelectPreset = { viewModel.takeAction(TimerPresetAction.SelectPreset(it)) },
        onBack = { viewModel.takeAction(TimerPresetAction.GoBack) },
    )
}

@Composable
private fun TimerPresetContent(
    state: TimerPresetState,
    onSelectPreset: (TimerPreset) -> Unit,
    onBack: () -> Unit,
) {
    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Header(onBack = onBack)

            Spacer(modifier = Modifier.height(Dimension.D500))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimension.D700),
            ) {
                Text(
                    text = stringResource(AppRes.string.presets_title),
                    typography = AppTheme.typography.Heading.H900,
                    color = AppTheme.colors.text,
                )
                Spacer(modifier = Modifier.height(Dimension.D300))
                Text(
                    text = stringResource(AppRes.string.presets_subtitle),
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(Dimension.D700))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimension.D700,
                    end = Dimension.D700,
                    bottom = Dimension.D1200,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimension.D400),
            ) {
                items(state.presets, key = { it.name }) { preset ->
                    PresetCard(
                        preset = preset,
                        onClick = { onSelectPreset(preset) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimension.D400, vertical = Dimension.D400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(icon = Icons.ArrowBack(stringResource(AppRes.string.cd_back)), onClick = onBack)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PresetCard(
    preset: TimerPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCustom = preset == TimerPreset.Custom
    val backgroundColor = if (isCustom) {
        AppTheme.colors.accentPrimary.color
    } else {
        AppTheme.colors.surfacePrimary.color
    }
    val contentColor = if (isCustom) {
        AppTheme.colors.onAccentPrimary
    } else {
        AppTheme.colors.onSurfacePrimary
    }
    val secondaryColor = if (isCustom) {
        AppTheme.colors.onAccentPrimary
    } else {
        AppTheme.colors.textSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .bounceClick(onClick = onClick)
            .padding(horizontal = Dimension.D700, vertical = Dimension.D700),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.displayName(),
                typography = AppTheme.typography.Heading.H700,
                color = contentColor,
            )
            Spacer(modifier = Modifier.height(Dimension.D200))
            Text(
                text = preset.description(),
                typography = AppTheme.typography.Body.B500,
                color = secondaryColor,
            )
        }
        Icon(
            icon = Icons.ChevronRight(null),
            size = IconSize.Small,
            color = contentColor,
        )
    }
}

@Composable
private fun TimerPreset.displayName(): String = stringResource(
    when (this) {
        TimerPreset.Tabata -> AppRes.string.preset_tabata_name
        TimerPreset.EMOM -> AppRes.string.preset_emom_name
        TimerPreset.AMRAP -> AppRes.string.preset_amrap_name
        TimerPreset.Intervals -> AppRes.string.preset_intervals_name
        TimerPreset.Custom -> AppRes.string.preset_custom_name
    }
)

@Composable
private fun TimerPreset.description(): String = stringResource(
    when (this) {
        TimerPreset.Tabata -> AppRes.string.preset_tabata_description
        TimerPreset.EMOM -> AppRes.string.preset_emom_description
        TimerPreset.AMRAP -> AppRes.string.preset_amrap_description
        TimerPreset.Intervals -> AppRes.string.preset_intervals_description
        TimerPreset.Custom -> AppRes.string.preset_custom_description
    }
)

@Composable
@Preview
private fun TimerPresetContentPreview() {
    PreviewContent {
        TimerPresetContent(
            state = TimerPresetState(),
            onSelectPreset = {},
            onBack = {},
        )
    }
}

@Composable
@Preview
private fun PresetCardPreview() {
    PreviewContent {
        Column(
            modifier = Modifier.padding(Dimension.D500),
            verticalArrangement = Arrangement.spacedBy(Dimension.D400),
        ) {
            PresetCard(
                preset = TimerPreset.Tabata,
                onClick = {},
            )
            PresetCard(
                preset = TimerPreset.Custom,
                onClick = {},
            )
        }
    }
}



