package com.dangerfield.hiittimer.features.settings.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.libraries.ui.components.Card
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.Switch
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is SettingsEvent.OpenUrl) onOpenUrl(event.url)
        }
    }

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(onBack = onBack)

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
                item { SectionLabel("Audio") }
                item {
                    SoundModeCard(
                        current = state.soundMode,
                        onChange = { viewModel.takeAction(SettingsAction.SetSoundMode(it)) },
                    )
                }
                item {
                    ToggleCard(
                        title = "Halfway callouts",
                        subtitle = "Play a cue at the midpoint of each block",
                        checked = state.halfwayCallouts,
                        onChange = { viewModel.takeAction(SettingsAction.SetHalfwayCallouts(it)) },
                    )
                }

                item { Spacer(modifier = Modifier.height(Dimension.D500)) }
                item { SectionLabel("Runner") }
                item {
                    ToggleCard(
                        title = "Show progress bar",
                        subtitle = "Total workout progress on the runner screen",
                        checked = state.showProgressBar,
                        onChange = { viewModel.takeAction(SettingsAction.SetShowProgressBar(it)) },
                    )
                }

                item { Spacer(modifier = Modifier.height(Dimension.D500)) }
                item { SectionLabel("Support") }
                item {
                    ActionCard(
                        title = "Rate the app",
                        subtitle = "If you're enjoying it, a rating helps a lot.",
                        icon = Icons.ThumbsUp("Rate"),
                        onClick = { viewModel.takeAction(SettingsAction.RateApp) },
                    )
                }
                item {
                    ActionCard(
                        title = "Tip jar",
                        subtitle = "Buy me a coffee",
                        icon = Icons.TipJar("Tip jar"),
                        onClick = { viewModel.takeAction(SettingsAction.OpenTipJar) },
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
            .padding(horizontal = Dimension.D500, vertical = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(icon = Icons.ArrowBack("Back"), onClick = onBack)
        Spacer(modifier = Modifier.size(Dimension.D300))
        Text(text = "Settings", typography = AppTheme.typography.Heading.H700)
    }
    HorizontalDivider()
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
private fun SoundModeCard(current: SoundMode, onChange: (SoundMode) -> Unit) {
    Card(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
    ) {
        Column {
            Text(text = "Sound", typography = AppTheme.typography.Body.B500)
            Spacer(modifier = Modifier.height(Dimension.D500))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
            ) {
                SoundMode.entries.forEach { mode ->
                    SoundModeChip(
                        mode = mode,
                        selected = mode == current,
                        onClick = { onChange(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundModeChip(
    mode: SoundMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val containerColor = if (selected) AppTheme.colors.accentPrimary else AppTheme.colors.surfaceSecondary
    val textColor = if (selected) AppTheme.colors.onAccentPrimary else AppTheme.colors.onSurfaceSecondary
    Card(
        modifier = modifier,
        onClick = onClick,
        color = containerColor,
        contentColor = textColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = mode.name,
                typography = AppTheme.typography.Label.L500,
                color = textColor,
            )
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Card(
        onClick = { onChange(!checked) },
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, typography = AppTheme.typography.Body.B500)
                Text(
                    text = subtitle,
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.size(Dimension.D500))
            Switch(checked = checked, onCheckedChange = { onChange(it) })
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: com.dangerfield.hiittimer.libraries.ui.components.icon.IconResource,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon = icon, size = IconSize.Medium, color = AppTheme.colors.accentPrimary)
            Spacer(modifier = Modifier.size(Dimension.D500))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, typography = AppTheme.typography.Body.B500)
                Text(
                    text = subtitle,
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Icon(icon = Icons.ChevronRight("Open"), size = IconSize.Small)
        }
    }
}
