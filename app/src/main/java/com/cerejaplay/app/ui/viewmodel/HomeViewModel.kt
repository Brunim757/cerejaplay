package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.HistoryItem
import com.cerejaplay.app.data.model.HomeRow
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.model.UserSettings
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val rows: List<HomeRow> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val favorites: List<MediaCard> = emptyList(),
    val becauseRows: List<HomeRow> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null
)

class HomeViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private var loadedEndpoint: String? = null
    private var becauseEndpoint: String? = null

    init {
        viewModelScope.launch {
            repository.history.collect { history ->
                _state.update { it.copy(history = history) }
                if (becauseEndpoint != null) loadBecauseYouWatched(force = false)
            }
        }
        viewModelScope.launch {
            repository.favorites.collect { favorites ->
                _state.update { it.copy(favorites = favorites) }
            }
        }
    }

    fun load(endpoint: String, force: Boolean = false) {
        if (!force && loadedEndpoint == endpoint && _state.value.rows.isNotEmpty()) return
        loadedEndpoint = endpoint
        viewModelScope.launch {
            _state.update { it.copy(loading = !it.refreshing, error = null) }
            runCatching { repository.home(endpoint) }
                .onSuccess { rows ->
                    _state.update { it.copy(rows = rows, loading = false, refreshing = false, error = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = error.message ?: "Falha ao carregar"
                        )
                    }
                }
        }
    }

    fun refresh() {
        val endpoint = loadedEndpoint ?: return
        _state.update { it.copy(refreshing = true, error = null) }
        load(endpoint, force = true)
        loadBecauseYouWatched(force = true)
    }

    fun configureBecauseYouWatched(endpoint: String) {
        becauseEndpoint = endpoint
        loadBecauseYouWatched(force = false)
    }

    private fun loadBecauseYouWatched(force: Boolean) {
        val endpoint = becauseEndpoint ?: return
        if (!force && _state.value.becauseRows.isNotEmpty()) return
        val seeds = _state.value.history
            .distinctBy { "${it.type}:${it.id}" }
            .take(4)
            .map { it.type to it.id }
        if (seeds.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.becauseYouWatched(endpoint, seeds) }
                .onSuccess { rows -> _state.update { it.copy(becauseRows = rows) } }
        }
    }

    fun setAudio(value: String) = viewModelScope.launch { repository.updateAudio(value) }
    fun setQuality(value: String) = viewModelScope.launch { repository.updateQuality(value) }
    fun setLanguage(value: String) = viewModelScope.launch { repository.updateLanguage(value) }
    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }

    companion object {
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
