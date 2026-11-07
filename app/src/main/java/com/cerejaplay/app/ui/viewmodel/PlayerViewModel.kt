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
    val startPositionMs: Long = 0L,
    /** Áudio pedido nas preferências (original/dub/sub). */
    val requestedAudio: String? = null,
    /** Aviso de fallback: "Só encontrei dublado. Deseja assistir mesmo assim?" */
    val audioNotice: String? = null,
    /** Exibe também as fontes instáveis/mortas quando true. */
    val showUnstable: Boolean = false
) {
    /** Todas as fontes jogáveis recebidas da API. */
    val allStreams: List<Stream> get() = watch?.streams.orEmpty()

    /** Fontes vivas; se nenhuma está confirmada, mostra todas. */
    val aliveStreams: List<Stream> get() = allStreams.filter { it.isAlive }

    /** O que a lista de fontes deve mostrar. Mortas ficam escondidas por padrão. */
    val visibleStreams: List<Stream>
        get() {
            val alive = aliveStreams
            return if (showUnstable || alive.isEmpty()) allStreams else alive
        }

    val unstableCount: Int get() = (allStreams.size - aliveStreams.size).coerceAtLeast(0)
}

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
    // Fontes que ja falharam nesta sessao: nunca voltamos para elas.
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
        _state.update {
            it.copy(selected = stream, error = null, switching = false, audioNotice = null)
        }
    }

    fun toggleUnstable() {
        _state.update { it.copy(showUnstable = !it.showUnstable) }
    }

    /** Usuário aceitou assistir no áudio alternativo (ex.: dublado). */
    fun confirmAudioFallback() {
        val s = _state.value
        val pool = s.allStreams.filter { it.isPlayable }
        val alive = pool.filter { it.isAlive }
        val next = (if (alive.isNotEmpty()) alive else pool).firstOrNull()
        if (next == null) {
            dismissAudioFallback()
            return
        }
        failedStreams.clear()
        autoSwitchCount = 0
        _state.update {
            it.copy(selected = next, audioNotice = null, error = null, switching = true)
        }
    }

    /** Usuário recusou o áudio alternativo: volta para a lista de fontes. */
    fun dismissAudioFallback() {
        _state.update { it.copy(audioNotice = null) }
    }

    /**
     * O player conseguiu tocar. Marca a fonte como boa (local + API) para que
     * as próximas buscas comecem por ela.
     */
    fun onPlaybackStarted(stream: Stream) {
        val current = _state.value.selected ?: return
        if (!sameStream(stream, current)) return
        val req = request ?: return
        viewModelScope.launch {
            repository.setLastGoodProvider(stream.extractor.ifBlank { stream.provider })
            repository.feedback(
                type = req.type,
                id = req.id,
                stream = stream,
                success = true,
                season = req.season,
                episode = req.episode,
                audio = stream.audio
            )
        }
    }

    fun onPlaybackError(stream: Stream, message: String) {
        val s = _state.value
        val current = s.selected ?: return
        // Erro de uma fonte antiga que ainda estava carregando: ignora.
        if (!sameStream(stream, current)) return

        failedStreams.add(streamKey(current))
        autoSwitchCount += 1
        request?.let { req ->
            viewModelScope.launch {
                repository.feedback(
                    type = req.type,
                    id = req.id,
                    stream = current,
                    success = false,
                    season = req.season,
                    episode = req.episode,
                    audio = current.audio
                )
            }
        }

        val pool = s.allStreams.filter { it.isPlayable }
        val alive = pool.filter { it.isAlive }
        val usable = if (alive.isNotEmpty()) alive else pool
        // Prioriza o áudio pedido; só cai para outro áudio se não houver mesmo.
        val matching = usable.filter { it.matchesAudio(s.requestedAudio) }
        val candidates = if (matching.isNotEmpty()) matching else usable
        val next = candidates.firstOrNull { !failedStreams.contains(streamKey(it)) }
            ?: candidates.firstOrNull { !failedStreams.contains(streamKey(it)) }

        if (next != null && autoSwitchCount <= MAX_AUTO_SWITCH) {
            val label = next.label ?: next.server ?: next.provider
            _state.update {
                it.copy(
                    selected = next,
                    error = "Fonte anterior falhou. Tentando $label...",
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
        // Uma troca de audio/qualidade nao pode deixar duas buscas correndo.
        loadJob?.cancel()
        failedStreams.clear()
        autoSwitchCount = 0
        _state.update {
            it.copy(loading = true, error = null, switching = false, audioNotice = null)
        }
        loadJob = viewModelScope.launch {
            try {
                val userSettings = repository.settings.first()
                val lastGood = repository.lastGoodProvider.first()
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
                if (payload == null || playable.isEmpty()) {
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

                val requestedAudio = payload.requestedAudio ?: userSettings.audio
                val alive = playable.filter { it.isAlive }
                val pool = if (alive.isNotEmpty()) alive else playable
                val matching = pool.filter { it.matchesAudio(requestedAudio) }

                val resumeAt = repository.resumePosition(
                    type = current.type,
                    id = current.id,
                    season = current.season,
                    episode = current.episode
                )

                // Base com TODAS as fontes (a UI decide esconder as mortas).
                val resolved = payload.copy(streams = playable, best = payload.best)

                if (matching.isEmpty()) {
                    // Não existe fonte no áudio pedido: NÃO toca dublado sem avisar.
                    val notice = payload.audioNotice
                        ?: "Só encontrei ${audioLabel(payload.fallbackAudio)}. Deseja assistir mesmo assim?"
                    _state.update {
                        it.copy(
                            watch = resolved,
                            loading = false,
                            error = null,
                            selected = null,
                            requestedAudio = requestedAudio,
                            audioNotice = notice,
                            startPositionMs = resumeAt
                        )
                    }
                    recordHistory(payload.title ?: current.title)
                    return@launch
                }

                val best = matching.firstOrNull { sameStream(it, payload.best) }
                    ?: matching.firstOrNull { it.extractor == lastGood }
                    ?: matching.first()
                _state.update {
                    it.copy(
                        watch = resolved,
                        loading = false,
                        error = null,
                        selected = best,
                        requestedAudio = requestedAudio,
                        audioNotice = null,
                        startPositionMs = resumeAt
                    )
                }
                recordHistory(payload.title ?: current.title)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Falha ao localizar a fonte de vídeo.")
                }
            }
        }
    }

    private fun audioLabel(audio: String?): String = when (audio?.lowercase()) {
        "dub" -> "dublado"
        "original" -> "original"
        "sub", "leg" -> "legendado"
        else -> "outro áudio"
    }

    private fun recordHistory(title: String) {
        val current = request ?: return
        viewModelScope.launch {
            repository.recordRaw(
                type = current.type,
                id = current.id,
                title = title,
                poster = current.poster,
                backdrop = current.backdrop,
                season = current.season?.takeIf { it > 0 },
                episode = current.episode?.takeIf { it > 0 }
            )
        }
    }

    companion object {
        private const val MAX_AUTO_SWITCH = 8
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { PlayerViewModel(repository) }
        }
    }
}
