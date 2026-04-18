package com.dangerfield.hiittimer.libraries.core

import com.dangerfield.hiittimer.buildinfo.HIITTimerBuildConfig
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalNativeApi::class)
actual object BuildInfo {
    actual val isDebug: Boolean
        get() = NativePlatform.isDebugBinary

    actual val platform: Platform = Platform.iOS

    actual val applicationId: String
        get() = HIITTimerBuildConfig.APPLICATION_ID

    actual val versionName: String
        get() = HIITTimerBuildConfig.VERSION_NAME

    actual val versionCode: Int
        get() = HIITTimerBuildConfig.VERSION_CODE

    actual val releaseChannel: String
        get() = HIITTimerBuildConfig.RELEASE_CHANNEL

    actual val buildNumber: Int
        get() = HIITTimerBuildConfig.BUILD_NUMBER
}