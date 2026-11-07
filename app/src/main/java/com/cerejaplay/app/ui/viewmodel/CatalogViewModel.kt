package com.cerejaplay.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.Job
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
    val refreshing: Boolean = false,
    val endReached: Boolean = false,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val error: String? = null
)

class CatalogViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var endpoint: String? = null
    private var categoriesEndpoint: String? = null
    private var refreshJob: Job? = null
    private var loadMoreJob: Job? = null

    // Trava síncrona: com scroll rápido, vários eventos chegam antes de o
    // StateFlow propagar o loadingMore. Sem isso, a mesma página era baixada
    // várias vezes e os cards duplicados quebravam o LazyGrid.
    private var loadingMoreGuard = false

    fun start(endpoint: String, categoriesEndpoint: String? = null, kind: String? = null) {
        this.categoriesEndpoint = categoriesEndpoint
        if (kind != null && categoriesEndpoint != null) loadCategories(categoriesEndpoint, kind)
        if (this.endpoint == endpoint && _state.value.cards.isNotEmpty()) return
        this.endpoint = endpoint
        refresh()
    }

    fun loadCategories(endpoint: String, kind: String) {
        viewModelScope.launch {
            runCatching { repository.categories(endpoint, kind) }
                .onSuccess { categories -> _state.update { it.copy(categories = categories) } }
        }
    }

    fun selectCategory(category: String?) {
        if (_state.value.selectedCategory == category) return
        _state.update { it.copy(selectedCategory = category) }
        refresh()
    }

    fun refresh() {
        val current = endpoint ?: return
        refreshJob?.cancel()
        loadMoreJob?.cancel()
        loadingMoreGuard = false
        val category = _state.value.selectedCategory
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    cards = emptyList(),
                    page = 0,
                    loading = !it.refreshing,
                    loadingMore = false,
                    endReached = false,
                    error = null
                )
            }
            runCatching { repository.catalog(current, 1, category) }
                .onSuccess { response ->
                    if (endpoint != current) return@onSuccess
                    val cards = uniqueCards(response.data)
                    val page = response.meta?.page ?: 1
                    val totalPages = response.meta?.totalPages
                    _state.update {
                        it.copy(
                            cards = cards,
                            page = page,
                            totalPages = totalPages,
                            loading = false,
                            loadingMore = false,
                            refreshing = false,
                            endReached = cards.isEmpty() ||
                                response.data.isEmpty() ||
                                (totalPages != null && page >= totalPages)
                        )
                    }
                }
                .onFailure { error ->
                    if (endpoint != current) return@onFailure
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMore = false,
                            refreshing = false,
                            error = error.message ?: "Falha ao carregar"
                        )
                    }
                }
        }
    }

    fun pullRefresh() {
        if (endpoint == null) return
        _state.update { it.copy(refreshing = true) }
        refresh()
    }

    fun loadMore() {
        val current = endpoint ?: return
        val snapshot = _state.value
        if (loadingMoreGuard || snapshot.loading || snapshot.loadingMore || snapshot.endReached) return
        val nextPage = snapshot.page + 1
        if (snapshot.totalPages != null && nextPage > snapshot.totalPages) {
            _state.update { it.copy(endReached = true) }
            return
        }

        loadingMoreGuard = true
        _state.update { it.copy(loadingMore = true, error = null) }

        loadMoreJob = viewModelScope.launch {
            runCatching { repository.catalog(current, nextPage, snapshot.selectedCategory) }
                .onSuccess { response ->
                    if (endpoint != current) return@onSuccess
                    val known = _state.value.cards.mapTo(mutableSetOf()) { cardKey(it) }
                    val fresh = response.data.filter { known.add(cardKey(it)) }
                    val page = response.meta?.page ?: nextPage
                    val totalPages = response.meta?.totalPages ?: _state.value.totalPages
                    _state.update {
                        it.copy(
                            cards = it.cards + fresh,
                            page = page,
                            totalPages = totalPages,
                            loadingMore = false,
                            endReached = response.data.isEmpty() ||
                                (totalPages != null && page >= totalPages)
                        )
                    }
                }
                .onFailure {
                    if (endpoint != current) return@onFailure
                    _state.update { state -> state.copy(loadingMore = false) }
                }
            loadingMoreGuard = false
        }
    }

    private fun cardKey(card: MediaCard): String = "${card.type}:${card.stableId}"

    private fun uniqueCards(cards: List<MediaCard>): List<MediaCard> {
        val seen = HashSet<String>(cards.size)
        return cards.filter { card ->
            card.stableId.isNotBlank() && seen.add(cardKey(card))
        }
    }

    companion object {
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { CatalogViewModel(repository) }
        }
    }
}
