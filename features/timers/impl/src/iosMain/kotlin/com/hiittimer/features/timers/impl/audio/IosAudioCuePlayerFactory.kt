package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import hiittimer.features.timers.impl.generated.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosAudioCuePlayerFactory @Inject constructor(
    private val appScope: AppCoroutineScope,
) : AudioCuePlayerFactory {
    override fun create(): AudioCuePlayer = IosAudioCuePlayer(appScope)
}

@OptIn(ExperimentalForeignApi::class, ExperimentalResourceApi::class)
private class IosAudioCuePlayer(
    private val appScope: AppCoroutineScope,
) : AudioCuePlayer {

    private val logger = KLog.withTag("IosAudioCuePlayer")
    private val synthesizer: AVSpeechSynthesizer by lazy { AVSpeechSynthesizer() }

    private var mode: SoundMode = SoundMode.Beeps
    private var pack: SoundPack = SoundPack.Classic
    private var volume: Float = 0.8f

    private var loadedPack: SoundPack? = null
    private var shortPlayer: AVAudioPlayer? = null
    private var longPlayer: AVAudioPlayer? = null
    private var finishPlayer: AVAudioPlayer? = null

    init {
        setupAudioSession()
        loadPack(pack)
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

    override fun setSoundPack(pack: SoundPack) {
        if (this.pack == pack && loadedPack == pack) return
        this.pack = pack
        loadPack(pack)
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        this.volume = clamped
        shortPlayer?.setVolume(clamped)
        longPlayer?.setVolume(clamped)
        finishPlayer?.setVolume(clamped)
    }

    override fun play(cue: RunnerCue) {
        when (mode) {
            SoundMode.Off -> Unit
            SoundMode.Beeps -> playBeep(cue)
            SoundMode.Voice -> playVoice(cue)
        }
    }

    private fun playBeep(cue: RunnerCue) {
        val player = when (cue) {
            is RunnerCue.Countdown -> shortPlayer
            is RunnerCue.Prepare -> if (cue.phase == 1) longPlayer else shortPlayer
            is RunnerCue.BlockStart -> longPlayer
            is RunnerCue.Halfway -> shortPlayer
            RunnerCue.Finish -> finishPlayer
        } ?: return
        player.pause()
        player.setCurrentTime(0.0)
        player.setVolume(volume)
        player.play()
    }

    private fun playVoice(cue: RunnerCue) {
        val phrase = when (cue) {
            is RunnerCue.Countdown -> cue.remainingSeconds.toString()
            is RunnerCue.Prepare -> when (cue.phase) {
                3 -> "Get ready"
                2 -> "Get set"
                1 -> "Go"
                else -> cue.phase.toString()
            }
            is RunnerCue.BlockStart -> cue.block.name
            is RunnerCue.Halfway -> "Halfway"
            RunnerCue.Finish -> "Done"
        }

        val shouldFlush = when (cue) {
            is RunnerCue.BlockStart, RunnerCue.Finish -> true
            is RunnerCue.Prepare -> cue.phase == 3
            else -> false
        }

        if (shouldFlush) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }

        val utterance = AVSpeechUtterance.speechUtteranceWithString(phrase)
        utterance.rate = 0.52f
        utterance.volume = volume
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        synthesizer.speakUtterance(utterance)
    }

    private fun loadPack(pack: SoundPack) {
        appScope.launch {
            val prefix = pack.name.lowercase()
            val short = loadPlayer("files/sounds/${prefix}_short.wav")
            val long = loadPlayer("files/sounds/${prefix}_long.wav")
            val finish = loadPlayer("files/sounds/${prefix}_finish.wav")
            if (short != null) short.setVolume(volume)
            if (long != null) long.setVolume(volume)
            if (finish != null) finish.setVolume(volume)
            shortPlayer = short
            longPlayer = long
            finishPlayer = finish
            loadedPack = pack
        }
    }

    private suspend fun loadPlayer(resourcePath: String): AVAudioPlayer? {
        val bytes = runCatching { Res.readBytes(resourcePath) }
            .onFailure { logger.e(it) { "failed to read $resourcePath" } }
            .getOrNull() ?: return null
        val data = bytes.toNSData() ?: return null
        val player = AVAudioPlayer(data = data, error = null)
        player.prepareToPlay()
        return player
    }

    override fun release() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        shortPlayer?.stop()
        longPlayer?.stop()
        finishPlayer?.stop()
        shortPlayer = null
        longPlayer = null
        finishPlayer = null
    }

    companion object {
        private const val MIX_WITH_OTHERS: ULong = 1u
        private const val DUCK_OTHERS: ULong = 2u
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData? {
    if (isEmpty()) return null
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
