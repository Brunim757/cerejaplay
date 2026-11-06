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
    val updatedAt: Long = 0L
) {
    val key: String get() = "$type:$id"
    val episodeLabel: String?
        get() = when {
            type == "series" && season != null && episode != null -> "T$season • E$episode"
            type == "anime" && episode != null -> "Episódio $episode"
            else -> null
        }
}

data class UserSettings(
    val audio: String = "dub",
    val quality: String = "auto",
    val language: String = "pt-BR"
)
