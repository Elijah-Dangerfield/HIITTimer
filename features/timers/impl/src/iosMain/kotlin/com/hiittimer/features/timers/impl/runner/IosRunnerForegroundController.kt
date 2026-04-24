package com.dangerfield.hiittimer.features.timers.impl.runner

import com.dangerfield.hiittimer.libraries.core.logging.KLog
import kotlinx.cinterop.ExperimentalForeignApi
import me.tatarka.inject.annotations.Inject
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.darwin.NSObjectProtocol
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Keeps the audio session alive for the life of a workout by playing a silent
 * PCM buffer on a looped [AVAudioPlayerNode]. Without this, iOS suspends the
 * process a few seconds after backgrounding — which kills timer tick
 * coroutines and the audio cues they fire.
 *
 * Handles interruption and route-change notifications so another app taking
 * audio focus (Music, Spotify, a phone call) doesn't silently stop our engine
 * without us ever restarting it.
 */
@OptIn(ExperimentalForeignApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosRunnerForegroundController @Inject constructor() : RunnerForegroundController {

    private val logger = KLog.withTag("IosRunnerForeground")

    private var engine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var interruptionObserver: NSObjectProtocol? = null

    override fun start() {
        if (engine != null) return

        val session = AVAudioSession.sharedInstance()
        if (!session.setCategory(
                AVAudioSessionCategoryPlayback,
                AVAudioSessionModeDefault,
                MIX_WITH_OTHERS,
                null,
            )
        ) {
            logger.e { scope ->
                scope.tag("audio_event", "foreground_set_category_failed")
                "setCategory returned false"
            }
        }
        if (!session.setActive(true, null)) {
            logger.e { scope ->
                scope.tag("audio_event", "foreground_activate_failed")
                "setActive(true) returned false"
            }
            return
        }

        if (!startEngine()) return
        registerInterruptionObserver()
        logger.i { scope ->
            scope.tag("audio_event", "foreground_started")
            "Silent keep-alive engine started"
        }
    }

    override fun stop() {
        playerNode?.stop()
        engine?.stop()
        playerNode = null
        engine = null
        interruptionObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        interruptionObserver = null
        if (!AVAudioSession.sharedInstance().setActive(false, null)) {
            logger.w { scope ->
                scope.tag("audio_event", "foreground_deactivate_failed")
                "setActive(false) returned false on stop"
            }
        }
    }

    private fun startEngine(): Boolean {
        val format = AVAudioFormat(standardFormatWithSampleRate = 44100.0, channels = 1.toUInt())
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = 44100.toUInt())
        buffer.frameLength = 44100.toUInt()

        val newEngine = AVAudioEngine()
        val node = AVAudioPlayerNode()
        newEngine.attachNode(node)
        newEngine.connect(node1 = node, to = newEngine.mainMixerNode, format = format)
        if (!newEngine.startAndReturnError(null)) {
            logger.e { scope ->
                scope.tag("audio_event", "foreground_engine_start_failed")
                "AVAudioEngine.startAndReturnError returned false"
            }
            return false
        }

        node.scheduleBuffer(
            buffer = buffer,
            atTime = null,
            options = AVAudioPlayerNodeBufferLoops,
            completionHandler = null,
        )
        node.play()

        engine = newEngine
        playerNode = node
        return true
    }

    private fun registerInterruptionObserver() {
        interruptionObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            handleInterruption(notification)
        }
    }

    private fun handleInterruption(notification: NSNotification?) {
        val userInfo = notification?.userInfo ?: return
        val typeValue = (userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber)
            ?.unsignedLongValue ?: return
        when (typeValue) {
            AVAudioSessionInterruptionTypeBegan -> {
                logger.i { scope ->
                    scope.tag("audio_event", "foreground_interruption_began")
                    "Keep-alive engine interrupted"
                }
            }
            AVAudioSessionInterruptionTypeEnded -> {
                val optsValue = (userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber)
                    ?.unsignedLongValue ?: 0UL
                val shouldResume =
                    (optsValue and AVAudioSessionInterruptionOptionShouldResume) != 0UL
                logger.i { scope ->
                    scope.tag("audio_event", "foreground_interruption_ended")
                    scope.extra("should_resume", shouldResume.toString())
                    "Keep-alive engine interruption ended"
                }
                if (shouldResume && engine != null) {
                    restartEngineAfterInterruption()
                }
            }
            else -> Unit
        }
    }

    private fun restartEngineAfterInterruption() {
        val session = AVAudioSession.sharedInstance()
        if (!session.setActive(true, null)) {
            logger.e { scope ->
                scope.tag("audio_event", "foreground_reactivate_failed")
                "setActive(true) returned false after interruption"
            }
            return
        }
        val existingEngine = engine
        if (existingEngine != null && !existingEngine.running) {
            if (!existingEngine.startAndReturnError(null)) {
                logger.e { scope ->
                    scope.tag("audio_event", "foreground_engine_restart_failed")
                    "AVAudioEngine.startAndReturnError returned false on resume"
                }
                return
            }
            playerNode?.play()
            logger.i { scope ->
                scope.tag("audio_event", "foreground_resumed")
                "Keep-alive engine restarted after interruption"
            }
        }
    }

    private companion object {
        const val MIX_WITH_OTHERS: ULong = 1u
    }
}
