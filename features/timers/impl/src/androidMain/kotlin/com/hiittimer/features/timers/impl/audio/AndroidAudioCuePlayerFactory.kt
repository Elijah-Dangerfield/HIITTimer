package com.dangerfield.hiittimer.features.timers.impl.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.dangerfield.hiittimer.features.timers.CueRole
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import rounds.features.timers.impl.generated.resources.Res
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.io.File
import java.util.Locale

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidAudioCuePlayerFactory @Inject constructor(
    private val context: Context,
    private val appScope: AppCoroutineScope,
) : AudioCuePlayerFactory {
    override fun create(): AudioCuePlayer = AndroidAudioCuePlayer(context, appScope)
}

@OptIn(ExperimentalResourceApi::class)
private class AndroidAudioCuePlayer(
    private val context: Context,
    private val appScope: AppCoroutineScope,
) : AudioCuePlayer {

    private val logger = KLog.withTag("AndroidAudioCuePlayer")
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private val focusAttributes: AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    // Tracks the most recent focus state the system told us about. Used by
    // the telemetry in play() so the Sentry breadcrumb on a silent-play
    // failure shows whether focus was already lost before we tried.
    @Volatile
    private var lastKnownFocusChange: Int = AudioManager.AUDIOFOCUS_GAIN

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        lastKnownFocusChange = change
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> logger.i { scope ->
                scope.tag("audio_event", "focus_gain")
                "Audio focus regained"
            }
            AudioManager.AUDIOFOCUS_LOSS -> logger.w { scope ->
                scope.tag("audio_event", "focus_loss_permanent")
                "Audio focus lost permanently — next cue will be silent until reacquired"
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> logger.i { scope ->
                scope.tag("audio_event", "focus_loss_transient")
                "Audio focus lost transiently"
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> logger.i { scope ->
                scope.tag("audio_event", "focus_loss_transient_duck")
                "Audio focus lost but can duck"
            }
            else -> logger.i { scope ->
                scope.tag("audio_event", "focus_change_unknown")
                scope.extra("change", change)
                "Audio focus change with unknown code"
            }
        }
    }

    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(focusAttributes)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
        } else {
            null
        }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(focusAttributes)
        .build()

    private var masterEnabled: Boolean = true
    private val modes: MutableMap<CueRole, SoundMode> =
        CueRole.entries.associateWith { SoundMode.Beeps }.toMutableMap()
    private var volume: Float = 0.8f
    private var voiceId: String? = null
    private val cuePacks: MutableMap<CueRole, SoundPack> =
        CueRole.entries.associateWith { SoundPack.Classic }.toMutableMap()

    private val sampleIds: MutableMap<Key, Int> = mutableMapOf()
    private val loadedIds: MutableSet<Int> = mutableSetOf()
    private var pendingPlay: Int? = null

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                logger.e { "SoundPool load failed sampleId=$sampleId status=$status" }
                return@setOnLoadCompleteListener
            }
            loadedIds += sampleId
            val pending = pendingPlay
            if (pending != null && pending == sampleId) {
                soundPool.play(sampleId, volume, volume, 1, 0, 1f)
                pendingPlay = null
            }
        }

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(1.1f)
                tts?.setAudioAttributes(focusAttributes)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = Unit
                    @Deprecated("deprecated in Android T; kept to satisfy abstract on older SDKs")
                    override fun onError(utteranceId: String?) = Unit
                    override fun onError(utteranceId: String?, errorCode: Int) = Unit
                })
                ttsReady = true
                applyVoice()
            }
        }

        appScope.launch {
            SoundPack.entries.forEach { pack ->
                CueSample.entries.forEach { sample ->
                    val id = loadSound(pack, sample)
                    if (id != 0) sampleIds[Key(pack, sample)] = id
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
        applyVoice()
    }

    private fun applyVoice() {
        val t = tts ?: return
        val id = voiceId
        val voice = id?.let { vid -> t.voices?.firstOrNull { it.name == vid } }
        if (voice != null) t.voice = voice
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }

    private val abandonFocus = Runnable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun requestDucking(holdMs: Long) {
        handler.removeCallbacks(abandonFocus)
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            logger.e { scope ->
                scope.tag("audio_event", "focus_request_failed")
                scope.extra("result", result)
                scope.extra("last_focus_change", lastKnownFocusChange)
                "requestAudioFocus did not return GRANTED (likely silent cue)"
            }
        }
        handler.postDelayed(abandonFocus, holdMs)
    }

    override fun play(cue: RunnerCue) {
        if (!masterEnabled) return
        val role = cue.route().role
        when (modes[role] ?: SoundMode.Beeps) {
            SoundMode.Off -> Unit
            SoundMode.Beeps -> {
                requestDucking(FocusHoldShortMs)
                playBeep(cue)
            }
            SoundMode.Voice -> {
                requestDucking(FocusHoldVoiceMs)
                playVoice(cue)
            }
        }
    }

    private fun playBeep(cue: RunnerCue) {
        val routing = cue.route()
        val pack = cuePacks[routing.role] ?: SoundPack.Classic
        val id = sampleIds[Key(pack, routing.sample)]
        if (id == null) {
            appScope.launch {
                val loaded = loadSound(pack, routing.sample)
                if (loaded != 0) sampleIds[Key(pack, routing.sample)] = loaded
            }
            return
        }
        if (id in loadedIds) {
            val streamId = soundPool.play(id, volume, volume, 1, 0, 1f)
            if (streamId == 0) {
                logger.e { scope ->
                    scope.tag("audio_event", "sound_pool_play_failed")
                    scope.extra("cue", cue::class.simpleName ?: "unknown")
                    scope.extra("last_focus_change", lastKnownFocusChange)
                    "SoundPool.play returned 0 (silent cue)"
                }
            }
        } else {
            pendingPlay = id
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
        if (!ttsReady) {
            playBeep(cue)
            return
        }
        val queueMode = when (cue) {
            is RunnerCue.BlockStart -> TextToSpeech.QUEUE_FLUSH
            RunnerCue.Finish -> TextToSpeech.QUEUE_FLUSH
            is RunnerCue.Prepare -> if (cue.phase == 3) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            else -> TextToSpeech.QUEUE_ADD
        }
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(phrase, queueMode, params, phrase)
    }

    private suspend fun loadSound(pack: SoundPack, sample: CueSample): Int {
        val prefix = pack.name.lowercase()
        val suffix = when (sample) {
            CueSample.Short -> "short"
            CueSample.Long -> "long"
            CueSample.Finish -> "finish"
        }
        val resourcePath = "files/sounds/${prefix}_$suffix.wav"
        val bytes = runCatching { Res.readBytes(resourcePath) }
            .onFailure { logger.e(it) { "failed to read $resourcePath" } }
            .getOrNull() ?: return 0
        val fileName = resourcePath.substringAfterLast('/')
        val cached = File(context.cacheDir, "cues/$fileName").apply {
            parentFile?.mkdirs()
            if (!exists() || length() != bytes.size.toLong()) {
                writeBytes(bytes)
            }
        }
        return soundPool.load(cached.absolutePath, 1)
    }

    override fun release() {
        handler.removeCallbacks(abandonFocus)
        abandonFocus.run()
        pendingPlay = null
        runCatching { soundPool.release() }
        sampleIds.clear()
        loadedIds.clear()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private data class Key(val pack: SoundPack, val sample: CueSample)

    companion object {
        private const val FocusHoldShortMs = 700L
        private const val FocusHoldVoiceMs = 2500L
    }
}
