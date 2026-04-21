package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.VoiceCatalog
import com.dangerfield.hiittimer.features.timers.VoiceOption
import kotlinx.cinterop.ExperimentalForeignApi
import me.tatarka.inject.annotations.Inject
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@OptIn(ExperimentalForeignApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosVoiceCatalog : VoiceCatalog {

    private val synthesizer by lazy { AVSpeechSynthesizer() }

    override suspend fun availableVoices(): List<VoiceOption> {
        val systemDefault = VoiceOption(
            id = "",
            displayName = "System default",
            languageTag = AVSpeechSynthesisVoice.currentLanguageCode(),
            isDefault = true,
        )
        val installed = AVSpeechSynthesisVoice.speechVoices()
            .filterIsInstance<AVSpeechSynthesisVoice>()
            .filter { it.name in CURATED_VOICE_NAMES }
            .distinctBy { it.name }
            .map {
                VoiceOption(
                    id = it.identifier,
                    displayName = it.name,
                    languageTag = it.language,
                )
            }
            .sortedBy { CURATED_VOICE_NAMES.indexOf(it.displayName) }
        return listOf(systemDefault) + installed
    }

    override fun preview(voiceId: String?, phrase: String, volume: Float) {
        ensureSessionActive()
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance.speechUtteranceWithString(phrase)
        utterance.rate = 0.52f
        utterance.volume = volume.coerceIn(0f, 1f)
        utterance.voice = voiceId?.takeIf { it.isNotBlank() }
            ?.let { AVSpeechSynthesisVoice.voiceWithIdentifier(it) }
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        synthesizer.speakUtterance(utterance)
    }

    override fun stopPreview() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    /** Ensure the shared session is in a category that allows Playback with other audio mixing.
     *  Settings can be reached before the audio cue player has initialized its session. */
    private fun ensureSessionActive() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayback,
            AVAudioSessionModeDefault,
            MIX_WITH_OTHERS,
            null,
        )
        session.setActive(true, null)
    }

    private companion object {
        const val MIX_WITH_OTHERS: ULong = 1u

        /** Curated iOS voice allowlist, ordered the way the user wanted them presented.
         *  If the user doesn't have a voice installed, it's silently omitted. */
        val CURATED_VOICE_NAMES: List<String> = listOf(
            "Samantha",
            "Daniel",
            "Karen",
            "Tessa",
            "Moira",
            "Rishi",
            "Melina",
            "Majed",
            "Jester",
            "Whisper",
            "Zarvox",
        )
    }
}
