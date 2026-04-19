package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
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
import platform.AudioToolbox.AudioServicesPlaySystemSound
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosAudioCuePlayerFactory @Inject constructor() : AudioCuePlayerFactory {
    override fun create(): AudioCuePlayer = IosAudioCuePlayer()
}

@OptIn(ExperimentalForeignApi::class)
class IosAudioCuePlayer(
    private var mode: SoundMode = SoundMode.Beeps,
) : AudioCuePlayer {

    private val synthesizer: AVSpeechSynthesizer by lazy { AVSpeechSynthesizer() }

    init {
        setupAudioSession()
    }

    private fun setupAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayback,
            AVAudioSessionModeDefault,
            MIX_WITH_OTHERS or DUCK_OTHERS,
            null,
        )
        session.setActive(true, null)
    }

    override fun setMode(mode: SoundMode) {
        this.mode = mode
    }

    override fun play(cue: RunnerCue) {
        when (mode) {
            SoundMode.Off -> Unit
            SoundMode.Beeps -> playBeep(cue)
            SoundMode.Voice -> playVoice(cue)
        }
    }

    private fun playBeep(cue: RunnerCue) {
        when (cue) {
            is RunnerCue.Countdown -> playSystemSound(SHORT_BEEP)
            is RunnerCue.BlockStart -> playSystemSound(LONG_BEEP)
            is RunnerCue.Halfway -> playSystemSound(SHORT_BEEP)
            RunnerCue.Finish -> playSystemSound(FINISH_SOUND)
        }
    }

    private fun playSystemSound(soundId: UInt) {
        AudioServicesPlaySystemSound(soundId)
    }

    private fun playVoice(cue: RunnerCue) {
        val phrase = when (cue) {
            is RunnerCue.Countdown -> cue.remainingSeconds.toString()
            is RunnerCue.BlockStart -> cue.block.name
            is RunnerCue.Halfway -> "Halfway"
            RunnerCue.Finish -> "Done"
        }

        val shouldFlush = when (cue) {
            is RunnerCue.BlockStart, RunnerCue.Finish -> true
            else -> false
        }

        if (shouldFlush) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }

        val utterance = AVSpeechUtterance.speechUtteranceWithString(phrase)
        utterance.rate = 0.52f
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        synthesizer.speakUtterance(utterance)
    }

    override fun release() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    companion object {
        private const val SHORT_BEEP: UInt = 1057u
        private const val LONG_BEEP: UInt = 1052u
        private const val FINISH_SOUND: UInt = 1025u

        private const val MIX_WITH_OTHERS: UInt = 1u
        private const val DUCK_OTHERS: UInt = 2u
    }
}
