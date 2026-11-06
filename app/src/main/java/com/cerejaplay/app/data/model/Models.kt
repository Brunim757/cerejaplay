package com.cerejaplay.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigResponse(
    val success: Boolean = false,
    val data: AppConfig = AppConfig()
)

@Serializable
data class AppConfig(
    val appName: String = "CerejaPlay",
    val tagline: String = "Filmes, séries e animes",
    val version: VersionConfig = VersionConfig(),
    val theme: ThemeConfig = ThemeConfig(),
    val tabs: List<TabConfig> = emptyList(),
    val preferences: PreferencesConfig = PreferencesConfig(),
    val player: PlayerConfig = PlayerConfig(),
    val features: FeatureFlags = FeatureFlags(),
    val assets: AssetsConfig = AssetsConfig(),
    val endpoints: EndpointsConfig = EndpointsConfig(),
    val legal: LegalConfig = LegalConfig()
)

@Serializable
data class VersionConfig(
    val current: String = "1.0.0",
    val minimum: String = "1.0.0",
    val latest: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val updateUrl: String = ""
)

@Serializable
data class ThemeConfig(
    val dark: Boolean = true,
    val primary: String = "#E23744",
    val onPrimary: String = "#FFFFFF",
    val secondary: String = "#FF4D5E",
    val background: String = "#0B0B0F",
    val surface: String = "#15151C",
    val surfaceVariant: String = "#1F1F29",
    val onSurface: String = "#FFFFFF",
    val muted: String = "#9A9AA5",
    val outline: String = "#2A2A36",
    val error: String = "#FF5252"
)

@Serializable
data class TabConfig(
    val id: String = "",
    val title: String = "",
    val type: String = "catalog",
    val mediaType: String? = null,
    val endpoint: String? = null
)

@Serializable
data class PreferencesConfig(
    val languages: List<LanguageChoice> = emptyList(),
    val defaultLanguage: String = "pt-BR",
    val audioOptions: List<AudioChoice> = emptyList(),
    val defaultAudio: String = "dub",
    val qualityOptions: List<String> = emptyList(),
    val defaultQuality: String = "auto"
)

@Serializable
data class LanguageChoice(val code: String = "", val label: String = "")

@Serializable
data class AudioChoice(val id: String = "", val label: String = "")

@Serializable
data class PlayerConfig(
    val autoplay: Boolean = true,
    val preferDirect: Boolean = true,
    val allowExternalPlayer: Boolean = true,
    val showQualitySelector: Boolean = true,
    val showLanguageSelector: Boolean = true,
    val resume: Boolean = true,
    val pictureInPicture: Boolean = true,
    val seekStepSeconds: Int = 10,
    val doubleTapSeekSeconds: Int = 10
)

@Serializable
data class FeatureFlags(
    val movies: Boolean = true,
    val series: Boolean = true,
    val animes: Boolean = true,
    val searchGlobal: Boolean = true,
    val continueWatching: Boolean = true,
    val subtitles: Boolean = true,
    val downloads: Boolean = false,
    val trailers: Boolean = false,
    val chromecast: Boolean = false
)

@Serializable
data class AssetsConfig(val logo: String? = null, val splash: String? = null, val placeholderPoster: String? = null)

@Serializable
data class EndpointsConfig(
    val config: String = "/api/app/config",
    val home: String = "/api/app/home",
    val catalog: CatalogEndpoints = CatalogEndpoints(),
    val search: String = "/api/app/search",
    val detail: String = "/api/app/detail",
    val watch: String = "/api/watch"
)

@Serializable
data class CatalogEndpoints(
    val movies: String = "/api/app/catalog/movies",
    val series: String = "/api/app/catalog/series",
    val animes: String = "/api/app/catalog/animes"
)

@Serializable
data class LegalConfig(
    val notice: String = "",
    val disclaimer: String = ""
)

@Serializable
data class HomeResponse(
    val success: Boolean = false,
    val data: HomePayload = HomePayload()
)

@Serializable
data class HomePayload(val rows: List<HomeRow> = emptyList())

@Serializable
data class HomeRow(
    val id: String = "",
    val title: String = "",
    val mediaType: String = "",
    val layout: String = "poster",
    val source: String? = null,
    val items: List<MediaCard> = emptyList()
)

@Serializable
data class MediaCard(
    val id: String? = null,
    val type: String = "",
    val imdbId: String? = null,
    val tmdbId: Long? = null,
    val slug: String? = null,
    val title: String = "",
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: Double = 0.0,
    val overview: String = "",
    val seasons: Int? = null,
    val episodes: Int? = null,
    val detailEndpoint: String? = null,
    val watchEndpoint: String? = null
) {
    val stableId: String get() = id ?: imdbId ?: slug ?: title
}

@Serializable
data class ListResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val data: List<MediaCard> = emptyList(),
    val meta: ListMeta? = null
)

@Serializable
data class ListMeta(
    val kind: String? = null,
    val page: Int? = null,
    val totalPages: Int? = null,
    val source: String? = null
)

@Serializable
data class SearchResponse(
    val success: Boolean = false,
    val data: SearchPayload = SearchPayload(),
    val meta: SearchMeta? = null
)

@Serializable
data class SearchMeta(val count: Int = 0, val page: Int? = null)

@Serializable
data class SearchPayload(
    val query: String = "",
    val page: Int = 1,
    val groups: SearchGroups = SearchGroups(),
    val errors: List<String> = emptyList()
)

@Serializable
data class SearchGroups(
    val movies: List<MediaCard> = emptyList(),
    val series: List<MediaCard> = emptyList(),
    val animes: List<MediaCard> = emptyList()
)

@Serializable
data class DetailResponse(
    val success: Boolean = false,
    val data: MediaDetail? = null
)

@Serializable
data class MediaDetail(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val originalTitle: String? = null,
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: Double = 0.0,
    val overview: String = "",
    val genres: List<String> = emptyList(),
    val runtime: Int? = null,
    val status: String? = null,
    val seasons: List<Season> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val totalEpisodes: Int = 0,
    val detailEndpoint: String? = null,
    val watchEndpoint: String? = null
)

@Serializable
data class Season(
    val number: Int = 1,
    val title: String? = null,
    val episodeCount: Int? = null,
    val id: String? = null
)

@Serializable
data class Episode(
    val number: Int = 1,
    val title: String? = null
)

@Serializable
data class WatchResponse(
    val success: Boolean = false,
    val data: WatchPayload? = null,
    val meta: WatchMeta? = null
)

@Serializable
data class WatchMeta(val best: String? = null)

@Serializable
data class WatchPayload(
    val type: String = "",
    val id: String = "",
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeLabel: String? = null,
    val seasons: List<Season> = emptyList(),
    val episodeCount: Int? = null,
    val languages: List<LanguageOption> = emptyList(),
    val providers: List<String> = emptyList(),
    val best: Stream? = null,
    val streams: List<Stream> = emptyList(),
    val elapsedMs: Long = 0
)

@Serializable
data class LanguageOption(
    val code: String? = null,
    val audio: String? = null,
    val label: String = ""
)

@Serializable
data class Stream(
    val provider: String = "",
    val extractor: String = "",
    val server: String? = null,
    val label: String? = null,
    val type: String = "iframe",
    val url: String? = null,
    val embed: String? = null,
    val quality: String? = null,
    val height: Int? = null,
    val language: String? = null,
    val audio: String? = null,
    val subtitles: List<Subtitle> = emptyList(),
    val qualities: List<QualityVariant> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val error: String? = null,
    val score: Int = 0
) {
    val isDirect: Boolean get() = (type == "hls" || type == "mp4") && !url.isNullOrBlank()
    val playTarget: String? get() = url ?: embed
}

@Serializable
data class Subtitle(
    val label: String = "",
    val url: String? = null,
    val language: String? = null
)

@Serializable
data class QualityVariant(
    val quality: String? = null,
    val bandwidth: Long? = null,
    val url: String? = null
)

@Serializable
data class ApiErrorBody(
    val success: Boolean = false,
    val error: String? = null
)
