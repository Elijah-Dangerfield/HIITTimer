package com.dangerfield.hiittimer.features.timers

import com.dangerfield.hiittimer.libraries.preferences.Preference

enum class SoundMode { Off, Beeps, Voice }

enum class SoundPack(val displayName: String) {
    Classic("Classic"),
    Chime("Chime"),
    Bell("Bell"),
}

object SoundModePref : Preference<String>() {
    override val key: String = "timer.sound.mode"
    override val default: String = SoundMode.Beeps.name
}

object SoundPackPref : Preference<String>() {
    override val key: String = "timer.sound.pack"
    override val default: String = SoundPack.Classic.name
}

/** Volume for timer cues, 0.0..1.0. Layered on top of system volume. */
object CueVolumePref : Preference<Float>() {
    override val key: String = "timer.sound.volume"
    override val default: Float = 0.8f
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
