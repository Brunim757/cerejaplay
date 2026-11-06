package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.EmptyState
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.LoadingState
import com.cerejaplay.app.ui.components.PosterCard
import com.cerejaplay.app.ui.components.SectionHeader
import com.cerejaplay.app.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    repository: CerejaRepository,
    endpoint: String,
    onOpenDetail: (String, String) -> Unit
) {
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(repository))
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(endpoint) {
        viewModel.configure(endpoint)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.statusBarsPadding()) {
            CerejaTopBar(title = "Buscar")
        }
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Filmes, séries e animes...") },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        when {
            state.loading && state.total == 0 -> LoadingState(message = "Buscando...")
            state.error != null && state.total == 0 -> ErrorState(message = state.error ?: "", onRetry = viewModel::retry)
            !state.searched -> EmptyState("Digite pelo menos 2 letras para buscar em todo o catálogo.")
            state.total == 0 -> EmptyState("Nada encontrado para \"$query\".")
            else -> SearchResults(
                movies = state.groups.movies,
                series = state.groups.series,
                animes = state.groups.animes,
                onOpenDetail = onOpenDetail
            )
        }
    }
}

@Composable
private fun SearchResults(
    movies: List<MediaCard>,
    series: List<MediaCard>,
    animes: List<MediaCard>,
    onOpenDetail: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        group("Filmes", movies, onOpenDetail)
        group("Séries", series, onOpenDetail)
        group("Animes", animes, onOpenDetail)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.group(
    title: String,
    cards: List<MediaCard>,
    onOpenDetail: (String, String) -> Unit
) {
    if (cards.isEmpty()) return
    item(key = "header-$title") {
        Spacer(Modifier.height(10.dp))
        SectionHeader(title = "$title (${cards.size})")
    }
    item(key = "row-$title") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards, key = { "${it.type}:${it.stableId}" }) { card ->
                PosterCard(card = card, onClick = { onOpenDetail(card.type, card.stableId) })
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}