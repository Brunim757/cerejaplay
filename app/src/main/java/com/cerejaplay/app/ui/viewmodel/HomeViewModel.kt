package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.HistoryItem
import com.cerejaplay.app.data.model.HomeRow
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
    val loading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private var loadedEndpoint: String? = null

    init {
        viewModelScope.launch {
            repository.history.collect { history ->
                _state.update { it.copy(history = history) }
            }
        }
    }

    fun load(endpoint: String, force: Boolean = false) {
        if (!force && loadedEndpoint == endpoint && _state.value.rows.isNotEmpty()) return
        loadedEndpoint = endpoint
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repository.home(endpoint) }
                .onSuccess { rows ->
                    _state.update { it.copy(rows = rows, loading = false, error = null) }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Falha ao carregar") }
                }
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
