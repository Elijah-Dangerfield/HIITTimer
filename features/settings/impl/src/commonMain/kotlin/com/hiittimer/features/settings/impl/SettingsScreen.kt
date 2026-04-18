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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.libraries.ui.components.HorizontalDivider
import com.dangerfield.hiittimer.libraries.ui.components.ListItemAccessory
import com.dangerfield.hiittimer.libraries.ui.components.ListSection
import com.dangerfield.hiittimer.libraries.ui.components.ListSectionItem
import com.dangerfield.hiittimer.libraries.ui.components.Screen
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
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is SettingsEvent.OpenUrl) onOpenUrl(event.url)
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
    HorizontalDivider()
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
