package com.dangerfield.hiittimer.features.timers

import com.dangerfield.hiittimer.libraries.preferences.Preference

enum class SoundMode { Off, Beeps, Voice }

enum class SoundPack(val displayName: String) {
    Classic("Classic"),
    Chime("Chime"),
    Bell("Bell"),
}

/** Master on/off for all timer audio cues. When false, cues are silent regardless of per-role mode. */
object SoundsEnabledPref : Preference<Boolean>() {
    override val key: String = "timer.sounds_enabled"
    override val default: Boolean = true
}

/** Event buckets that get their own sound selection. */
enum class CueRole {
    Countdown, // 3-2-1 preroll and end-of-block ticks
    BlockStart, // the "go" moment each block begins
    Halfway, // midpoint marker (gated by HalfwayCalloutsPref)
    Finish, // end of the whole workout
}

object CountdownPackPref : Preference<String>() {
    override val key: String = "timer.sound.pack.countdown"
    override val default: String = SoundPack.Classic.name
}

object BlockStartPackPref : Preference<String>() {
    override val key: String = "timer.sound.pack.block_start"
    override val default: String = SoundPack.Classic.name
}

object HalfwayPackPref : Preference<String>() {
    override val key: String = "timer.sound.pack.halfway"
    override val default: String = SoundPack.Classic.name
}

object FinishPackPref : Preference<String>() {
    override val key: String = "timer.sound.pack.finish"
    override val default: String = SoundPack.Classic.name
}

fun CueRole.pref(): Preference<String> = when (this) {
    CueRole.Countdown -> CountdownPackPref
    CueRole.BlockStart -> BlockStartPackPref
    CueRole.Halfway -> HalfwayPackPref
    CueRole.Finish -> FinishPackPref
}

object CountdownModePref : Preference<String>() {
    override val key: String = "timer.sound.mode.countdown"
    override val default: String = SoundMode.Beeps.name
}

object BlockStartModePref : Preference<String>() {
    override val key: String = "timer.sound.mode.block_start"
    override val default: String = SoundMode.Beeps.name
}

object HalfwayModePref : Preference<String>() {
    override val key: String = "timer.sound.mode.halfway"
    override val default: String = SoundMode.Beeps.name
}

object FinishModePref : Preference<String>() {
    override val key: String = "timer.sound.mode.finish"
    override val default: String = SoundMode.Beeps.name
}

fun CueRole.modePref(): Preference<String> = when (this) {
    CueRole.Countdown -> CountdownModePref
    CueRole.BlockStart -> BlockStartModePref
    CueRole.Halfway -> HalfwayModePref
    CueRole.Finish -> FinishModePref
}

/** Volume for timer cues, 0.0..1.0. Layered on top of system volume. */
object CueVolumePref : Preference<Float>() {
    override val key: String = "timer.sound.volume"
    override val default: Float = 0.8f
}

/** Platform-specific voice identifier for TTS cues. Empty string means system default. */
object VoiceIdPref : Preference<String>() {
    override val key: String = "timer.sound.voice_id"
    override val default: String = ""
}

object HapticsEnabledPref : Preference<Boolean>() {
    override val key: String = "timer.haptics.enabled"
    override val default: Boolean = true
}

object HalfwayCalloutsPref : Preference<Boolean>() {
    override val key: String = "timer.halfway_callouts"
    override val default: Boolean = false
}

object ShowProgressBarPref : Preference<Boolean>() {
    override val key: String = "timer.runner.show_progress"
    override val default: Boolean = true
}

object CompletedWorkoutsPref : Preference<Int>() {
    override val key: String = "app.completed_workouts"
    override val default: Int = 0
}

object ReviewPromptShownPref : Preference<Boolean>() {
    override val key: String = "app.review_prompt_shown"
    override val default: Boolean = false
}

object SkipBlockDeleteConfirmationPref : Preference<Boolean>() {
    override val key: String = "block.skip_delete_confirmation"
    override val default: Boolean = false
}

object ExampleTimerSeededPref : Preference<Boolean>() {
    override val key: String = "app.example_timer_seeded"
    override val default: Boolean = false
}

object HasSeenRunnerVolumeTooltipPref : Preference<Boolean>() {
    override val key: String = "runner.has_seen_volume_tooltip"
    override val default: Boolean = false
}
