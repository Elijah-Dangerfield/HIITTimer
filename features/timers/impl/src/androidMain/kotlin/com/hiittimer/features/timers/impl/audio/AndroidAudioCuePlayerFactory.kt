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
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import hiittimer.features.timers.impl.generated.resources.Res
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

    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(focusAttributes)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener({ })
                .build()
        } else {
            null
        }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(focusAttributes)
        .build()

    private var mode: SoundMode = SoundMode.Beeps
    private var pack: SoundPack = SoundPack.Classic
    private var volume: Float = 0.8f

    private var shortId: Int = 0
    private var longId: Int = 0
    private var finishId: Int = 0

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    init {
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
            }
        }
        loadPack(pack)
    }

    override fun setMode(mode: SoundMode) {
        this.mode = mode
    }

    override fun setSoundPack(pack: SoundPack) {
        if (this.pack == pack) return
        this.pack = pack
        unloadCurrentPack()
        loadPack(pack)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
        handler.postDelayed(abandonFocus, holdMs)
    }

    override fun play(cue: RunnerCue) {
        when (mode) {
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
        val id = when (cue) {
            is RunnerCue.Countdown -> shortId
            is RunnerCue.Prepare -> if (cue.phase == 1) longId else shortId
            is RunnerCue.BlockStart -> longId
            is RunnerCue.Halfway -> shortId
            RunnerCue.Finish -> finishId
        }
        if (id == 0) return
        soundPool.play(id, volume, volume, /* priority = */ 1, /* loop = */ 0, /* rate = */ 1f)
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

    private fun loadPack(pack: SoundPack) {
        appScope.launch {
            val prefix = pack.name.lowercase()
            val short = loadSound("files/sounds/${prefix}_short.wav")
            val long = loadSound("files/sounds/${prefix}_long.wav")
            val finish = loadSound("files/sounds/${prefix}_finish.wav")
            shortId = short
            longId = long
            finishId = finish
        }
    }

    private suspend fun loadSound(resourcePath: String): Int {
        val bytes = runCatching { Res.readBytes(resourcePath) }
            .onFailure { logger.e(it) { "failed to read $resourcePath" } }
            .getOrNull() ?: return 0
        // SoundPool.load requires a file path. Write to cache once, then load.
        val fileName = resourcePath.substringAfterLast('/')
        val cached = File(context.cacheDir, "cues/$fileName").apply {
            parentFile?.mkdirs()
            if (!exists() || length() != bytes.size.toLong()) {
                writeBytes(bytes)
            }
        }
        return soundPool.load(cached.absolutePath, /* priority = */ 1)
    }

    private fun unloadCurrentPack() {
        if (shortId != 0) soundPool.unload(shortId)
        if (longId != 0) soundPool.unload(longId)
        if (finishId != 0) soundPool.unload(finishId)
        shortId = 0
        longId = 0
        finishId = 0
    }

    override fun release() {
        handler.removeCallbacks(abandonFocus)
        abandonFocus.run()
        runCatching { soundPool.release() }
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    companion object {
        private const val FocusHoldShortMs = 700L
        private const val FocusHoldVoiceMs = 2500L
    }
}
