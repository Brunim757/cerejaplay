package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.SearchGroups
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val loading: Boolean = false,
    val searched: Boolean = false,
    val groups: SearchGroups = SearchGroups(),
    val error: String? = null
) {
    val total: Int get() = groups.movies.size + groups.series.size + groups.animes.size
}

class SearchViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recents: StateFlow<List<String>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var job: Job? = null
    private var endpoint: String? = null

    fun configure(endpoint: String) {
        this.endpoint = endpoint
    }

    fun onQueryChange(value: String) {
        _query.value = value
        job?.cancel()
        val trimmed = value.trim()
        if (trimmed.length < 2) {
            _state.update { it.copy(groups = SearchGroups(), searched = false, loading = false, error = null) }
            return
        }
        job = viewModelScope.launch {
            delay(420)
            execute(trimmed)
        }
    }

    fun submit() {
        val trimmed = _query.value.trim()
        if (trimmed.length < 2) return
        viewModelScope.launch { repository.addRecentSearch(trimmed) }
        job?.cancel()
        job = viewModelScope.launch { execute(trimmed) }
    }

    fun useSuggestion(value: String) {
        _query.value = value
        submit()
    }

    fun clearRecents() {
        viewModelScope.launch { repository.clearRecentSearches() }
    }

    fun retry() {
        val trimmed = _query.value.trim()
        if (trimmed.length >= 2) {
            job?.cancel()
            job = viewModelScope.launch { execute(trimmed) }
        }
    }

    private suspend fun execute(query: String) {
        val current = endpoint ?: return
        _state.update { it.copy(loading = true, error = null) }
        runCatching { repository.search(current, query) }
            .onSuccess { response ->
                _state.update {
                    it.copy(loading = false, searched = true, groups = response.data.groups, error = null)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(loading = false, searched = true, error = error.message ?: "Falha na busca")
                }
            }
    }

    companion object {
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}
