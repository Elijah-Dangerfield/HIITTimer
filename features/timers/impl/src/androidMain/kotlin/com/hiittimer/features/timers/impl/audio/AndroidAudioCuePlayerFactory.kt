package com.dangerfield.hiittimer.features.timers.impl.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.dangerfield.hiittimer.features.timers.SoundMode
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerCue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.util.Locale

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidAudioCuePlayerFactory @Inject constructor(
    private val context: Context,
) : AudioCuePlayerFactory {
    override fun create(): AudioCuePlayer = AndroidAudioCuePlayer(context)
}

class AndroidAudioCuePlayer(
    private val context: Context,
    private var mode: SoundMode = SoundMode.Beeps,
) : AudioCuePlayer {

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

    private val toneGen: ToneGenerator by lazy {
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneVolume)
    }

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
                    override fun onError(utteranceId: String?) = Unit
                })
                ttsReady = true
            }
        }
    }

    override fun setMode(mode: SoundMode) {
        this.mode = mode
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
        when (cue) {
            is RunnerCue.Countdown -> toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, ToneShortMs)
            is RunnerCue.BlockStart -> toneGen.startTone(ToneGenerator.TONE_PROP_ACK, ToneLongMs)
            is RunnerCue.Halfway -> toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, ToneShortMs)
            RunnerCue.Finish -> toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ToneLongMs)
        }
    }

    private fun playVoice(cue: RunnerCue) {
        val phrase = when (cue) {
            is RunnerCue.Countdown -> cue.remainingSeconds.toString()
            is RunnerCue.BlockStart -> cue.block.name
            is RunnerCue.Halfway -> "Halfway"
            RunnerCue.Finish -> "Done"
        }
        if (ttsReady) {
            val queueMode = when (cue) {
                is RunnerCue.BlockStart -> TextToSpeech.QUEUE_FLUSH
                RunnerCue.Finish -> TextToSpeech.QUEUE_FLUSH
                else -> TextToSpeech.QUEUE_ADD
            }
            tts?.speak(phrase, queueMode, null, phrase)
        } else {
            playBeep(cue)
        }
    }

    override fun release() {
        handler.removeCallbacks(abandonFocus)
        abandonFocus.run()
        runCatching { toneGen.release() }
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    companion object {
        private const val ToneVolume = 80
        private const val ToneShortMs = 150
        private const val ToneLongMs = 300
        private const val FocusHoldShortMs = 700L
        private const val FocusHoldVoiceMs = 2500L
    }
}
