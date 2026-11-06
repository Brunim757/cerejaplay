package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.EmptyState
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.PosterCard
import com.cerejaplay.app.ui.components.PosterCardSkeleton
import com.cerejaplay.app.ui.components.SkeletonBox
import com.cerejaplay.app.ui.viewmodel.CatalogViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CatalogScreen(
    repository: CerejaRepository,
    title: String,
    endpoint: String,
    onOpenDetail: (String, String) -> Unit,
    onOpenSearch: (() -> Unit)? = null
) {
    val viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(endpoint) {
        viewModel.start(endpoint)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.statusBarsPadding()) {
            CerejaTopBar(
                showBrand = true,
                actions = {
                    if (onOpenSearch != null) {
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
            CatalogHeader(title = title, count = state.cards.size)
        }

        when {
            state.loading && state.cards.isEmpty() -> CatalogSkeleton()
            state.error != null && state.cards.isEmpty() -> ErrorState(
                message = state.error ?: "",
                onRetry = viewModel::refresh
            )
            state.cards.isEmpty() -> EmptyState("Nenhum título disponível agora.")
            else -> CatalogGrid(
                cards = state.cards,
                loadingMore = state.loadingMore,
                endReached = state.endReached,
                error = state.error,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::refresh,
                onOpenDetail = onOpenDetail
            )
        }
    }
}

@Composable
private fun CatalogHeader(title: String, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (count > 0) "$count títulos carregados" else "Explore o catálogo completo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CatalogGrid(
    cards: List<MediaCard>,
    loadingMore: Boolean,
    endReached: Boolean,
    error: String?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Carregando mais títulos...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (error != null && cards.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text("Tentar novamente", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (endReached && cards.isNotEmpty() && !loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        text = "  Você chegou ao fim  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                PosterCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                PosterCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                PosterCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(24.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
}
