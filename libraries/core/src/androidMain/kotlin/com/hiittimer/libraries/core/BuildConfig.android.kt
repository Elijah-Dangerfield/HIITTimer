package com.dangerfield.hiittimer.libraries.core

import com.dangerfield.hiittimer.buildinfo.HIITTimerBuildConfig
import com.dangerfield.hiittimer.libraries.core.BuildConfig as AndroidBuildConfig

actual object BuildInfo {
    actual val isDebug: Boolean
        get() = AndroidBuildConfig.DEBUG

    actual val platform: Platform
        get() = Platform.Android

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