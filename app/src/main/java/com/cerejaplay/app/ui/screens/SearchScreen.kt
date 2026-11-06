package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.model.TabConfig
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.EmptyState
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.PosterCard
import com.cerejaplay.app.ui.components.PosterCardSkeleton
import com.cerejaplay.app.ui.components.SectionHeader
import com.cerejaplay.app.ui.components.SkeletonBox
import com.cerejaplay.app.ui.viewmodel.SearchViewModel

private val searchSuggestions = listOf(
    "Round 6",
    "The Last of Us",
    "Attack on Titan",
    "Wandinha",
    "Duna",
    "Arcane",
    "Demon Slayer",
    "Stranger Things"
)

@Composable
fun SearchScreen(
    repository: CerejaRepository,
    endpoint: String,
    onOpenDetail: (String, String) -> Unit,
    onOpenTab: (String) -> Unit = {}
) {
    val config = LocalAppConfig.current
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(repository))
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val categories = remember(config.tabs) {
        config.tabs.filter { it.type == "catalog" && it.mediaType != null }
    }

    LaunchedEffect(endpoint) {
        viewModel.configure(endpoint)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.statusBarsPadding()) {
            CerejaTopBar(showBrand = true)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Buscar",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Filmes, séries e animes em um só lugar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp),
            placeholder = {
                Text(
                    text = "Digite o nome de um título...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Limpar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.submit()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        when {
            state.loading && state.total == 0 -> SearchSkeleton()
            state.error != null && state.total == 0 -> ErrorState(
                message = state.error ?: "",
                onRetry = viewModel::retry
            )
            !state.searched -> SearchDiscovery(
                recents = recents,
                categories = categories,
                onSuggestion = viewModel::useSuggestion,
                onClearRecents = viewModel::clearRecents,
                onOpenTab = onOpenTab
            )
            state.total == 0 -> EmptyState("Nada encontrado para \"$query\". Tente outro título.")
            else -> SearchResults(
                query = query,
                movies = state.groups.movies,
                series = state.groups.series,
                animes = state.groups.animes,
                onOpenDetail = onOpenDetail
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchDiscovery(
    recents: List<String>,
    categories: List<TabConfig>,
    onSuggestion: (String) -> Unit,
    onClearRecents: () -> Unit,
    onOpenTab: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 34.dp)
    ) {
        if (recents.isNotEmpty()) {
            item(key = "recent-header") {
                SectionHeader(
                    title = "Buscas recentes",
                    actionLabel = "Limpar",
                    onAction = onClearRecents
                )
            }
            item(key = "recent-chips") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recents.forEach { recent ->
                        RecentChip(
                            label = recent,
                            onClick = { onSuggestion(recent) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item(key = "suggestions-header") {
            SectionHeader(
                title = "Em alta agora",
                subtitle = "Sugestões para começar"
            )
        }
        item(key = "suggestion-chips") {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                searchSuggestions.forEach { suggestion ->
                    SuggestionChip(suggestion, onClick = { onSuggestion(suggestion) })
                }
            }
        }

        if (categories.isNotEmpty()) {
            item(key = "categories-header") {
                SectionHeader(
                    title = "Navegue por categoria",
                    subtitle = "Vá direto ao que você gosta"
                )
            }
            item(key = "categories-row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { category ->
                        CategoryTile(
                            tab = category,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenTab(category.id) }
                        )
                    }
                }
            }
        }

        item(key = "search-tip") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Text(
                        text = "Dica: use o botão de busca do teclado para salvar o termo nas buscas recentes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CategoryTile(
    tab: TabConfig,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icon = when (tab.mediaType) {
        "movie" -> Icons.Rounded.Movie
        "series" -> Icons.Rounded.Tv
        else -> Icons.Rounded.AutoAwesome
    }
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(50.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = tab.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private enum class ResultFilter(val label: String) {
    All("Tudo"),
    Movies("Filmes"),
    Series("Séries"),
    Animes("Animes")
}

@Composable
private fun SearchResults(
    query: String,
    movies: List<MediaCard>,
    series: List<MediaCard>,
    animes: List<MediaCard>,
    onOpenDetail: (String, String) -> Unit
) {
    var filter by remember(query) { mutableStateOf(ResultFilter.All) }
    val all = remember(movies, series, animes) { movies + series + animes }
    val visible = remember(all, filter) {
        when (filter) {
            ResultFilter.All -> all
            ResultFilter.Movies -> movies
            ResultFilter.Series -> series
            ResultFilter.Animes -> animes
        }
    }
    val total = all.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "result-summary") {
            Column {
                Text(
                    text = "$total resultados para \"$query\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultFilter.entries.forEach { option ->
                        val count = when (option) {
                            ResultFilter.All -> all.size
                            ResultFilter.Movies -> movies.size
                            ResultFilter.Series -> series.size
                            ResultFilter.Animes -> animes.size
                        }
                        if (option == ResultFilter.All || count > 0) {
                            ResultChip(
                                label = "${option.label} ($count)",
                                active = filter == option,
                                onClick = { filter = option }
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(
            visible,
            key = { index, card -> "result-$index-${card.type}:${card.stableId}" }
        ) { _, card ->
            PosterCard(
                card = card,
                width = null,
                onClick = { onOpenDetail(card.type, card.stableId) }
            )
        }
    }
}

@Composable
private fun ResultChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
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
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun SearchSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(180.dp)
                .height(20.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                PosterCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))
        SkeletonBox(
            modifier = Modifier
                .width(130.dp)
                .height(20.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                PosterCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
    }
}
