package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.EmptyState
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.PosterCard
import com.cerejaplay.app.ui.components.PosterCardSkeleton
import com.cerejaplay.app.ui.components.SkeletonBox
import com.cerejaplay.app.ui.components.cleanMediaTitle
import com.cerejaplay.app.ui.viewmodel.CatalogViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class CatalogFilter(val label: String) {
    Highlight("Em destaque"),
    TopRated("Melhores notas"),
    Newest("Mais recentes")
}

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

    when {
        state.loading && state.cards.isEmpty() -> CatalogSkeleton(
            onOpenSearch = onOpenSearch
        )

        state.error != null && state.cards.isEmpty() -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    CerejaTopBar(showBrand = true, actions = { SearchAction(onOpenSearch, dark = false) })
                }
                ErrorState(
                    message = state.error ?: "",
                    onRetry = viewModel::refresh
                )
            }
        }

        state.cards.isEmpty() -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    CerejaTopBar(showBrand = true, actions = { SearchAction(onOpenSearch, dark = false) })
                }
                EmptyState("Nenhum título disponível agora.")
            }
        }

        else -> CatalogGrid(
            title = title,
            cards = state.cards,
            loadingMore = state.loadingMore,
            endReached = state.endReached,
            error = state.error,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::refresh,
            onOpenDetail = onOpenDetail,
            onOpenSearch = onOpenSearch
        )
    }
}

@Composable
private fun CatalogGrid(
    title: String,
    cards: List<MediaCard>,
    loadingMore: Boolean,
    endReached: Boolean,
    error: String?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenDetail: (String, String) -> Unit,
    onOpenSearch: (() -> Unit)?
) {
    val gridState = rememberLazyGridState()
    val size by rememberUpdatedState(cards.size)
    var filter by remember { mutableStateOf(CatalogFilter.Highlight) }

    val sorted = remember(cards, filter) {
        when (filter) {
            CatalogFilter.Highlight -> cards
            CatalogFilter.TopRated -> cards.sortedByDescending { it.rating }
            CatalogFilter.Newest -> cards.sortedWith(
                compareByDescending<MediaCard> { it.year ?: 0 }.thenByDescending { it.rating }
            )
        }
    }

    LaunchedEffect(gridState, cards) {
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "catalog-hero") {
            CatalogHero(
                title = title,
                count = cards.size,
                featured = cards.firstOrNull { !it.backdrop.isNullOrBlank() } ?: cards.first(),
                onOpenSearch = onOpenSearch
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "catalog-filters") {
            CatalogFilters(
                selected = filter,
                total = cards.size,
                onSelect = { filter = it }
            )
        }

        // Índice + id: mesmo que a API repita um card, a chave nunca colide
        // e o LazyGrid não derruba o app durante o scroll.
        itemsIndexed(sorted, key = { index, card -> "$index:${card.type}:${card.stableId}" }) { _, card ->
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
private fun CatalogHero(
    title: String,
    count: Int,
    featured: MediaCard,
    onOpenSearch: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        AsyncImage(
            model = featured.backdrop ?: featured.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.52f),
                        0.40f to Color.Black.copy(alpha = 0.10f),
                        0.72f to MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CerejaTopBar(
                showBrand = true,
                contentColor = Color.White,
                actions = { SearchAction(onOpenSearch, dark = true) }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "CATÁLOGO CEREJAPLAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (count == 1) "1 título disponível" else "$count títulos disponíveis",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Destaque: ${featured.title.cleanMediaTitle()}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CatalogFilters(
    selected: CatalogFilter,
    total: Int,
    onSelect: (CatalogFilter) -> Unit
) {
    Column(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explorar",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$total títulos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CatalogFilter.entries.forEach { filter ->
                val active = filter == selected
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    border = if (active) null else BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(filter) }
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAction(onOpenSearch: (() -> Unit)?, dark: Boolean) {
    if (onOpenSearch == null) return
    IconButton(
        onClick = onOpenSearch,
        modifier = Modifier
            .size(40.dp)
            .background(
                if (dark) Color.Black.copy(alpha = 0.30f) else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Buscar",
            tint = if (dark) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CatalogSkeleton(onOpenSearch: (() -> Unit)?) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
        ) {
            SkeletonBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                CerejaTopBar(
                    showBrand = true,
                    contentColor = Color.White,
                    actions = { SearchAction(onOpenSearch, dark = true) }
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 18.dp, vertical = 20.dp)
                    .fillMaxWidth()
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(Modifier.height(9.dp))
                SkeletonBox(
                    modifier = Modifier
                        .width(190.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(9.dp))
                SkeletonBox(
                    modifier = Modifier
                        .width(150.dp)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier
                        .width(92.dp)
                        .height(34.dp),
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        PosterCardSkeleton(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
