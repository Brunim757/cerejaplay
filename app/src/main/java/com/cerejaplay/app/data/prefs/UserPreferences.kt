package com.cerejaplay.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cerejaplay.app.data.model.AppConfig
import com.cerejaplay.app.data.model.HistoryItem
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.cerejaDataStore: DataStore<Preferences> by preferencesDataStore(name = "cereja_prefs")

class UserPreferences(private val context: Context, private val json: Json) {

    private object Keys {
        val CONFIG = stringPreferencesKey("config_json")
        val AUDIO = stringPreferencesKey("audio")
        val QUALITY = stringPreferencesKey("quality")
        val LANGUAGE = stringPreferencesKey("language")
        val HISTORY = stringPreferencesKey("history_json")
        val FAVORITES = stringPreferencesKey("favorites_json")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches_json")
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

    val favorites: Flow<List<MediaCard>> = context.cerejaDataStore.data.map { prefs ->
        decodeCards(prefs[Keys.FAVORITES])
    }

    val recentSearches: Flow<List<String>> = context.cerejaDataStore.data.map { prefs ->
        decodeSearches(prefs[Keys.RECENT_SEARCHES])
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
        val previous = current.firstOrNull { it.key == item.key }
        // Mantém o progresso somente quando é o mesmo episódio/filme. Ao trocar
        // de episódio o player deve começar do início.
        val samePlayback = previous != null &&
            previous.season == item.season &&
            previous.episode == item.episode
        val merged = item.copy(
            positionMs = if (samePlayback) previous!!.positionMs else 0L,
            durationMs = if (samePlayback) previous!!.durationMs else 0L
        )
        val updated = (listOf(merged) + current.filterNot { it.key == item.key }).take(30)
        val encoded = runCatching { json.encodeToString(ListSerializer(HistoryItem.serializer()), updated) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[Keys.HISTORY] = encoded }
    }

    suspend fun updateProgress(
        type: String,
        id: String,
        season: Int?,
        episode: Int?,
        positionMs: Long,
        durationMs: Long
    ) {
        val current = context.cerejaDataStore.data.first()[Keys.HISTORY].let(::decodeHistory)
        if (current.isEmpty()) return
        var changed = false
        val updated = current.map { item ->
            val sameTitle = item.type == type && item.id == id
            val samePlayback = item.season == season && item.episode == episode
            if (sameTitle && samePlayback) {
                changed = true
                item.copy(
                    positionMs = positionMs.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(0L),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                item
            }
        }
        if (!changed) return
        val encoded = runCatching { json.encodeToString(ListSerializer(HistoryItem.serializer()), updated) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[Keys.HISTORY] = encoded }
    }

    suspend fun clearHistory() {
        context.cerejaDataStore.edit { it.remove(Keys.HISTORY) }
    }

    suspend fun toggleFavorite(card: MediaCard) {
        val current = context.cerejaDataStore.data.first()[Keys.FAVORITES].let(::decodeCards)
        val key = "${card.type}:${card.stableId}"
        val exists = current.any { "${it.type}:${it.stableId}" == key }
        val updated = if (exists) {
            current.filterNot { "${it.type}:${it.stableId}" == key }
        } else {
            (listOf(card) + current).take(120)
        }
        val encoded = runCatching { json.encodeToString(ListSerializer(MediaCard.serializer()), updated) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[Keys.FAVORITES] = encoded }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val current = context.cerejaDataStore.data.first()[Keys.RECENT_SEARCHES].let(::decodeSearches)
        val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(8)
        val encoded = runCatching { json.encodeToString(ListSerializer(String.serializer()), updated) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[Keys.RECENT_SEARCHES] = encoded }
    }

    suspend fun clearRecentSearches() {
        context.cerejaDataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }

    private fun decodeHistory(encoded: String?): List<HistoryItem> {
        if (encoded.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(HistoryItem.serializer()), encoded) }.getOrDefault(emptyList())
    }

    private fun decodeCards(encoded: String?): List<MediaCard> {
        if (encoded.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(MediaCard.serializer()), encoded) }.getOrDefault(emptyList())
    }

    private fun decodeSearches(encoded: String?): List<String> {
        if (encoded.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(String.serializer()), encoded) }.getOrDefault(emptyList())
    }
}
