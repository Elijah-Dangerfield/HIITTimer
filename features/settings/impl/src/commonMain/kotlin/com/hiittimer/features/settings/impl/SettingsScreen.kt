package com.dangerfield.hiittimer.features.settings.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.libraries.flowroutines.ObserveEvents
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.ListItemAccessory
import com.dangerfield.hiittimer.libraries.ui.components.ListSection
import com.dangerfield.hiittimer.libraries.ui.components.ListSectionItem
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.Surface
import com.dangerfield.hiittimer.system.Radii
import kotlin.math.roundToInt
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconResource
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
    onNavigateToFeedback: () -> Unit,
    onNavigateToBugReport: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    viewModel.ObserveEvents { event ->
        when (event) {
            is SettingsEvent.OpenUrl -> onOpenUrl(event.url)
            SettingsEvent.NavigateToFeedback -> onNavigateToFeedback()
            SettingsEvent.NavigateToBugReport -> onNavigateToBugReport()
        }
    }

    val scroll = rememberScrollState()

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Header(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = Dimension.D700),
            ) {
                Spacer(modifier = Modifier.height(Dimension.D900))

                ListSection(
                    title = "Audio",
                    items = listOf(
                        ListSectionItem(
                            headlineText = "Sound",
                            supportingText = soundModeSubtitle(state.soundMode),
                            leadingContent = { SettingsIcon(Icons.VolumeUp("Sound")) },
                            accessory = ListItemAccessory.Text(state.soundMode.displayName()),
                            onClick = { viewModel.takeAction(SettingsAction.CycleSoundMode) },
                        ),
                        ListSectionItem(
                            headlineText = "Sound pack",
                            supportingText = soundPackSubtitle(state.soundPack),
                            leadingContent = { SettingsIcon(Icons.Dot("Sound pack")) },
                            accessory = ListItemAccessory.Text(state.soundPack.displayName),
                            onClick = { viewModel.takeAction(SettingsAction.CycleSoundPack) },
                        ),
                        ListSectionItem(
                            headlineText = "Halfway callouts",
                            supportingText = "Cue at the midpoint of each block.",
                            leadingContent = { SettingsIcon(Icons.Dot("Halfway")) },
                            accessory = ListItemAccessory.Switch(
                                checked = state.halfwayCallouts,
                                onCheckedChange = {
                                    viewModel.takeAction(SettingsAction.SetHalfwayCallouts(it))
                                },
                            ),
                            onClick = null,
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D500))

                VolumeRow(
                    volume = state.cueVolume,
                    onVolumeChange = { viewModel.takeAction(SettingsAction.SetCueVolume(it)) },
                )

                Spacer(modifier = Modifier.height(Dimension.D1000))

                ListSection(
                    title = "Feedback",
                    items = listOf(
                        ListSectionItem(
                            headlineText = "Haptics",
                            supportingText = "Vibrate on transitions and countdown.",
                            leadingContent = { SettingsIcon(Icons.Dot("Haptics")) },
                            accessory = ListItemAccessory.Switch(
                                checked = state.hapticsEnabled,
                                onCheckedChange = {
                                    viewModel.takeAction(SettingsAction.SetHapticsEnabled(it))
                                },
                            ),
                            onClick = null,
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1000))

                ListSection(
                    title = "Runner",
                    items = listOf(
                        ListSectionItem(
                            headlineText = "Show progress bar",
                            supportingText = "Total workout progress while running.",
                            leadingContent = { SettingsIcon(Icons.Chart("Progress bar")) },
                            accessory = ListItemAccessory.Switch(
                                checked = state.showProgressBar,
                                onCheckedChange = {
                                    viewModel.takeAction(SettingsAction.SetShowProgressBar(it))
                                },
                            ),
                            onClick = null,
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1000))

                ListSection(
                    title = "Support",
                    items = listOf(
                        ListSectionItem(
                            headlineText = "Rate the app",
                            leadingContent = { SettingsIcon(Icons.ThumbsUp("Rate")) },
                            onClick = { viewModel.takeAction(SettingsAction.RateApp) },
                        ),
                        ListSectionItem(
                            headlineText = "Tip jar",
                            supportingText = "Buy me a coffee.",
                            leadingContent = { SettingsIcon(Icons.TipJar("Tip jar")) },
                            onClick = { viewModel.takeAction(SettingsAction.OpenTipJar) },
                        ),
                        ListSectionItem(
                            headlineText = "Leave feedback",
                            supportingText = "Tell me what you think.",
                            leadingContent = { SettingsIcon(Icons.Chat("Feedback")) },
                            onClick = { viewModel.takeAction(SettingsAction.LeaveFeedback) },
                        ),
                        ListSectionItem(
                            headlineText = "Report a bug",
                            supportingText = "Something not working right?",
                            leadingContent = { SettingsIcon(Icons.Bug("Bug")) },
                            onClick = { viewModel.takeAction(SettingsAction.ReportBug) },
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1500))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "HIIT Timer",
                        typography = AppTheme.typography.Body.B400,
                        color = AppTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(Dimension.D200))
                    Text(
                        text = "v${state.versionName} (${state.buildNumber})",
                        typography = AppTheme.typography.Caption.C400,
                        color = AppTheme.colors.textSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(Dimension.D1500))
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
        IconButton(icon = Icons.ArrowBack("Back"), onClick = onBack)
        Spacer(modifier = Modifier.size(Dimension.D300))
        Text(text = "Settings", typography = AppTheme.typography.Heading.H800)
    }
}

@Composable
private fun SettingsIcon(icon: IconResource) {
    Icon(
        icon = icon,
        color = AppTheme.colors.onSurfacePrimary,
        size = IconSize.Small,
    )
}

private fun SoundMode.displayName(): String = when (this) {
    SoundMode.Off -> "Off"
    SoundMode.Beeps -> "Beeps"
    SoundMode.Voice -> "Voice"
}

private fun soundModeSubtitle(mode: SoundMode): String = when (mode) {
    SoundMode.Off -> "No sound during workout."
    SoundMode.Beeps -> "Short tones on transitions and countdown."
    SoundMode.Voice -> "Spoken block names and countdown."
}

private fun soundPackSubtitle(pack: SoundPack): String = when (pack) {
    SoundPack.Classic -> "Clean sine-wave beeps."
    SoundPack.Chime -> "Soft, resonant chimes."
    SoundPack.Bell -> "Struck bell, metallic and punchy."
}

@Composable
private fun VolumeRow(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Surface(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
        radius = Radii.Card,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Dimension.D600,
            vertical = Dimension.D500,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.VolumeUp("Cue volume"))
                Spacer(modifier = Modifier.size(Dimension.D500))
                Text(
                    text = "Cue volume",
                    typography = AppTheme.typography.Body.B700.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(end = Dimension.D400),
                )
            }
            Spacer(modifier = Modifier.height(Dimension.D300))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.fillMaxWidth().padding(end = Dimension.D400),
                    colors = SliderDefaults.colors(
                        thumbColor = AppTheme.colors.accentPrimary.color,
                        activeTrackColor = AppTheme.colors.accentPrimary.color,
                        inactiveTrackColor = AppTheme.colors.surfaceDisabled.color,
                    ),
                )
                Text(
                    text = "${(volume * 100).roundToInt()}%",
                    typography = AppTheme.typography.Label.L400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(Dimension.D100))
            Text(
                text = "Independent from your phone volume. Music stays loud; cues stay whatever you pick.",
                typography = AppTheme.typography.Body.B400,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}
