package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.Stream
import com.cerejaplay.app.data.model.UserSettings
import com.cerejaplay.app.data.model.WatchPayload
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val watch: WatchPayload? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val selected: Stream? = null,
    val switching: Boolean = false
)

class PlayerViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private data class Request(
        val endpoint: String,
        val type: String,
        val id: String,
        val season: Int?,
        val episode: Int?,
        val title: String,
        val poster: String?
    )

    private var request: Request? = null

    fun load(
        endpoint: String,
        type: String,
        id: String,
        season: Int?,
        episode: Int?,
        title: String,
        poster: String?
    ) {
        request = Request(endpoint, type, id, season, episode, title, poster)
        execute()
    }

    fun retry() {
        if (request != null) execute()
    }

    fun selectStream(stream: Stream) {
        _state.update { it.copy(selected = stream, error = null, switching = false) }
    }

    fun onPlaybackError(message: String) {
        val payload = _state.value.watch ?: return
        val current = _state.value.selected
        val next = payload.streams.firstOrNull { candidate ->
            candidate.isPlayable && !sameStream(candidate, current)
        }
        if (next != null) {
            _state.update { it.copy(selected = next, error = message) }
        } else {
            _state.update {
                it.copy(error = "Não foi possível reproduzir esta fonte. Tente novamente mais tarde.")
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun setAudio(value: String) = viewModelScope.launch {
        repository.updateAudio(value)
        execute()
    }

    fun setQuality(value: String) = viewModelScope.launch {
        repository.updateQuality(value)
        execute()
    }

    fun setLanguage(value: String) = viewModelScope.launch {
        repository.updateLanguage(value)
        execute()
    }

    private fun sameStream(first: Stream, second: Stream?): Boolean {
        if (second == null) return false
        return first.type == second.type &&
            first.url == second.url &&
            first.embed == second.embed &&
            first.resolver == second.resolver
    }

    private fun execute() {
        val current = request ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val userSettings = repository.settings.first()
            runCatching {
                repository.watch(
                    endpoint = current.endpoint,
                    type = current.type,
                    id = current.id,
                    season = current.season,
                    episode = current.episode,
                    settings = userSettings
                )
            }.onSuccess { response ->
                val payload = response.data
                val playable = payload?.streams?.filter { it.isPlayable }.orEmpty()
                val best = payload?.best?.takeIf { it.isPlayable } ?: playable.firstOrNull()
                if (payload == null || playable.isEmpty() || best == null) {
                    _state.update {
                        it.copy(
                            loading = false,
                            watch = payload,
                            selected = null,
                            error = "Nenhuma fonte compatível com o player nativo para este título."
                        )
                    }
                    return@onSuccess
                }
                val resolved = payload.copy(streams = playable, best = best)
                _state.update {
                    it.copy(
                        watch = resolved,
                        loading = false,
                        error = null,
                        selected = best
                    )
                }
                repository.recordRaw(
                    type = current.type,
                    id = current.id,
                    title = payload.title ?: current.title,
                    poster = current.poster,
                    backdrop = null,
                    season = current.season?.takeIf { it > 0 },
                    episode = current.episode?.takeIf { it > 0 }
                )
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Falha ao localizar a fonte de vídeo.")
                }
            }
        }
    }

    companion object {
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { PlayerViewModel(repository) }
        }
    }
}