package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.model.MediaDetail
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val detail: MediaDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val selectedSeason: Int = 1,
    val favorites: List<MediaCard> = emptyList()
) {
    val isFavorite: Boolean
        get() = detail != null && favorites.any {
            it.type == detail.type && it.stableId == detail.id
        }
}

class DetailViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private var loadedKey: String? = null

    init {
        viewModelScope.launch {
            repository.favorites.collect { favorites ->
                _state.update { it.copy(favorites = favorites) }
            }
        }
    }

    fun load(endpoint: String, type: String, id: String, force: Boolean = false) {
        val key = "$type:$id"
        if (!force && loadedKey == key && _state.value.detail != null) return
        loadedKey = key
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repository.detail(endpoint, type, id) }
                .onSuccess { detail ->
                    val firstSeason = detail.seasons.firstOrNull()?.number ?: 1
                    _state.update {
                        it.copy(detail = detail, loading = false, error = null, selectedSeason = firstSeason)
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Falha ao carregar") }
                }
        }
    }

    fun selectSeason(number: Int) {
        _state.update { it.copy(selectedSeason = number) }
    }

    fun toggleFavorite() {
        val detail = _state.value.detail ?: return
        viewModelScope.launch { repository.toggleFavorite(detail.toCard()) }
    }

    companion object {
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { DetailViewModel(repository) }
        }
    }
}

private fun MediaDetail.toCard(): MediaCard = MediaCard(
    id = id,
    type = type,
    title = title,
    year = year,
    poster = poster,
    backdrop = backdrop,
    rating = rating,
    overview = overview,
    seasons = seasons.size.takeIf { it > 0 },
    episodes = totalEpisodes.takeIf { it > 0 },
    detailEndpoint = detailEndpoint,
    watchEndpoint = watchEndpoint
)
