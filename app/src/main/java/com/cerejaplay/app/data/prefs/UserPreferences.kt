package com.cerejaplay.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cerejaplay.app.data.model.AppConfig
import com.cerejaplay.app.data.model.HistoryItem
import com.cerejaplay.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.cerejaDataStore: DataStore<Preferences> by preferencesDataStore(name = "cereja_prefs")

class UserPreferences(private val context: Context, private val json: Json) {

    private object Keys {
        val CONFIG = stringPreferencesKey("config_json")
        val AUDIO = stringPreferencesKey("audio")
        val QUALITY = stringPreferencesKey("quality")
        val LANGUAGE = stringPreferencesKey("language")
        val HISTORY = stringPreferencesKey("history_json")
    }

    val settings: Flow<UserSettings> = context.cerejaDataStore.data.map { prefs ->
        UserSettings(
            audio = prefs[Keys.AUDIO] ?: "dub",
            quality = prefs[Keys.QUALITY] ?: "auto",
            language = prefs[Keys.LANGUAGE] ?: "pt-BR"
        )
    }

    val history: Flow<List<HistoryItem>> = context.cerejaDataStore.data.map { prefs ->
        decodeHistory(prefs[Keys.HISTORY])
    }

    suspend fun updateAudio(audio: String) {
        context.cerejaDataStore.edit { it[Keys.AUDIO] = audio }
    }

    suspend fun updateQuality(quality: String) {
        context.cerejaDataStore.edit { it[Keys.QUALITY] = quality }
    }

    suspend fun updateLanguage(language: String) {
        context.cerejaDataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun cacheConfig(config: AppConfig) {
        val encoded = runCatching { json.encodeToString(AppConfig.serializer(), config) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[Keys.CONFIG] = encoded }
    }

    suspend fun loadCachedConfig(): AppConfig? {
        val encoded = context.cerejaDataStore.data.first()[Keys.CONFIG] ?: return null
        return runCatching { json.decodeFromString(AppConfig.serializer(), encoded) }.getOrNull()
    }

    suspend fun addHistory(item: HistoryItem) {
        val current = context.cerejaDataStore.data.first()[Keys.HISTORY].let(::decodeHistory)
        val updated = (listOf(item) + current.filterNot { it.key == item.key }).take(20)
        val encoded = runCatching { json.encodeToString(ListSerializer(HistoryItem.serializer()), updated) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[Keys.HISTORY] = encoded }
    }

    suspend fun clearHistory() {
        context.cerejaDataStore.edit { it.remove(Keys.HISTORY) }
    }

    private fun decodeHistory(encoded: String?): List<HistoryItem> {
        if (encoded.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(HistoryItem.serializer()), encoded) }.getOrDefault(emptyList())
    }
}
