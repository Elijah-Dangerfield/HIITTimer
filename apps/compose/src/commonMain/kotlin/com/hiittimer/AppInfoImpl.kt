package com.dangerfield.hiittimer

import com.dangerfield.hiittimer.appconfig.AppBuildConfig
import com.dangerfield.hiittimer.libraries.hiittimer.AppInfo
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AppInfoImpl : AppInfo {
    override val versionName: String = AppBuildConfig.VERSION_NAME
    override val versionCode: Int = AppBuildConfig.VERSION_CODE
    override val buildNumber: Int = AppBuildConfig.BUILD_NUMBER
}
