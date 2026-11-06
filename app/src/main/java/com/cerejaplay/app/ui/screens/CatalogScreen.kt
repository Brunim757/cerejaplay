package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.EmptyState
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.LoadingState
import com.cerejaplay.app.ui.components.PosterCard
import com.cerejaplay.app.ui.viewmodel.CatalogViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CatalogScreen(
    repository: CerejaRepository,
    title: String,
    endpoint: String,
    onOpenDetail: (String, String) -> Unit
) {
    val viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(endpoint) {
        viewModel.start(endpoint)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.statusBarsPadding()) {
            CerejaTopBar(title = title)
        }
        when {
            state.loading && state.cards.isEmpty() -> LoadingState(message = "Carregando $title...")
            state.error != null && state.cards.isEmpty() -> ErrorState(
                message = state.error ?: "",
                onRetry = viewModel::refresh
            )
            state.cards.isEmpty() -> EmptyState("Nenhum título disponível agora.")
            else -> CatalogGrid(
                cards = state.cards,
                loadingMore = state.loadingMore,
                onLoadMore = viewModel::loadMore,
                onOpenDetail = onOpenDetail
            )
        }
    }
}

@Composable
private fun CatalogGrid(
    cards: List<com.cerejaplay.app.data.model.MediaCard>,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenDetail: (String, String) -> Unit
) {
    val gridState = rememberLazyGridState()
    val size by rememberUpdatedState(cards.size)

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= size - 6) onLoadMore()
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Índice + id: mesmo que a API repita um card, a chave nunca colide
        // e o LazyGrid não derruba o app durante o scroll.
        itemsIndexed(cards, key = { index, card -> "$index:${card.type}:${card.stableId}" }) { _, card ->
            PosterCard(
                card = card,
                width = null,
                onClick = { onOpenDetail(card.type, card.stableId) }
            )
        }
        if (loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}