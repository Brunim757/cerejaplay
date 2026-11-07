package com.cerejaplay.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

const val DEFAULT_PROFILE = "principal"

class UserPreferences(private val context: Context, private val json: Json) {

    private object Keys {
        val CONFIG = stringPreferencesKey("config_json")
        val AUDIO = stringPreferencesKey("audio")
        val QUALITY = stringPreferencesKey("quality")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val PROFILE = stringPreferencesKey("profile")
        val ONBOARDING = booleanPreferencesKey("onboarding_seen")
        val LAST_GOOD = stringPreferencesKey("last_good_provider")
        // Chaves legadas (perfil único) para migração transparente.
        val HISTORY = stringPreferencesKey("history_json")
        val FAVORITES = stringPreferencesKey("favorites_json")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches_json")
    }

    private fun historyKey(profile: String) = stringPreferencesKey("history_json_$profile")
    private fun favoritesKey(profile: String) = stringPreferencesKey("favorites_json_$profile")
    private fun searchesKey(profile: String) = stringPreferencesKey("recent_searches_json_$profile")

    private fun profileOf(prefs: Preferences): String = prefs[Keys.PROFILE] ?: DEFAULT_PROFILE

    val settings: Flow<UserSettings> = context.cerejaDataStore.data.map { prefs ->
        UserSettings(
            audio = prefs[Keys.AUDIO] ?: "dub",
            quality = prefs[Keys.QUALITY] ?: "auto",
            language = prefs[Keys.LANGUAGE] ?: "pt-BR",
            theme = prefs[Keys.THEME] ?: "dark",
            profile = profileOf(prefs)
        )
    }

    val history: Flow<List<HistoryItem>> = context.cerejaDataStore.data.map { prefs ->
        val profile = profileOf(prefs)
        decodeHistory(prefs[historyKey(profile)] ?: prefs[Keys.HISTORY])
    }

    val favorites: Flow<List<MediaCard>> = context.cerejaDataStore.data.map { prefs ->
        val profile = profileOf(prefs)
        decodeCards(prefs[favoritesKey(profile)] ?: prefs[Keys.FAVORITES])
    }

    val recentSearches: Flow<List<String>> = context.cerejaDataStore.data.map { prefs ->
        val profile = profileOf(prefs)
        decodeSearches(prefs[searchesKey(profile)] ?: prefs[Keys.RECENT_SEARCHES])
    }

    val onboardingSeen: Flow<Boolean> = context.cerejaDataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING] ?: false
    }

    /** Último provedor/extrator que tocou de verdade neste aparelho. */
    val lastGoodProvider: Flow<String?> = context.cerejaDataStore.data.map { prefs ->
        prefs[Keys.LAST_GOOD]
    }

    suspend fun setLastGoodProvider(value: String) {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return
        context.cerejaDataStore.edit { it[Keys.LAST_GOOD] = cleaned }
    }

    suspend fun setOnboardingSeen() {
        context.cerejaDataStore.edit { it[Keys.ONBOARDING] = true }
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

    suspend fun updateTheme(theme: String) {
        context.cerejaDataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun updateProfile(profile: String) {
        val cleaned = profile.trim().ifBlank { DEFAULT_PROFILE }
        context.cerejaDataStore.edit { it[Keys.PROFILE] = cleaned }
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
        val prefs = context.cerejaDataStore.data.first()
        val profile = profileOf(prefs)
        val key = historyKey(profile)
        val current = decodeHistory(prefs[key] ?: prefs[Keys.HISTORY])
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
        context.cerejaDataStore.edit { it[key] = encoded }
    }

    suspend fun updateProgress(
        type: String,
        id: String,
        season: Int?,
        episode: Int?,
        positionMs: Long,
        durationMs: Long
    ) {
        val prefs = context.cerejaDataStore.data.first()
        val profile = profileOf(prefs)
        val key = historyKey(profile)
        val current = decodeHistory(prefs[key] ?: prefs[Keys.HISTORY])
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
        context.cerejaDataStore.edit { it[key] = encoded }
    }

    suspend fun clearHistory() {
        val profile = profileOf(context.cerejaDataStore.data.first())
        context.cerejaDataStore.edit {
            it.remove(historyKey(profile))
            it.remove(Keys.HISTORY)
        }
    }

    suspend fun toggleFavorite(card: MediaCard) {
        val prefs = context.cerejaDataStore.data.first()
        val profile = profileOf(prefs)
        val key = favoritesKey(profile)
        val current = decodeCards(prefs[key] ?: prefs[Keys.FAVORITES])
        val cardKey = "${card.type}:${card.stableId}"
        val exists = current.any { "${it.type}:${it.stableId}" == cardKey }
        val updated = if (exists) {
            current.filterNot { "${it.type}:${it.stableId}" == cardKey }
        } else {
            (listOf(card) + current).take(120)
        }
        val encoded = runCatching { json.encodeToString(ListSerializer(MediaCard.serializer()), updated) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[key] = encoded }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val prefs = context.cerejaDataStore.data.first()
        val profile = profileOf(prefs)
        val key = searchesKey(profile)
        val current = decodeSearches(prefs[key] ?: prefs[Keys.RECENT_SEARCHES])
        val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(8)
        val encoded = runCatching { json.encodeToString(ListSerializer(String.serializer()), updated) }.getOrNull() ?: return
        context.cerejaDataStore.edit { it[key] = encoded }
    }

    suspend fun clearRecentSearches() {
        val profile = profileOf(context.cerejaDataStore.data.first())
        context.cerejaDataStore.edit {
            it.remove(searchesKey(profile))
            it.remove(Keys.RECENT_SEARCHES)
        }
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
