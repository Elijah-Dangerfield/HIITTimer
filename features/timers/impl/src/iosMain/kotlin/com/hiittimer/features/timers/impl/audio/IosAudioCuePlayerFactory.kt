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
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonCategoryChange
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.darwin.NSObjectProtocol
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

    // Single source of truth for whether iOS currently considers our session
    // active. Flipped false on interruption-began and any setActive/setCategory
    // that returns false; flipped true on successful activate/reactivate.
    // Plain Boolean: K/N 2.x's relaxed memory model handles the cross-thread
    // reads/writes here safely, and a rare stale read only costs an extra
    // no-op reactivate call.
    private var sessionActive: Boolean = false

    private var interruptionObserver: NSObjectProtocol? = null
    private var routeChangeObserver: NSObjectProtocol? = null

    init {
        registerAudioLifecycleObservers()
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
                startBeep(loaded, cue)
            }
            return
        }
        startBeep(player, cue)
    }

    private fun startBeep(player: AVAudioPlayer, cue: RunnerCue) {
        val beepMs = (player.duration * 1000.0).toLong().coerceAtLeast(0L)
        duckFor(beepMs + BEEP_DUCK_TAIL_MS)
        startPlayer(player, cue)
    }

    private fun startPlayer(player: AVAudioPlayer, cue: RunnerCue) {
        // If another app started audio while we were idle the session can be
        // implicitly deactivated and player.play() becomes a silent no-op.
        // Opportunistic reactivate here keeps the next cue audible.
        if (!sessionActive) {
            appScope.launch(Dispatchers.Main) {
                reactivateSession(reason = "pre_play")
            }
        }
        player.pause()
        player.setCurrentTime(0.0)
        player.setVolume(volume)
        val started = player.play()
        if (!started) {
            logger.e { scope ->
                scope.tag("audio_event", "play_failed")
                scope.extra("cue", cue::class.simpleName ?: "unknown")
                scope.extra("session_active", sessionActive)
                scope.extra("duration_s", player.duration)
                "AVAudioPlayer.play() returned false"
            }
        } else {
            logger.d { scope ->
                scope.tag("audio_event", "play_started")
                scope.extra("cue", cue::class.simpleName ?: "unknown")
                "play() returned true"
            }
        }
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

        if (!sessionActive) {
            appScope.launch(Dispatchers.Main) {
                reactivateSession(reason = "pre_voice")
            }
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
            val ok = AVAudioSession.sharedInstance().setActive(true, null)
            sessionActive = ok
            if (!ok) {
                logger.e { scope ->
                    scope.tag("audio_event", "activate_failed")
                    "Initial setActive(true) returned false"
                }
            } else {
                logger.i { scope ->
                    scope.tag("audio_event", "session_activated")
                    "Audio session active"
                }
            }
        }
    }

    private suspend fun reactivateSession(reason: String) {
        withContext(sessionDispatcher) {
            val ok = AVAudioSession.sharedInstance().setActive(true, null)
            sessionActive = ok
            if (!ok) {
                logger.e { scope ->
                    scope.tag("audio_event", "reactivate_failed")
                    scope.extra("reason", reason)
                    "setActive(true) returned false during reactivation"
                }
            } else {
                logger.i { scope ->
                    scope.tag("audio_event", "session_reactivated")
                    scope.extra("reason", reason)
                    "Session reactivated"
                }
            }
        }
    }

    private suspend fun configureSession(options: ULong) {
        withContext(sessionDispatcher) {
            val ok = AVAudioSession.sharedInstance().setCategory(
                AVAudioSessionCategoryPlayback,
                AVAudioSessionModeDefault,
                options,
                null,
            )
            if (!ok) {
                sessionActive = false
                logger.e { scope ->
                    scope.tag("audio_event", "set_category_failed")
                    scope.extra("options", options.toString())
                    "setCategory returned false"
                }
            }
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

    private fun registerAudioLifecycleObservers() {
        val center = NSNotificationCenter.defaultCenter
        interruptionObserver = center.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            handleInterruption(notification)
        }
        routeChangeObserver = center.addObserverForName(
            name = AVAudioSessionRouteChangeNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            handleRouteChange(notification)
        }
    }

    private fun handleInterruption(notification: NSNotification?) {
        val userInfo = notification?.userInfo ?: return
        val typeValue = (userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedLongValue
            ?: return
        when (typeValue) {
            AVAudioSessionInterruptionTypeBegan -> {
                sessionActive = false
                logger.i { scope ->
                    scope.tag("audio_event", "interruption_began")
                    "Audio session interrupted"
                }
            }
            AVAudioSessionInterruptionTypeEnded -> {
                val optsValue = (userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber)
                    ?.unsignedLongValue ?: 0UL
                val shouldResume =
                    (optsValue and AVAudioSessionInterruptionOptionShouldResume) != 0UL
                logger.i { scope ->
                    scope.tag("audio_event", "interruption_ended")
                    scope.extra("should_resume", shouldResume.toString())
                    "Audio session interruption ended"
                }
                if (shouldResume) {
                    appScope.launch(Dispatchers.Main) {
                        reactivateSession(reason = "interruption_ended")
                    }
                }
            }
            else -> Unit
        }
    }

    private fun handleRouteChange(notification: NSNotification?) {
        val userInfo = notification?.userInfo ?: return
        val reason = (userInfo[AVAudioSessionRouteChangeReasonKey] as? NSNumber)
            ?.unsignedLongValue ?: return
        logger.i { scope ->
            scope.tag("audio_event", "route_changed")
            scope.extra("reason", reason.toString())
            "Audio route changed"
        }
        if (reason == AVAudioSessionRouteChangeReasonCategoryChange ||
            reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable
        ) {
            appScope.launch(Dispatchers.Main) {
                reactivateSession(reason = "route_change_$reason")
            }
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
        val center = NSNotificationCenter.defaultCenter
        interruptionObserver?.let { center.removeObserver(it) }
        routeChangeObserver?.let { center.removeObserver(it) }
        interruptionObserver = null
        routeChangeObserver = null
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
