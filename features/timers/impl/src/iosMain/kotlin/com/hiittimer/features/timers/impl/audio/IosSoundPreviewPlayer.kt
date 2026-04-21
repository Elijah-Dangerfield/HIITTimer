@file:OptIn(ExperimentalForeignApi::class)

package com.dangerfield.hiittimer.features.timers.impl.audio

import com.dangerfield.hiittimer.features.timers.SoundCueType
import com.dangerfield.hiittimer.features.timers.SoundPack
import com.dangerfield.hiittimer.features.timers.SoundPreviewPlayer
import com.dangerfield.hiittimer.libraries.core.logging.KLog
import com.dangerfield.hiittimer.libraries.flowroutines.AppCoroutineScope
import rounds.features.timers.impl.generated.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
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
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosSoundPreviewPlayer(
    private val appScope: AppCoroutineScope,
) : SoundPreviewPlayer {

    private val logger = KLog.withTag("SoundPreview")
    private val players = mutableMapOf<Key, AVAudioPlayer>()
    private var unduckJob: Job? = null

    init {
        appScope.launch(Dispatchers.Main) {
            activateSession()
            SoundPack.entries.forEach { pack ->
                SoundCueType.entries.forEach { cue ->
                    loadPlayer(pack, cue)?.let { players[Key(pack, cue)] = it }
                }
            }
        }
    }

    override fun preview(pack: SoundPack, cueType: SoundCueType, volume: Float) {
        appScope.launch(Dispatchers.Main) {
            val key = Key(pack, cueType)
            val player = players[key] ?: loadPlayer(pack, cueType)?.also {
                players[key] = it
            }
            if (player == null) {
                logger.e { "no player loaded for $pack/$cueType" }
                return@launch
            }
            player.pause()
            player.setCurrentTime(0.0)
            player.setVolume(volume.coerceIn(0f, 1f))
            val durationMs = (player.duration * 1000.0).toLong().coerceAtLeast(0L)
            duckFor(durationMs + BEEP_DUCK_TAIL_MS)
            val started = player.play()
            logger.d { "preview $pack/$cueType vol=$volume started=$started" }
        }
    }

    override fun stop() {
        appScope.launch(Dispatchers.Main) {
            players.values.forEach { it.pause() }
            unduckJob?.cancel()
            unduckJob = null
            configureSession(MIX_WITH_OTHERS)
        }
    }

    private fun activateSession() {
        configureSession(MIX_WITH_OTHERS)
        AVAudioSession.sharedInstance().setActive(true, null)
    }

    private fun configureSession(options: ULong) {
        AVAudioSession.sharedInstance().setCategory(
            AVAudioSessionCategoryPlayback,
            AVAudioSessionModeDefault,
            options,
            null,
        )
    }

    private fun duckFor(durationMs: Long) {
        unduckJob?.cancel()
        configureSession(MIX_WITH_OTHERS or DUCK_OTHERS)
        unduckJob = appScope.launch(Dispatchers.Main) {
            delay(durationMs)
            configureSession(MIX_WITH_OTHERS)
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadPlayer(pack: SoundPack, cueType: SoundCueType): AVAudioPlayer? {
        val prefix = pack.name.lowercase()
        val suffix = when (cueType) {
            SoundCueType.Short -> "short"
            SoundCueType.Long -> "long"
            SoundCueType.Finish -> "finish"
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

    private data class Key(val pack: SoundPack, val cue: SoundCueType)

    companion object {
        private const val MIX_WITH_OTHERS: ULong = 1u
        private const val DUCK_OTHERS: ULong = 2u
        private const val BEEP_DUCK_TAIL_MS: Long = 150
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData? {
    if (isEmpty()) return null
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
