package com.dangerfield.hiittimer.libraries.preferences

import kotlinx.coroutines.flow.Flow

interface Preferences {
    fun <T : Any> flow(pref: Preference<T>): Flow<T>
    suspend fun <T : Any> get(pref: Preference<T>): T
    suspend fun <T : Any> set(pref: Preference<T>, value: T)
}
