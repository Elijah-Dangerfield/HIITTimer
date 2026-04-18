package com.dangerfield.hiittimer.features.timers.impl.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
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

    private val toneGen: ToneGenerator by lazy {
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneVolume)
    }

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
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
            SoundMode.Beeps -> playBeep(cue)
            SoundMode.Voice -> playVoice(cue)
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
            tts?.speak(phrase, TextToSpeech.QUEUE_ADD, null, phrase)
        } else {
            playBeep(cue)
        }
    }

    override fun release() {
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
    }
}
