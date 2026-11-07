package com.cerejaplay.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    val type: String,
    val id: String,
    val title: String,
    val poster: String? = null,
    val backdrop: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L
) {
    val key: String get() = "$type:$id"

    val episodeLabel: String?
        get() = when {
            type == "series" && season != null && episode != null -> "T$season • E$episode"
            type == "anime" && episode != null -> "Episódio $episode"
            else -> null
        }

    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class UserSettings(
    val audio: String = "dub",
    val quality: String = "auto",
    val language: String = "pt-BR",
    val theme: String = "dark",
    val profile: String = "principal"
)

@Serializable
data class Profile(
    val id: String,
    val name: String
)

val defaultProfiles: List<Profile> = listOf(
    Profile("principal", "Principal"),
    Profile("infantil", "Infantil"),
    Profile("cinéfilo", "Cinéfilo")
)
