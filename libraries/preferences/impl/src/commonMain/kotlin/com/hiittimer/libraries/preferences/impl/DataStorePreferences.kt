package com.dangerfield.hiittimer.libraries.preferences.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences as DsPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dangerfield.hiittimer.libraries.preferences.Preference
import com.dangerfield.hiittimer.libraries.preferences.Preferences
import com.dangerfield.hiittimer.libraries.storage.FileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DataStorePreferences(
    fileManager: FileManager,
) : Preferences {

    private val dataStore: DataStore<DsPreferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { fileManager.createFile(FileName) }
    )

    override fun <T : Any> flow(pref: Preference<T>): Flow<T> =
        dataStore.data.map { it.read(pref) }

    override suspend fun <T : Any> get(pref: Preference<T>): T =
        dataStore.data.first().read(pref)

    override suspend fun <T : Any> set(pref: Preference<T>, value: T) {
        dataStore.edit { prefs -> prefs.write(pref, value) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> DsPreferences.read(pref: Preference<T>): T = when (val default = pref.default) {
        is Boolean -> (this[booleanPreferencesKey(pref.key)] ?: default) as T
        is Int -> (this[intPreferencesKey(pref.key)] ?: default) as T
        is Long -> (this[longPreferencesKey(pref.key)] ?: default) as T
        is Float -> (this[floatPreferencesKey(pref.key)] ?: default) as T
        is Double -> (this[doublePreferencesKey(pref.key)] ?: default) as T
        is String -> (this[stringPreferencesKey(pref.key)] ?: default) as T
        else -> error("Unsupported preference type for ${pref.key}: ${default::class}")
    }

    private fun <T : Any> androidx.datastore.preferences.core.MutablePreferences.write(pref: Preference<T>, value: T) {
        when (value) {
            is Boolean -> this[booleanPreferencesKey(pref.key)] = value
            is Int -> this[intPreferencesKey(pref.key)] = value
            is Long -> this[longPreferencesKey(pref.key)] = value
            is Float -> this[floatPreferencesKey(pref.key)] = value
            is Double -> this[doublePreferencesKey(pref.key)] = value
            is String -> this[stringPreferencesKey(pref.key)] = value
            else -> error("Unsupported preference type for ${pref.key}: ${value::class}")
        }
    }

    companion object {
        private const val FileName = "user_preferences.preferences_pb"
    }
}
