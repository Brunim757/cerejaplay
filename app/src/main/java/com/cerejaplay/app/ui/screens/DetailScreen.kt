package com.cerejaplay.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cerejaplay.app.data.model.MediaDetail
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.LoadingState
import com.cerejaplay.app.ui.components.RatingBadge
import com.cerejaplay.app.ui.components.SectionHeader
import com.cerejaplay.app.ui.components.typeLabel
import com.cerejaplay.app.ui.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    repository: CerejaRepository,
    type: String,
    id: String,
    onBack: () -> Unit,
    onPlay: (season: Int, episode: Int, title: String, poster: String?) -> Unit
) {
    val config = LocalAppConfig.current
    val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(type, id, config.endpoints.detail) {
        viewModel.load(config.endpoints.detail, type, id)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading && state.detail == null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.statusBarsPadding())
                    LoadingState(message = "Carregando detalhes...")
                }
            }

            state.error != null && state.detail == null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.statusBarsPadding())
                    ErrorState(
                        message = state.error ?: "",
                        onRetry = { viewModel.load(config.endpoints.detail, type, id, true) }
                    )
                }
            }

            state.detail != null -> {
                DetailContent(
                    detail = state.detail!!,
                    selectedSeason = state.selectedSeason,
                    onSelectSeason = viewModel::selectSeason,
                    onPlay = onPlay
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(10.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = Color.White)
        }
    }
}

@Composable
private fun DetailContent(
    detail: MediaDetail,
    selectedSeason: Int,
    onSelectSeason: (Int) -> Unit,
    onPlay: (Int, Int, String, String?) -> Unit
) {
    val chunkedEpisodes = remember(detail, selectedSeason) {
        val season = detail.seasons.firstOrNull { it.number == selectedSeason }
        val count = when {
            detail.type == "anime" -> detail.episodes.size.takeIf { it > 0 } ?: detail.totalEpisodes
            detail.type == "series" -> season?.episodeCount ?: 24
            else -> detail.totalEpisodes
        }
        (1..count.coerceAtLeast(1)).toList().chunked(5)
    }
    // O scraper de alguns animes devolve o mesmo episódio em dublado e
    // legendado. Deduplicar evita uma lista repetida e chaves duplicadas.
    val animeEpisodes = remember(detail) {
        detail.episodes
            .distinctBy { it.number }
            .sortedBy { it.number }
    }
    val isSeries = detail.type == "series"
    val isAnime = detail.type == "anime"
    val playsEpisodes = isSeries || isAnime

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
            ) {
                AsyncImage(
                    model = detail.backdrop ?: detail.poster,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.30f),
                                0.45f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp)
                ) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(detail.rating)
                        Spacer(Modifier.width(8.dp))
                        if (detail.year != null) {
                            MetaText(detail.year.toString())
                            Spacer(Modifier.width(8.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = typeLabel(detail.type),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (detail.runtime != null && detail.runtime > 0) {
                            Spacer(Modifier.width(8.dp))
                            MetaText("${detail.runtime} min")
                        }
                    }
                }
            }
        }

        item(key = "actions") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { onPlay(if (playsEpisodes) selectedSeason else 0, if (playsEpisodes) 1 else 0, detail.title, detail.poster) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (playsEpisodes) "Assistir episódio 1" else "Assistir agora",
                        fontWeight = FontWeight.Bold
                    )
                }
                if (detail.genres.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detail.genres.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "overview") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = "Sinopse",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = detail.overview.ifBlank { "Sinopse não disponível para este título." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSeries && detail.seasons.isNotEmpty()) {
            item(key = "seasons") {
                SectionHeader(title = "Temporadas")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detail.seasons.forEach { season ->
                        FilterChip(
                            selected = selectedSeason == season.number,
                            onClick = { onSelectSeason(season.number) },
                            label = { Text("T${season.number}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        if (isAnime) {
            item(key = "episodes-header") {
                SectionHeader(
                    title = "Episódios",
                    subtitle = if (detail.totalEpisodes > 0) "${detail.totalEpisodes} episódios" else null
                )
            }
            if (animeEpisodes.isEmpty()) {
                item(key = "no-episodes") {
                    Text(
                        text = "Nenhum episódio disponível.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                itemsIndexed(
                    animeEpisodes,
                    key = { index, episode -> "ep-$index-${episode.number}" }
                ) { _, episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(1, episode.number, detail.title, detail.poster) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = episode.number.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = episode.title ?: "Episódio ${episode.number}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (isSeries) {
            item(key = "episodes-header") {
                SectionHeader(title = "Episódios", subtitle = "Temporada $selectedSeason")
            }
            chunkedEpisodes.forEachIndexed { rowIndex, row ->
                item(key = "ep-row-$selectedSeason-$rowIndex") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { number ->
                            EpisodeButton(
                                number = number,
                                modifier = Modifier.weight(1f),
                                onClick = { onPlay(selectedSeason, number, detail.title, detail.poster) }
                            )
                        }
                        repeat(5 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item(key = "legal") {
            Text(
                text = com.cerejaplay.app.ui.LocalAppConfig.current.legal.disclaimer,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun EpisodeButton(number: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MetaText(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.85f)
    )
}