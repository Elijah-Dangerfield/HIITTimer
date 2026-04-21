package com.dangerfield.hiittimer.features.timers.impl.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.dangerfield.hiittimer.features.timers.SoundCueType
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.SoundPreviewPlayer
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

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidSoundPreviewPlayer(
    private val context: Context,
    private val appScope: AppCoroutineScope,
) : SoundPreviewPlayer {

    private val logger = KLog.withTag("SoundPreview")

    private val focusAttributes: AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(focusAttributes)
        .build()

    private val loadedIds = mutableSetOf<Int>()
    private var pendingPlay: PendingPlay? = null

    private var loadedPack: SoundPack? = null
    private var shortId: Int = 0
    private var longId: Int = 0
    private var finishId: Int = 0
    private var currentStreamId: Int = 0

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                logger.e { "SoundPool failed to load sampleId=$sampleId status=$status" }
                return@setOnLoadCompleteListener
            }
            loadedIds += sampleId
            val pending = pendingPlay
            if (pending != null && pending.sampleId == sampleId) {
                currentStreamId = soundPool.play(sampleId, pending.volume, pending.volume, 1, 0, 1f)
                pendingPlay = null
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    override fun preview(pack: SoundPack, cueType: SoundCueType, volume: Float) {
        if (loadedPack != pack) {
            unloadCurrent()
            loadedPack = pack
            appScope.launch {
                val prefix = pack.name.lowercase()
                shortId = loadSound("files/sounds/${prefix}_short.wav")
                longId = loadSound("files/sounds/${prefix}_long.wav")
                finishId = loadSound("files/sounds/${prefix}_finish.wav")
                playOrPend(cueType, volume)
            }
        } else {
            playOrPend(cueType, volume)
        }
    }

    private fun playOrPend(cueType: SoundCueType, volume: Float) {
        val id = when (cueType) {
            SoundCueType.Short -> shortId
            SoundCueType.Long -> longId
            SoundCueType.Finish -> finishId
        }
        if (id == 0) return
        stop()
        val clampedVolume = volume.coerceIn(0f, 1f)
        if (id in loadedIds) {
            currentStreamId = soundPool.play(id, clampedVolume, clampedVolume, 1, 0, 1f)
        } else {
            pendingPlay = PendingPlay(sampleId = id, volume = clampedVolume)
        }
    }

    override fun stop() {
        pendingPlay = null
        if (currentStreamId != 0) {
            soundPool.stop(currentStreamId)
            currentStreamId = 0
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadSound(resourcePath: String): Int {
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

    private fun unloadCurrent() {
        loadedIds.clear()
        pendingPlay = null
        if (shortId != 0) soundPool.unload(shortId)
        if (longId != 0) soundPool.unload(longId)
        if (finishId != 0) soundPool.unload(finishId)
        shortId = 0
        longId = 0
        finishId = 0
    }

    private data class PendingPlay(val sampleId: Int, val volume: Float)
}
