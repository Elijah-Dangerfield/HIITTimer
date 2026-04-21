package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.CueRole
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import rounds.features.timers.impl.generated.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

@OptIn(ExperimentalForeignApi::class, ExperimentalResourceApi::class, ExperimentalCoroutinesApi::class)
private class IosAudioCuePlayer(
    private val appScope: AppCoroutineScope,
) : AudioCuePlayer {

    private val logger = KLog.withTag("IosAudioCuePlayer")
    private val synthesizer: AVSpeechSynthesizer by lazy { AVSpeechSynthesizer() }
    private val sessionDispatcher = Dispatchers.Default.limitedParallelism(1)

    private var masterEnabled: Boolean = true
    private val modes: MutableMap<CueRole, SoundMode> =
        CueRole.entries.associateWith { SoundMode.Beeps }.toMutableMap()
    private var volume: Float = 0.8f
    private var voiceId: String? = null
    private val cuePacks: MutableMap<CueRole, SoundPack> =
        CueRole.entries.associateWith { SoundPack.Classic }.toMutableMap()
    private val players: MutableMap<Key, AVAudioPlayer> = mutableMapOf()
    private var unduckJob: Job? = null

    init {
        appScope.launch(Dispatchers.Main) {
            activateSession()
            SoundPack.entries.forEach { pack ->
                CueSample.entries.forEach { sample ->
                    loadPlayer(pack, sample)?.let {
                        it.setVolume(volume)
                        players[Key(pack, sample)] = it
                    }
                }
            }
        }
    }

    override fun setMasterEnabled(enabled: Boolean) {
        this.masterEnabled = enabled
    }

    override fun setMode(role: CueRole, mode: SoundMode) {
        modes[role] = mode
    }

    override fun setCuePack(role: CueRole, pack: SoundPack) {
        cuePacks[role] = pack
    }

    override fun setVoice(voiceId: String?) {
        this.voiceId = voiceId?.takeIf { it.isNotBlank() }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        this.volume = clamped
        players.values.forEach { it.setVolume(clamped) }
    }

    override fun play(cue: RunnerCue) {
        if (!masterEnabled) return
        val role = cue.route().role
        when (modes[role] ?: SoundMode.Beeps) {
            SoundMode.Off -> Unit
            SoundMode.Beeps -> playBeep(cue)
            SoundMode.Voice -> playVoice(cue)
        }
    }

    private fun playBeep(cue: RunnerCue) {
        val routing = cue.route()
        val pack = cuePacks[routing.role] ?: SoundPack.Classic
        val player = players[Key(pack, routing.sample)]
        if (player == null) {
            appScope.launch(Dispatchers.Main) {
                val loaded = loadPlayer(pack, routing.sample) ?: return@launch
                loaded.setVolume(volume)
                players[Key(pack, routing.sample)] = loaded
                startBeep(loaded)
            }
            return
        }
        startBeep(player)
    }

    private fun startBeep(player: AVAudioPlayer) {
        val beepMs = (player.duration * 1000.0).toLong().coerceAtLeast(0L)
        duckFor(beepMs + BEEP_DUCK_TAIL_MS)
        startPlayer(player)
    }

    private fun startPlayer(player: AVAudioPlayer) {
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
        utterance.voice = voiceId?.let { AVSpeechSynthesisVoice.voiceWithIdentifier(it) }
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        duckFor(VOICE_DUCK_MS)
        synthesizer.speakUtterance(utterance)
    }

    private suspend fun activateSession() {
        configureSession(MIX_WITH_OTHERS)
        withContext(sessionDispatcher) {
            AVAudioSession.sharedInstance().setActive(true, null)
        }
    }

    private suspend fun configureSession(options: ULong) {
        withContext(sessionDispatcher) {
            AVAudioSession.sharedInstance().setCategory(
                AVAudioSessionCategoryPlayback,
                AVAudioSessionModeDefault,
                options,
                null,
            )
        }
    }

    private fun duckFor(durationMs: Long) {
        unduckJob?.cancel()
        unduckJob = appScope.launch(Dispatchers.Main) {
            configureSession(MIX_WITH_OTHERS or DUCK_OTHERS)
            delay(durationMs)
            configureSession(MIX_WITH_OTHERS)
        }
    }

    private suspend fun loadPlayer(pack: SoundPack, sample: CueSample): AVAudioPlayer? {
        val prefix = pack.name.lowercase()
        val suffix = when (sample) {
            CueSample.Short -> "short"
            CueSample.Long -> "long"
            CueSample.Finish -> "finish"
        }
        val path = "files/sounds/${prefix}_$suffix.wav"
        val bytes = runCatching { withContext(Dispatchers.Default) { Res.readBytes(path) } }
            .onFailure { logger.e(it) { "failed to read $path" } }
            .getOrNull() ?: return null
        val data = bytes.toNSData() ?: return null
        val player = AVAudioPlayer(data = data, error = null)
        player.prepareToPlay()
        return player
    }

    override fun release() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        unduckJob?.cancel()
        unduckJob = null
        players.values.forEach { it.stop() }
        players.clear()
        appScope.launch(Dispatchers.Main) { configureSession(MIX_WITH_OTHERS) }
    }

    private data class Key(val pack: SoundPack, val sample: CueSample)

    companion object {
        private const val MIX_WITH_OTHERS: ULong = 1u
        private const val DUCK_OTHERS: ULong = 2u
        private const val BEEP_DUCK_TAIL_MS: Long = 1300
        private const val VOICE_DUCK_MS: Long = 3500
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData? {
    if (isEmpty()) return null
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
