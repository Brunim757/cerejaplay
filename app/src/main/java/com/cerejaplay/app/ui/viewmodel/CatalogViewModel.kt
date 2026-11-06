package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val cards: List<MediaCard> = emptyList(),
    val page: Int = 0,
    val totalPages: Int? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null
)

class CatalogViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var endpoint: String? = null

    fun start(endpoint: String) {
        if (this.endpoint == endpoint && _state.value.cards.isNotEmpty()) return
        this.endpoint = endpoint
        refresh()
    }

    fun refresh() {
        val current = endpoint ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, endReached = false) }
            runCatching { repository.catalog(current, 1) }
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            cards = response.data,
                            page = response.meta?.page ?: 1,
                            totalPages = response.meta?.totalPages,
                            loading = false,
                            endReached = response.data.isEmpty()
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Falha ao carregar") }
                }
        }
    }

    fun loadMore() {
        val current = endpoint ?: return
        val snapshot = _state.value
        if (snapshot.loading || snapshot.loadingMore || snapshot.endReached) return
        val nextPage = snapshot.page + 1
        if (snapshot.totalPages != null && nextPage > snapshot.totalPages) {
            _state.update { it.copy(endReached = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            runCatching { repository.catalog(current, nextPage) }
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            cards = it.cards + response.data,
                            page = response.meta?.page ?: nextPage,
                            totalPages = response.meta?.totalPages ?: it.totalPages,
                            loadingMore = false,
                            endReached = response.data.isEmpty()
                        )
                    }
                }
                .onFailure {
                    _state.update { state -> state.copy(loadingMore = false) }
                }
        }
    }

    companion object {
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { CatalogViewModel(repository) }
        }
    }
}
