package com.aivideogen.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_STABILITY_API_KEY = stringPreferencesKey("stability_api_key")
        private val KEY_OPENAI_API_KEY    = stringPreferencesKey("openai_api_key")
        private val KEY_DEFAULT_STYLE     = stringPreferencesKey("default_style")
        private val KEY_DEFAULT_RESOLUTION = stringPreferencesKey("default_resolution")
    }

    val stabilityApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_STABILITY_API_KEY] ?: ""
    }

    val openAiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_OPENAI_API_KEY] ?: ""
    }

    val defaultStyle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_STYLE] ?: "CINEMATIC"
    }

    val defaultResolution: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_RESOLUTION] ?: "HD_720"
    }

    suspend fun saveStabilityApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[KEY_STABILITY_API_KEY] = key }
    }

    suspend fun saveOpenAiApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[KEY_OPENAI_API_KEY] = key }
    }

    suspend fun saveDefaultStyle(style: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DEFAULT_STYLE] = style }
    }

    suspend fun saveDefaultResolution(resolution: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DEFAULT_RESOLUTION] = resolution }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
