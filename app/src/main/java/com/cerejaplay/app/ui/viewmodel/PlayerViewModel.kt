package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.Stream
import com.cerejaplay.app.data.model.UserSettings
import com.cerejaplay.app.data.model.WatchPayload
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val switching: Boolean = false,
    val startPositionMs: Long = 0L
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
        val poster: String?,
        val backdrop: String?
    )

    private var request: Request? = null
    private var loadJob: Job? = null
    // Fontes que ja falharam nesta sessao: nunca voltamos para elas, o que
    // evitava o vai-e-vem entre a primeira e a segunda fonte.
    private val failedStreams = mutableSetOf<String>()
    private var autoSwitchCount = 0

    fun load(
        endpoint: String,
        type: String,
        id: String,
        season: Int?,
        episode: Int?,
        title: String,
        poster: String?,
        backdrop: String?
    ) {
        request = Request(endpoint, type, id, season, episode, title, poster, backdrop)
        execute()
    }

    fun retry() {
        if (request != null) execute()
    }

    fun selectStream(stream: Stream) {
        failedStreams.remove(streamKey(stream))
        _state.update { it.copy(selected = stream, error = null, switching = false) }
    }

    fun onPlaybackError(stream: Stream, message: String) {
        val current = _state.value.selected ?: return
        // Erro de uma fonte antiga que ainda estava carregando: ignora.
        if (!sameStream(stream, current)) return

        failedStreams.add(streamKey(current))
        autoSwitchCount += 1
        val payload = _state.value.watch
        val next = payload?.streams?.firstOrNull { candidate ->
            candidate.isPlayable && !failedStreams.contains(streamKey(candidate))
        }

        if (next != null && autoSwitchCount <= MAX_AUTO_SWITCH) {
            val label = next.label ?: next.server ?: next.provider
            _state.update {
                it.copy(
                    selected = next,
                    error = "Fonte anterior falhou. Tentando $label…",
                    switching = true
                )
            }
        } else {
            _state.update {
                it.copy(
                    error = "Nenhuma fonte conseguiu reproduzir. Escolha outra em Fontes.",
                    switching = false
                )
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val current = request ?: return
        viewModelScope.launch {
            repository.saveProgress(
                type = current.type,
                id = current.id,
                season = current.season,
                episode = current.episode,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
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

    private fun streamKey(stream: Stream): String =
        "${stream.type}|${stream.url ?: stream.embed ?: stream.resolver}|${stream.audio ?: stream.language ?: ""}"

    private fun execute() {
        val current = request ?: return
        // Uma troca de audio/qualidade nao pode deixar duas buscas correndo:
        // a resposta antiga sobrescrevia a escolha mais recente.
        loadJob?.cancel()
        failedStreams.clear()
        autoSwitchCount = 0
        _state.update { it.copy(loading = true, error = null, switching = false) }
        loadJob = viewModelScope.launch {
            try {
                val userSettings = repository.settings.first()
                val response = repository.watch(
                    endpoint = current.endpoint,
                    type = current.type,
                    id = current.id,
                    season = current.season,
                    episode = current.episode,
                    settings = userSettings
                )
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
                    return@launch
                }
                val resumeAt = repository.resumePosition(
                    type = current.type,
                    id = current.id,
                    season = current.season,
                    episode = current.episode
                )
                val resolved = payload.copy(streams = playable, best = best)
                _state.update {
                    it.copy(
                        watch = resolved,
                        loading = false,
                        error = null,
                        selected = best,
                        startPositionMs = resumeAt
                    )
                }
                repository.recordRaw(
                    type = current.type,
                    id = current.id,
                    title = payload.title ?: current.title,
                    poster = current.poster,
                    backdrop = current.backdrop,
                    season = current.season?.takeIf { it > 0 },
                    episode = current.episode?.takeIf { it > 0 }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Falha ao localizar a fonte de vídeo.")
                }
            }
        }
    }

    companion object {
        private const val MAX_AUTO_SWITCH = 8
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { PlayerViewModel(repository) }
        }
    }
}
