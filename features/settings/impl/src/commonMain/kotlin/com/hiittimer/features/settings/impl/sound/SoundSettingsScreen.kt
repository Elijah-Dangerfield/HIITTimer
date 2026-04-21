package com.dangerfield.hiittimer.features.settings.impl.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.features.timers.CueRole
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.VoiceOption
import com.dangerfield.hiittimer.libraries.flowroutines.ObserveEvents
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.libraries.ui.bounceClick
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.Surface
import com.dangerfield.hiittimer.libraries.ui.components.Switch
import com.dangerfield.hiittimer.libraries.ui.components.dialog.bottomsheet.BottomSheet
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icon
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconButton
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconResource
import com.dangerfield.hiittimer.libraries.ui.components.icon.IconSize
import com.dangerfield.hiittimer.libraries.ui.components.icon.Icons
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import com.dangerfield.hiittimer.system.Radii
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.cd_back
import rounds.libraries.resources.generated.resources.cd_change_voice
import rounds.libraries.resources.generated.resources.cd_preview
import rounds.libraries.resources.generated.resources.cd_selected
import rounds.libraries.resources.generated.resources.cd_volume
import rounds.libraries.resources.generated.resources.cue_role_block_start
import rounds.libraries.resources.generated.resources.cue_role_block_start_desc
import rounds.libraries.resources.generated.resources.cue_role_countdown
import rounds.libraries.resources.generated.resources.cue_role_countdown_desc
import rounds.libraries.resources.generated.resources.cue_role_finish
import rounds.libraries.resources.generated.resources.cue_role_finish_desc
import rounds.libraries.resources.generated.resources.cue_role_halfway
import rounds.libraries.resources.generated.resources.cue_role_halfway_desc
import rounds.libraries.resources.generated.resources.sound_mode_beeps
import rounds.libraries.resources.generated.resources.sound_mode_off
import rounds.libraries.resources.generated.resources.sound_mode_voice
import rounds.libraries.resources.generated.resources.sound_pack_bell
import rounds.libraries.resources.generated.resources.sound_pack_chime
import rounds.libraries.resources.generated.resources.sound_pack_classic
import rounds.libraries.resources.generated.resources.sound_settings_cues_header
import rounds.libraries.resources.generated.resources.sound_settings_cues_subtitle
import rounds.libraries.resources.generated.resources.sound_settings_halfway_subtitle
import rounds.libraries.resources.generated.resources.sound_settings_halfway_title
import rounds.libraries.resources.generated.resources.sound_settings_master_subtitle
import rounds.libraries.resources.generated.resources.sound_settings_master_title
import rounds.libraries.resources.generated.resources.sound_settings_title
import rounds.libraries.resources.generated.resources.sound_settings_voice_label
import rounds.libraries.resources.generated.resources.sound_settings_voice_system_default
import rounds.libraries.resources.generated.resources.sound_settings_volume_percent
import rounds.libraries.resources.generated.resources.sound_settings_volume_subtitle
import rounds.libraries.resources.generated.resources.sound_settings_volume_title

@Composable
fun SoundSettingsScreen(
    viewModel: SoundSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    viewModel.ObserveEvents { event ->
        when (event) {
            SoundSettingsEvent.Close -> onBack()
        }
    }

    SoundSettingsContent(
        state = state,
        onSetSoundsEnabled = { viewModel.takeAction(SoundSettingsAction.SetSoundsEnabled(it)) },
        onSetCueMode = { role, mode -> viewModel.takeAction(SoundSettingsAction.SetCueMode(role, mode)) },
        onSetCuePack = { role, pack -> viewModel.takeAction(SoundSettingsAction.SetCuePack(role, pack)) },
        onPreviewCue = { viewModel.takeAction(SoundSettingsAction.PreviewCue(it)) },
        onSetVolume = { viewModel.takeAction(SoundSettingsAction.SetCueVolume(it)) },
        onSetVoice = { viewModel.takeAction(SoundSettingsAction.SetVoice(it)) },
        onSetHalfway = { viewModel.takeAction(SoundSettingsAction.SetHalfwayCallouts(it)) },
        onBack = { viewModel.takeAction(SoundSettingsAction.GoBack) },
    )
}

@Composable
private fun SoundSettingsContent(
    state: SoundSettingsState,
    onSetSoundsEnabled: (Boolean) -> Unit,
    onSetCueMode: (CueRole, SoundMode) -> Unit,
    onSetCuePack: (CueRole, SoundPack) -> Unit,
    onPreviewCue: (CueRole) -> Unit,
    onSetVolume: (Float) -> Unit,
    onSetVoice: (String) -> Unit,
    onSetHalfway: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var showVoicePicker by remember { mutableStateOf(false) }

    Screen(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Header(onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Dimension.D700,
                    vertical = Dimension.D500,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimension.D700),
            ) {
                item(key = "master") {
                    MasterToggleRow(
                        enabled = state.soundsEnabled,
                        onToggle = onSetSoundsEnabled,
                    )
                }

                if (state.soundsEnabled) {
                    item(key = "cues-header") {
                        Column {
                            Text(
                                text = stringResource(AppRes.string.sound_settings_cues_header),
                                typography = AppTheme.typography.Label.L500.SemiBold,
                                color = AppTheme.colors.textSecondary,
                            )
                            Spacer(modifier = Modifier.height(Dimension.D200))
                            Text(
                                text = stringResource(AppRes.string.sound_settings_cues_subtitle),
                                typography = AppTheme.typography.Body.B400,
                                color = AppTheme.colors.textSecondary,
                            )
                        }
                    }

                    if (state.anyRoleUsesVoice && state.availableVoices.isNotEmpty()) {
                        item(key = "voice-row") {
                            VoiceRow(
                                selected = state.selectedVoice,
                                onClick = { showVoicePicker = true },
                            )
                        }
                    }

                    CueRole.entries.forEach { role ->
                        if (role == CueRole.Halfway && !state.halfwayCallouts) {
                            item(key = "cue-Halfway") {
                                HalfwayToggleRow(
                                    enabled = state.halfwayCallouts,
                                    onToggle = onSetHalfway,
                                )
                            }
                        } else {
                            item(key = "cue-${role.name}") {
                                CueCard(
                                    role = role,
                                    mode = state.cueModes[role] ?: SoundMode.Beeps,
                                    pack = state.cuePacks[role] ?: SoundPack.Classic,
                                    onSelectMode = { mode -> onSetCueMode(role, mode) },
                                    onSelectPack = { pack -> onSetCuePack(role, pack) },
                                    onPreview = { onPreviewCue(role) },
                                    trailing = if (role == CueRole.Halfway) {
                                        {
                                            Switch(
                                                checked = state.halfwayCallouts,
                                                onCheckedChange = onSetHalfway,
                                            )
                                        }
                                    } else null,
                                )
                            }
                        }
                    }

                    item(key = "volume") {
                        VolumeSection(
                            volume = state.cueVolume,
                            onVolumeChange = onSetVolume,
                        )
                    }
                }

                item(key = "spacer") {
                    Spacer(modifier = Modifier.height(Dimension.D1200))
                }
            }
        }
    }

    if (showVoicePicker) {
        VoicePickerSheet(
            voices = state.availableVoices,
            selectedId = state.voiceId,
            onSelect = { id ->
                onSetVoice(id)
            },
            onDismiss = { showVoicePicker = false },
        )
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
        Text(text = stringResource(AppRes.string.sound_settings_title), typography = AppTheme.typography.Heading.H800)
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
private fun MasterToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
        radius = Radii.Card,
        contentPadding = PaddingValues(
            horizontal = Dimension.D600,
            vertical = Dimension.D500,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(AppRes.string.sound_settings_master_title),
                    typography = AppTheme.typography.Body.B700.SemiBold,
                )
                Spacer(modifier = Modifier.height(Dimension.D100))
                Text(
                    text = stringResource(AppRes.string.sound_settings_master_subtitle),
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.size(Dimension.D400))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun CueCard(
    role: CueRole,
    mode: SoundMode,
    pack: SoundPack,
    onSelectMode: (SoundMode) -> Unit,
    onSelectPack: (SoundPack) -> Unit,
    onPreview: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
        radius = Radii.Card,
        contentPadding = PaddingValues(
            horizontal = Dimension.D600,
            vertical = Dimension.D500,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = role.displayName(),
                        typography = AppTheme.typography.Body.B700.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(Dimension.D100))
                    Text(
                        text = role.description(),
                        typography = AppTheme.typography.Body.B400,
                        color = AppTheme.colors.textSecondary,
                    )
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.size(Dimension.D400))
                    trailing()
                } else if (mode == SoundMode.Beeps) {
                    PreviewButton(onClick = onPreview)
                }
            }

            Spacer(modifier = Modifier.height(Dimension.D400))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
            ) {
                SoundMode.entries.forEach { m ->
                    ModeChip(
                        mode = m,
                        isSelected = m == mode,
                        onClick = { onSelectMode(m) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (mode == SoundMode.Beeps) {
                Spacer(modifier = Modifier.height(Dimension.D400))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
                ) {
                    SoundPack.entries.forEach { p ->
                        PackChip(
                            pack = p,
                            isSelected = p == pack,
                            onClick = { onSelectPack(p) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    mode: SoundMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = AppTheme.colors.accentPrimary.color
    val bg = if (isSelected) accent else AppTheme.colors.surfaceSecondary.color
    val fg = if (isSelected) AppTheme.colors.onAccentPrimary else AppTheme.colors.onSurfacePrimary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) accent else AppTheme.colors.border.color,
                shape = RoundedCornerShape(12.dp),
            )
            .bounceClick(onClick = onClick)
            .padding(vertical = Dimension.D400, horizontal = Dimension.D300),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon = when (mode) {
                SoundMode.Off -> Icons.VolumeOff(stringResource(AppRes.string.sound_mode_off))
                SoundMode.Beeps -> Icons.VolumeUp(stringResource(AppRes.string.sound_mode_beeps))
                SoundMode.Voice -> Icons.Chat(stringResource(AppRes.string.sound_mode_voice))
            },
            size = IconSize.Small,
            color = fg,
        )
        Spacer(modifier = Modifier.size(Dimension.D200))
        Text(
            text = mode.displayName(),
            typography = AppTheme.typography.Label.L500.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun PreviewButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.accentPrimary.color.copy(alpha = 0.18f))
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = Icons.Play(stringResource(AppRes.string.cd_preview)),
            size = IconSize.Small,
            color = AppTheme.colors.accentPrimary,
        )
    }
}

@Composable
private fun PackChip(
    pack: SoundPack,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = AppTheme.colors.accentPrimary.color
    val bg = if (isSelected) accent else AppTheme.colors.surfaceSecondary.color
    val fg = if (isSelected) AppTheme.colors.onAccentPrimary else AppTheme.colors.onSurfacePrimary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) accent else AppTheme.colors.border.color,
                shape = RoundedCornerShape(12.dp),
            )
            .bounceClick(onClick = onClick)
            .padding(vertical = Dimension.D400, horizontal = Dimension.D300),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = pack.displayName(),
            typography = AppTheme.typography.Label.L500.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun VoiceRow(selected: VoiceOption?, onClick: () -> Unit) {
    Surface(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
        radius = Radii.Card,
        contentPadding = PaddingValues(
            horizontal = Dimension.D600,
            vertical = Dimension.D500,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(AppRes.string.sound_settings_voice_label),
                    typography = AppTheme.typography.Body.B700.SemiBold,
                )
                Spacer(modifier = Modifier.height(Dimension.D100))
                Text(
                    text = selected?.displayName ?: stringResource(AppRes.string.sound_settings_voice_system_default),
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Icon(
                icon = Icons.ChevronRight(stringResource(AppRes.string.cd_change_voice)),
                color = AppTheme.colors.textSecondary,
                size = IconSize.Small,
            )
        }
    }
}

@Composable
private fun VoicePickerSheet(
    voices: List<VoiceOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(
        onDismissRequest = onDismiss,
        showDragHandle = true,
    ) {
        Text(
            text = stringResource(AppRes.string.sound_settings_voice_label),
            typography = AppTheme.typography.Heading.H700,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimension.D700, vertical = Dimension.D400),
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = Dimension.D500,
                vertical = Dimension.D300,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimension.D200),
        ) {
            items(voices, key = { it.id.ifEmpty { "default" } }) { voice ->
                VoiceRowItem(
                    voice = voice,
                    isSelected = voice.id == selectedId,
                    onClick = { onSelect(voice.id) },
                )
            }
            item { Spacer(modifier = Modifier.height(Dimension.D700)) }
        }
    }
}

@Composable
private fun VoiceRowItem(
    voice: VoiceOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accent = AppTheme.colors.accentPrimary.color
    val bg = if (isSelected) accent.copy(alpha = 0.18f) else AppTheme.colors.surfacePrimary.color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .bounceClick(onClick = onClick)
            .padding(horizontal = Dimension.D500, vertical = Dimension.D400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = voice.displayName,
                typography = AppTheme.typography.Body.B500.SemiBold,
            )
            if (voice.languageTag.isNotBlank() && !voice.isDefault) {
                Spacer(modifier = Modifier.height(Dimension.D100))
                Text(
                    text = voice.languageTag,
                    typography = AppTheme.typography.Label.L400,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        if (isSelected) {
            Icon(
                icon = Icons.Check(stringResource(AppRes.string.cd_selected)),
                color = AppTheme.colors.accentPrimary,
                size = IconSize.Small,
            )
        }
    }
}

@Composable
private fun HalfwayToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
        radius = Radii.Card,
        contentPadding = PaddingValues(
            horizontal = Dimension.D600,
            vertical = Dimension.D500,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(AppRes.string.sound_settings_halfway_title),
                    typography = AppTheme.typography.Body.B700.SemiBold,
                )
                Spacer(modifier = Modifier.height(Dimension.D100))
                Text(
                    text = stringResource(AppRes.string.sound_settings_halfway_subtitle),
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.size(Dimension.D400))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun VolumeSection(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Surface(
        color = AppTheme.colors.surfacePrimary,
        contentColor = AppTheme.colors.onSurfacePrimary,
        radius = Radii.Card,
        contentPadding = PaddingValues(
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
                SettingsIcon(Icons.VolumeUp(stringResource(AppRes.string.cd_volume)))
                Spacer(modifier = Modifier.size(Dimension.D500))
                Text(
                    text = stringResource(AppRes.string.sound_settings_volume_title),
                    typography = AppTheme.typography.Body.B700.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(AppRes.string.sound_settings_volume_percent, (volume * 100).roundToInt()),
                    typography = AppTheme.typography.Label.L500,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(Dimension.D300))
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = AppTheme.colors.accentPrimary.color,
                    activeTrackColor = AppTheme.colors.accentPrimary.color,
                    inactiveTrackColor = AppTheme.colors.surfaceDisabled.color,
                ),
            )
            Text(
                text = stringResource(AppRes.string.sound_settings_volume_subtitle),
                typography = AppTheme.typography.Body.B400,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SoundPack.displayName(): String = stringResource(
    when (this) {
        SoundPack.Classic -> AppRes.string.sound_pack_classic
        SoundPack.Chime -> AppRes.string.sound_pack_chime
        SoundPack.Bell -> AppRes.string.sound_pack_bell
    }
)

@Composable
private fun SoundMode.displayName(): String = stringResource(
    when (this) {
        SoundMode.Off -> AppRes.string.sound_mode_off
        SoundMode.Beeps -> AppRes.string.sound_mode_beeps
        SoundMode.Voice -> AppRes.string.sound_mode_voice
    }
)

@Composable
private fun CueRole.displayName(): String = stringResource(
    when (this) {
        CueRole.Countdown -> AppRes.string.cue_role_countdown
        CueRole.BlockStart -> AppRes.string.cue_role_block_start
        CueRole.Halfway -> AppRes.string.cue_role_halfway
        CueRole.Finish -> AppRes.string.cue_role_finish
    }
)

@Composable
private fun CueRole.description(): String = stringResource(
    when (this) {
        CueRole.Countdown -> AppRes.string.cue_role_countdown_desc
        CueRole.BlockStart -> AppRes.string.cue_role_block_start_desc
        CueRole.Halfway -> AppRes.string.cue_role_halfway_desc
        CueRole.Finish -> AppRes.string.cue_role_finish_desc
    }
)

@Composable
@Preview
private fun SoundSettingsContentPreview() {
    PreviewContent {
        SoundSettingsContent(
            state = SoundSettingsState(halfwayCallouts = true),
            onSetSoundsEnabled = {},
            onSetCueMode = { _, _ -> },
            onSetCuePack = { _, _ -> },
            onPreviewCue = {},
            onSetVolume = {},
            onSetVoice = {},
            onSetHalfway = {},
            onBack = {},
        )
    }
}

@Composable
@Preview
private fun SoundSettingsContentMixedPreview() {
    PreviewContent {
        SoundSettingsContent(
            state = SoundSettingsState(
                cueModes = mapOf(
                    CueRole.Countdown to SoundMode.Beeps,
                    CueRole.BlockStart to SoundMode.Voice,
                    CueRole.Halfway to SoundMode.Beeps,
                    CueRole.Finish to SoundMode.Voice,
                ),
                halfwayCallouts = true,
            ),
            onSetSoundsEnabled = {},
            onSetCueMode = { _, _ -> },
            onSetCuePack = { _, _ -> },
            onPreviewCue = {},
            onSetVolume = {},
            onSetVoice = {},
            onSetHalfway = {},
            onBack = {},
        )
    }
}

@Composable
@Preview
private fun SoundSettingsContentMutedPreview() {
    PreviewContent {
        SoundSettingsContent(
            state = SoundSettingsState(soundsEnabled = false),
            onSetSoundsEnabled = {},
            onSetCueMode = { _, _ -> },
            onSetCuePack = { _, _ -> },
            onPreviewCue = {},
            onSetVolume = {},
            onSetVoice = {},
            onSetHalfway = {},
            onBack = {},
        )
    }
}
