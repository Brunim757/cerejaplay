package com.cerejaplay.app.data.repo

import com.cerejaplay.app.data.api.CerejaService
import com.cerejaplay.app.data.model.ApiErrorBody
import com.cerejaplay.app.data.model.AppConfig
import com.cerejaplay.app.data.model.AudioChoice
import com.cerejaplay.app.data.model.CalendarEntry
import com.cerejaplay.app.data.model.CatalogEndpoints
import com.cerejaplay.app.data.model.DetailResponse
import com.cerejaplay.app.data.model.EndpointsConfig
import com.cerejaplay.app.data.model.HistoryItem
import com.cerejaplay.app.data.model.LanguageChoice
import com.cerejaplay.app.data.model.ListResponse
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.model.MediaDetail
import com.cerejaplay.app.data.model.PreferencesConfig
import com.cerejaplay.app.data.model.RelatedItem
import com.cerejaplay.app.data.model.SearchResponse
import com.cerejaplay.app.data.model.TabConfig
import com.cerejaplay.app.data.model.UserSettings
import com.cerejaplay.app.data.model.WatchResponse
import com.cerejaplay.app.data.prefs.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class ApiFailure(message: String, cause: Throwable? = null) : Exception(message, cause)

object DefaultConfig {
    val value: AppConfig = AppConfig(
        tabs = listOf(
            TabConfig("home", "Início", "home", null, "/api/app/home"),
            TabConfig("movies", "Filmes", "catalog", "movie", "/api/app/catalog/movies"),
            TabConfig("series", "Séries", "catalog", "series", "/api/app/catalog/series"),
            TabConfig("animes", "Animes", "catalog", "anime", "/api/app/catalog/animes"),
            TabConfig("search", "Buscar", "search", null, "/api/app/search")
        ),
        preferences = PreferencesConfig(
            languages = listOf(
                LanguageChoice("pt-BR", "Português (Brasil)"),
                LanguageChoice("en", "English"),
                LanguageChoice("es", "Español")
            ),
            audioOptions = listOf(
                AudioChoice("dub", "Dublado"),
                AudioChoice("sub", "Legendado"),
                AudioChoice("original", "Original")
            ),
            qualityOptions = listOf("auto", "1080p", "720p", "480p", "360p")
        ),
        endpoints = EndpointsConfig(
            catalog = CatalogEndpoints(
                movies = "/api/app/catalog/movies",
                series = "/api/app/catalog/series",
                animes = "/api/app/catalog/animes"
            )
        )
    )
}

class CerejaRepository(
    private val service: CerejaService,
    private val json: Json,
    private val preferences: UserPreferences
) {

    val settings: Flow<UserSettings> = preferences.settings
    val history: Flow<List<HistoryItem>> = preferences.history
    val favorites: Flow<List<MediaCard>> = preferences.favorites
    val recentSearches: Flow<List<String>> = preferences.recentSearches

    suspend fun loadConfig(): AppConfig {
        val remote = service.config().data
        if (remote.tabs.isNotEmpty()) {
            preferences.cacheConfig(remote)
            return remote
        }
        return preferences.loadCachedConfig() ?: DefaultConfig.value
    }

    suspend fun cachedConfig(): AppConfig? = preferences.loadCachedConfig()

    suspend fun fallbackConfig(): AppConfig = DefaultConfig.value

    suspend fun home(endpoint: String): List<com.cerejaplay.app.data.model.HomeRow> =
        call { service.home(endpoint) }.data.rows

    suspend fun becauseYouWatched(
        endpoint: String,
        seeds: List<Pair<String, String>>
    ): List<com.cerejaplay.app.data.model.HomeRow> {
        if (seeds.isEmpty()) return emptyList()
        val encoded = seeds.joinToString(",") { (type, id) -> "$type:$id" }
        return call { service.becauseYouWatched(endpoint, encoded) }.data.rows
    }

    suspend fun catalog(endpoint: String, page: Int, category: String? = null): ListResponse =
        call { service.catalog(endpoint, page, category) }

    suspend fun search(endpoint: String, query: String, page: Int = 1): SearchResponse =
        call { service.search(endpoint, query, page) }

    suspend fun detail(endpoint: String, type: String, id: String): MediaDetail =
        call { service.detail(endpoint, type, id) }.data
            ?: throw ApiFailure("Conteúdo não encontrado")

    suspend fun related(endpoint: String, type: String, id: String): List<RelatedItem> =
        call { service.related(endpoint, type, id) }.data

    suspend fun recommendations(endpoint: String, type: String, id: String): List<MediaCard> =
        call { service.recommendations(endpoint, type, id) }.data

    suspend fun categories(endpoint: String, kind: String): List<String> =
        call { service.categories("$endpoint/$kind") }.data.categories

    suspend fun genres(endpoint: String, kind: String): List<String> =
        call { service.genres("$endpoint/$kind") }.data.genres

    suspend fun discover(
        endpoint: String,
        kind: String,
        genre: String? = null,
        year: Int? = null,
        minRating: Double? = null,
        sort: String? = null,
        page: Int = 1
    ): ListResponse = call { service.discover(endpoint, kind, genre, year, minRating, sort, page) }

    suspend fun calendar(endpoint: String, days: Int = 7): List<CalendarEntry> =
        call { service.calendar(endpoint, days) }.data

    suspend fun watch(
        endpoint: String,
        type: String,
        id: String,
        season: Int? = null,
        episode: Int? = null,
        settings: UserSettings
    ): WatchResponse = call {
        service.watch(
            path = endpoint,
            type = type,
            id = id,
            season = season,
            episode = episode,
            language = settings.language,
            audio = settings.audio,
            quality = settings.quality.takeIf { it != "auto" }
        )
    }

    suspend fun recordHistory(card: MediaCard, season: Int? = null, episode: Int? = null) {
        val id = card.stableId
        if (id.isBlank()) return
        preferences.addHistory(
            HistoryItem(
                type = card.type,
                id = id,
                title = card.title,
                poster = card.poster,
                backdrop = card.backdrop,
                season = season,
                episode = episode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordHistory(detail: MediaDetail, season: Int? = null, episode: Int? = null) {
        preferences.addHistory(
            HistoryItem(
                type = detail.type,
                id = detail.id,
                title = detail.title,
                poster = detail.poster,
                backdrop = detail.backdrop,
                season = season,
                episode = episode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordRaw(
        type: String,
        id: String,
        title: String,
        poster: String?,
        backdrop: String?,
        season: Int? = null,
        episode: Int? = null
    ) {
        if (id.isBlank() || title.isBlank()) return
        preferences.addHistory(
            HistoryItem(
                type = type,
                id = id,
                title = title,
                poster = poster,
                backdrop = backdrop,
                season = season,
                episode = episode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleFavorite(card: MediaCard) = preferences.toggleFavorite(card)

    suspend fun addRecentSearch(query: String) = preferences.addRecentSearch(query)

    suspend fun clearRecentSearches() = preferences.clearRecentSearches()

    suspend fun saveProgress(
        type: String,
        id: String,
        season: Int?,
        episode: Int?,
        positionMs: Long,
        durationMs: Long
    ) = preferences.updateProgress(type, id, season, episode, positionMs, durationMs)

    suspend fun resumePosition(type: String, id: String, season: Int?, episode: Int?): Long {
        val item = preferences.history.first().firstOrNull {
            it.type == type && it.id == id && it.season == season && it.episode == episode
        } ?: return 0L
        if (item.durationMs <= 0L) return 0L
        // Não retoma se estiver no começo ou praticamente no fim.
        if (item.positionMs < 5_000L) return 0L
        if (item.positionMs >= item.durationMs - 10_000L) return 0L
        return item.positionMs
    }

    suspend fun updateAudio(value: String) = preferences.updateAudio(value)
    suspend fun updateQuality(value: String) = preferences.updateQuality(value)
    suspend fun updateLanguage(value: String) = preferences.updateLanguage(value)
    suspend fun updateTheme(value: String) = preferences.updateTheme(value)
    suspend fun updateProfile(value: String) = preferences.updateProfile(value)
    suspend fun clearHistory() = preferences.clearHistory()

    private suspend fun <T> call(block: suspend () -> T): T {
        try {
            return block()
        } catch (http: HttpException) {
            throw ApiFailure(messageFor(http), http)
        } catch (io: IOException) {
            throw ApiFailure("Sem conexão com a API. Verifique sua internet.", io)
        } catch (error: ApiFailure) {
            throw error
        } catch (error: Exception) {
            throw ApiFailure(error.message ?: "Falha inesperada ao carregar os dados.", error)
        }
    }

    private fun messageFor(http: HttpException): String {
        val raw = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
        if (!raw.isNullOrBlank()) {
            val parsed = runCatching { json.decodeFromString(ApiErrorBody.serializer(), raw) }.getOrNull()
            if (!parsed?.error.isNullOrBlank()) return parsed!!.error!!
        }
        return when (http.code()) {
            404 -> "Conteúdo não encontrado."
            503 -> "Recurso indisponível no momento."
            in 500..599 -> "O servidor demorou para responder. Tente novamente."
            else -> "Falha ao carregar (HTTP ${http.code()})."
        }
    }
}
