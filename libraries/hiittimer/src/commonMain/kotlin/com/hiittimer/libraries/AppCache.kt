package com.dangerfield.hiittimer.libraries.hiittimer

import com.dangerfield.hiittimer.libraries.storage.Cache
import com.dangerfield.hiittimer.libraries.storage.CacheFactory
import com.dangerfield.hiittimer.libraries.storage.versionedJsonSerializer
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * In-memory + persistent cache for app-wide state that doesn't need to be in the database.
 */
@Serializable
data class AppData(
    // Onboarding
    val hasUserOnboarded: Boolean = false,
    
    // Screen visits - automatically tracked for any TrackableRoute
    val screenVisits: Map<String, Int> = emptyMap(),
    
    // User actions
    val feedbacksGiven: Int = 0,
    val bugsReported: Int = 0,
) {
    /**
     * Get the visit count for a screen by its tracking key.
     */
    fun getVisitCount(trackingKey: String): Int = screenVisits[trackingKey] ?: 0
    
    /**
     * Increment the visit count for a screen.
     */
    fun incrementVisit(trackingKey: String): AppData = copy(
        screenVisits = screenVisits + (trackingKey to (getVisitCount(trackingKey) + 1))
    )
}

interface AppCache : Cache<AppData>

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = AppCache::class)
@Inject
class AppCacheImpl(
    cacheFactory: CacheFactory
) : AppCache, Cache<AppData> by cacheFactory.persistent(
    name = "app_data",
    serializer = versionedJsonSerializer(
        defaultValue = { AppData() },
    )
)