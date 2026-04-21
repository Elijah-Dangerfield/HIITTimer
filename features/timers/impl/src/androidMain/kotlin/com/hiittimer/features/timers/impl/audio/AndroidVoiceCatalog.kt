package com.dangerfield.hiittimer.features.timers.impl.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.dangerfield.hiittimer.features.timers.VoiceCatalog
import com.dangerfield.hiittimer.features.timers.VoiceOption
import kotlinx.coroutines.CompletableDeferred
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.util.Locale

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidVoiceCatalog(
    private val context: Context,
) : VoiceCatalog {

    private val readyDeferred = CompletableDeferred<Boolean>()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        readyDeferred.complete(status == TextToSpeech.SUCCESS)
    }

    override suspend fun availableVoices(): List<VoiceOption> {
        val ready = readyDeferred.await()
        if (!ready) return listOf(SYSTEM_DEFAULT)
        val voices = tts.voices ?: return listOf(SYSTEM_DEFAULT)
        val installed = voices
            .filter { !it.isNetworkConnectionRequired && it.features?.contains(Voice.QUALITY_VERY_LOW.toString()) != true }
            .map {
                VoiceOption(
                    id = it.name,
                    displayName = prettify(it),
                    languageTag = it.locale.toLanguageTag(),
                )
            }
            .sortedWith(compareBy({ it.languageTag }, { it.displayName }))
        return listOf(SYSTEM_DEFAULT) + installed
    }

    override fun preview(voiceId: String?, phrase: String, volume: Float) {
        tts.stop()
        val id = voiceId?.takeIf { it.isNotBlank() }
        if (id != null) {
            tts.voices?.firstOrNull { it.name == id }?.let { tts.voice = it }
        } else {
            tts.language = Locale.getDefault()
        }
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
        }
        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, params, "voice-preview")
    }

    override fun stopPreview() {
        tts.stop()
    }

    private fun prettify(voice: Voice): String {
        val locale = voice.locale
        val lang = locale.getDisplayLanguage(Locale.getDefault())
        val country = locale.getDisplayCountry(Locale.getDefault()).takeIf { it.isNotBlank() }
        val variant = voice.name.substringAfterLast('-').takeIf { it.isNotBlank() && it != voice.name }
        val label = buildString {
            append(lang)
            if (country != null) append(" ($country)")
            if (variant != null) append(" · ").append(variant)
        }
        return label
    }

    companion object {
        private val SYSTEM_DEFAULT = VoiceOption(
            id = "",
            displayName = "System default",
            languageTag = Locale.getDefault().toLanguageTag(),
            isDefault = true,
        )
    }
}
