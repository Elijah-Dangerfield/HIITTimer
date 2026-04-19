package com.dangerfield.hiittimer.features.timers

import com.dangerfield.hiittimer.libraries.preferences.Preference

enum class SoundMode { Off, Beeps, Voice }

object SoundModePref : Preference<String>() {
    override val key: String = "timer.sound.mode"
    override val default: String = SoundMode.Beeps.name
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
