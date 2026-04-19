package com.dangerfield.hiittimer.features.timers.impl.runner

import kotlinx.cinterop.ExperimentalForeignApi
import me.tatarka.inject.annotations.Inject
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@OptIn(ExperimentalForeignApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosRunnerForegroundController @Inject constructor() : RunnerForegroundController {

    private var engine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null

    override fun start() {
        if (engine != null) return

        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayback,
            AVAudioSessionModeDefault,
            MIX_WITH_OTHERS or DUCK_OTHERS,
            null,
        )
        session.setActive(true, null)

        val format = AVAudioFormat(standardFormatWithSampleRate = 44100.0, channels = 1.toUInt())
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = 44100.toUInt())
        buffer.frameLength = 44100.toUInt()

        val newEngine = AVAudioEngine()
        val node = AVAudioPlayerNode()
        newEngine.attachNode(node)
        newEngine.connect(node1 = node, to = newEngine.mainMixerNode, format = format)
        if (!newEngine.startAndReturnError(null)) return

        node.scheduleBuffer(
            buffer = buffer,
            atTime = null,
            options = AVAudioPlayerNodeBufferLoops,
            completionHandler = null,
        )
        node.play()

        engine = newEngine
        playerNode = node
    }

    override fun stop() {
        playerNode?.stop()
        engine?.stop()
        playerNode = null
        engine = null
        AVAudioSession.sharedInstance().setActive(false, null)
    }

    private companion object {
        const val MIX_WITH_OTHERS: ULong = 1u
        const val DUCK_OTHERS: ULong = 2u
    }
}
