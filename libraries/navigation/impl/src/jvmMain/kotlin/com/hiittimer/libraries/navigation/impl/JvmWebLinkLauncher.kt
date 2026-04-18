package com.dangerfield.hiittimer.libraries.navigation.impl

import com.dangerfield.hiittimer.libraries.core.Catching
import com.dangerfield.hiittimer.libraries.core.failure
import com.dangerfield.hiittimer.libraries.navigation.WebLinkLauncher
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class JvmWebLinkLauncher @Inject constructor() : WebLinkLauncher {

    override fun open(url: String): Catching<Unit> = failure(
        UnsupportedOperationException("Opening web links is not supported on JVM target: $url")
    )
}
