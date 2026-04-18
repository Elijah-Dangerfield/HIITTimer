package com.dangerfield.hiittimer

import com.dangerfield.hiittimer.libraries.hiittimer.impl.AppEventDispatcher
import com.dangerfield.hiittimer.libraries.navigation.impl.DelegatingRouter
import com.dangerfield.hiittimer.libraries.hiittimer.Telemetry
import com.dangerfield.hiittimer.libraries.navigation.FeatureEntryPoint
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock

@ContributesTo(AppScope::class)
@SingleIn(AppScope::class)
interface AppComponent {
    val featureEntryPoints: Set<FeatureEntryPoint>
    val appViewModel: AppViewModel  // Singleton, shared between MainActivity and App
    val delegatingRouter: DelegatingRouter
    val telemetry: Telemetry
    val shakeHandler: ShakeHandler
    
    /**
     * Eagerly initialized to start observing app lifecycle events.
     * This ensures sessions are created on foreground entry.
     */
    val appEventDispatcher: AppEventDispatcher

    @Provides
    fun provideClock(): Clock = Clock.System

}
