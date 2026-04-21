package com.dangerfield.hiittimer.features.timers

data class VoiceOption(
    /** Platform-specific stable identifier. iOS: AVSpeechSynthesisVoice.identifier.
     *  Android: Voice.name. Empty means "system default". */
    val id: String,
    val displayName: String,
    /** BCP-47 language tag e.g. "en-US", "en-GB". */
    val languageTag: String,
    val isDefault: Boolean = false,
)

interface VoiceCatalog {
    /** Voices installed on this device. May be empty if voice synthesis isn't usable.
     *  The list always begins with a synthetic "System default" entry whose id is "". */
    suspend fun availableVoices(): List<VoiceOption>

    /** Speak a sample phrase with the given voice. Pass null / empty for system default. */
    fun preview(voiceId: String?, phrase: String = "Three. Two. One. Go.", volume: Float = 0.8f)

    fun stopPreview()
}
