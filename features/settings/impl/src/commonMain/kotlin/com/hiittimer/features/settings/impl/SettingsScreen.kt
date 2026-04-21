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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.libraries.flowroutines.ObserveEvents
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
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
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.app_name
import rounds.libraries.resources.generated.resources.cd_back
import rounds.libraries.resources.generated.resources.cd_bug
import rounds.libraries.resources.generated.resources.cd_feedback
import rounds.libraries.resources.generated.resources.cd_haptics
import rounds.libraries.resources.generated.resources.cd_privacy_policy
import rounds.libraries.resources.generated.resources.cd_progress_bar
import rounds.libraries.resources.generated.resources.cd_rate
import rounds.libraries.resources.generated.resources.cd_sound
import rounds.libraries.resources.generated.resources.cd_terms_of_service
import rounds.libraries.resources.generated.resources.cd_tip_jar
import rounds.libraries.resources.generated.resources.settings_haptics
import rounds.libraries.resources.generated.resources.settings_haptics_subtitle
import rounds.libraries.resources.generated.resources.settings_leave_feedback
import rounds.libraries.resources.generated.resources.settings_leave_feedback_subtitle
import rounds.libraries.resources.generated.resources.settings_privacy_policy
import rounds.libraries.resources.generated.resources.settings_rate_app
import rounds.libraries.resources.generated.resources.settings_report_bug
import rounds.libraries.resources.generated.resources.settings_report_bug_subtitle
import rounds.libraries.resources.generated.resources.settings_section_audio
import rounds.libraries.resources.generated.resources.settings_section_feedback
import rounds.libraries.resources.generated.resources.settings_section_legal
import rounds.libraries.resources.generated.resources.settings_section_runner
import rounds.libraries.resources.generated.resources.settings_section_support
import rounds.libraries.resources.generated.resources.settings_show_progress
import rounds.libraries.resources.generated.resources.settings_show_progress_subtitle
import rounds.libraries.resources.generated.resources.settings_sound_off
import rounds.libraries.resources.generated.resources.settings_sound_on
import rounds.libraries.resources.generated.resources.settings_sound_settings
import rounds.libraries.resources.generated.resources.settings_terms_of_service
import rounds.libraries.resources.generated.resources.settings_tip_jar
import rounds.libraries.resources.generated.resources.settings_tip_jar_subtitle
import rounds.libraries.resources.generated.resources.settings_title
import rounds.libraries.resources.generated.resources.settings_version_build

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToBugReport: () -> Unit,
    onNavigateToSoundSettings: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    viewModel.ObserveEvents { event ->
        when (event) {
            is SettingsEvent.OpenUrl -> onOpenUrl(event.url)
            SettingsEvent.NavigateToFeedback -> onNavigateToFeedback()
            SettingsEvent.NavigateToBugReport -> onNavigateToBugReport()
            SettingsEvent.NavigateToSoundSettings -> onNavigateToSoundSettings()
        }
    }

    SettingsContent(
        state = state,
        onBack = onBack,
        onAction = viewModel::takeAction,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
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
                    title = stringResource(AppRes.string.settings_section_audio),
                    items = listOf(
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_sound_settings),
                            supportingText = if (state.soundsEnabled) stringResource(AppRes.string.settings_sound_on) else stringResource(AppRes.string.settings_sound_off),
                            leadingContent = { SettingsIcon(Icons.VolumeUp(stringResource(AppRes.string.cd_sound))) },
                            accessory = ListItemAccessory.Chevron,
                            onClick = { onAction(SettingsAction.OpenSoundSettings) },
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1000))

                ListSection(
                    title = stringResource(AppRes.string.settings_section_feedback),
                    items = listOf(
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_haptics),
                            supportingText = stringResource(AppRes.string.settings_haptics_subtitle),
                            leadingContent = { SettingsIcon(Icons.Dot(stringResource(AppRes.string.cd_haptics))) },
                            accessory = ListItemAccessory.Switch(
                                checked = state.hapticsEnabled,
                                onCheckedChange = {
                                    onAction(SettingsAction.SetHapticsEnabled(it))
                                },
                            ),
                            onClick = null,
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1000))

                ListSection(
                    title = stringResource(AppRes.string.settings_section_runner),
                    items = listOf(
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_show_progress),
                            supportingText = stringResource(AppRes.string.settings_show_progress_subtitle),
                            leadingContent = { SettingsIcon(Icons.Chart(stringResource(AppRes.string.cd_progress_bar))) },
                            accessory = ListItemAccessory.Switch(
                                checked = state.showProgressBar,
                                onCheckedChange = {
                                    onAction(SettingsAction.SetShowProgressBar(it))
                                },
                            ),
                            onClick = null,
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1000))

                ListSection(
                    title = stringResource(AppRes.string.settings_section_support),
                    items = listOf(
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_rate_app),
                            leadingContent = { SettingsIcon(Icons.ThumbsUp(stringResource(AppRes.string.cd_rate))) },
                            onClick = { onAction(SettingsAction.RateApp) },
                        ),
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_tip_jar),
                            supportingText = stringResource(AppRes.string.settings_tip_jar_subtitle),
                            leadingContent = { SettingsIcon(Icons.TipJar(stringResource(AppRes.string.cd_tip_jar))) },
                            onClick = { onAction(SettingsAction.OpenTipJar) },
                        ),
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_leave_feedback),
                            supportingText = stringResource(AppRes.string.settings_leave_feedback_subtitle),
                            leadingContent = { SettingsIcon(Icons.Chat(stringResource(AppRes.string.cd_feedback))) },
                            onClick = { onAction(SettingsAction.LeaveFeedback) },
                        ),
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_report_bug),
                            supportingText = stringResource(AppRes.string.settings_report_bug_subtitle),
                            leadingContent = { SettingsIcon(Icons.Bug(stringResource(AppRes.string.cd_bug))) },
                            onClick = { onAction(SettingsAction.ReportBug) },
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1000))

                ListSection(
                    title = stringResource(AppRes.string.settings_section_legal),
                    items = listOf(
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_privacy_policy),
                            leadingContent = { SettingsIcon(Icons.Lock(stringResource(AppRes.string.cd_privacy_policy))) },
                            accessory = ListItemAccessory.Chevron,
                            onClick = { onAction(SettingsAction.OpenPrivacyPolicy) },
                        ),
                        ListSectionItem(
                            headlineText = stringResource(AppRes.string.settings_terms_of_service),
                            leadingContent = { SettingsIcon(Icons.Info(stringResource(AppRes.string.cd_terms_of_service))) },
                            accessory = ListItemAccessory.Chevron,
                            onClick = { onAction(SettingsAction.OpenTermsOfService) },
                        ),
                    )
                )

                Spacer(modifier = Modifier.height(Dimension.D1500))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(AppRes.string.app_name),
                        typography = AppTheme.typography.Body.B400,
                        color = AppTheme.colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(Dimension.D200))
                    Text(
                        text = stringResource(AppRes.string.settings_version_build, state.versionName, state.buildNumber.toString()),
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
        IconButton(icon = Icons.ArrowBack(stringResource(AppRes.string.cd_back)), onClick = onBack)
        Spacer(modifier = Modifier.size(Dimension.D300))
        Text(text = stringResource(AppRes.string.settings_title), typography = AppTheme.typography.Heading.H800)
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

@Composable
@Preview
private fun SettingsContentPreview() {
    PreviewContent {
        SettingsContent(
            state = SettingsState(
                soundsEnabled = true,
                cueVolume = 0.8f,
                hapticsEnabled = true,
                showProgressBar = true,
                versionName = "1.2.0",
                buildNumber = 42,
            ),
            onBack = {},
            onAction = {},
        )
    }
}
