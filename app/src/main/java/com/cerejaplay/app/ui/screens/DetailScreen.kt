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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cerejaplay.app.data.model.MediaDetail
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.RatingBadge
import com.cerejaplay.app.ui.components.SectionHeader
import com.cerejaplay.app.ui.components.SkeletonBox
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
            state.loading && state.detail == null -> DetailSkeleton()

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
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.48f))
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Voltar",
                tint = Color.White
            )
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
    // O scraper de alguns animes devolve o mesmo episódio em dublado e
    // legendado. Deduplicar evita uma lista repetida.
    val animeEpisodes = remember(detail) {
        detail.episodes
            .distinctBy { it.number }
            .sortedBy { it.number }
    }
    val season = detail.seasons.firstOrNull { it.number == selectedSeason }
    val isSeries = detail.type == "series"
    val isAnime = detail.type == "anime"
    val playsEpisodes = isSeries || isAnime

    val episodeNumbers = remember(detail, selectedSeason, animeEpisodes) {
        when {
            isAnime -> animeEpisodes.map { it.number }
            isSeries -> {
                val count = season?.episodeCount
                    ?: detail.totalEpisodes.takeIf { it in 1..60 }
                    ?: 24
                (1..count.coerceAtLeast(1)).toList()
            }
            else -> emptyList()
        }
    }
    val episodeRows = remember(episodeNumbers) {
        episodeNumbers.chunked(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item(key = "hero") {
            DetailHero(
                detail = detail,
                isSeries = isSeries,
                isAnime = isAnime,
                selectedSeason = selectedSeason,
                onPlay = onPlay
            )
        }

        if (detail.genres.isNotEmpty()) {
            item(key = "genres") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detail.genres.forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
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

        item(key = "overview") {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text(
                    text = "Sinopse",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = detail.overview.ifBlank { "Sinopse não disponível para este título." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSeries && detail.seasons.isNotEmpty()) {
            item(key = "seasons-header") {
                SectionHeader(
                    title = "Temporadas",
                    subtitle = "${detail.seasons.size} temporadas disponíveis"
                )
            }
            item(key = "seasons") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detail.seasons.forEach { item ->
                        val selected = selectedSeason == item.number
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) null else BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.clickable { onSelectSeason(item.number) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "T${item.number}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.episodeCount != null) {
                                    Text(
                                        text = "${item.episodeCount} eps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (playsEpisodes) {
            item(key = "episodes-header") {
                SectionHeader(
                    title = if (isAnime) "Episódios" else "Episódios da temporada",
                    subtitle = if (isAnime) "${episodeNumbers.size} episódios"
                    else "Temporada $selectedSeason"
                )
            }
            if (episodeRows.isEmpty()) {
                item(key = "no-episodes") {
                    Text(
                        text = "Nenhum episódio disponível.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            } else {
                episodeRows.forEachIndexed { rowIndex, row ->
                    item(key = "ep-row-$selectedSeason-$rowIndex") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { number ->
                                EpisodeButton(
                                    number = number,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onPlay(
                                            if (isSeries) selectedSeason else 1,
                                            number,
                                            detail.title,
                                            detail.poster
                                        )
                                    }
                                )
                            }
                            repeat(5 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item(key = "about") {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(
                    text = "Sobre",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                if (!detail.originalTitle.isNullOrBlank() && detail.originalTitle != detail.title) {
                    InfoRow("Título original", detail.originalTitle!!)
                }
                if (detail.status != null) InfoRow("Status", detail.status!!)
                if (detail.totalEpisodes > 0) InfoRow("Total de episódios", detail.totalEpisodes.toString())
                if (detail.runtime != null && detail.runtime > 0) InfoRow("Duração", "${detail.runtime} min")
                InfoRow("Tipo", typeLabel(detail.type).lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        item(key = "legal") {
            Text(
                text = LocalAppConfig.current.legal.disclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun DetailHero(
    detail: MediaDetail,
    isSeries: Boolean,
    isAnime: Boolean,
    selectedSeason: Int,
    onPlay: (Int, Int, String, String?) -> Unit
) {
    val playsEpisodes = isSeries || isAnime
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp)
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
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.38f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
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
                if (detail.rating > 0) Spacer(Modifier.width(8.dp))
                if (detail.year != null) {
                    MetaText(detail.year.toString())
                    Spacer(Modifier.width(8.dp))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                ) {
                    Text(
                        text = typeLabel(detail.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (detail.runtime != null && detail.runtime > 0) {
                    Spacer(Modifier.width(8.dp))
                    MetaText("${detail.runtime} min")
                }
                if (isSeries && detail.seasons.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    MetaText("${detail.seasons.size} temporadas")
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onPlay(
                        if (playsEpisodes) selectedSeason else 0,
                        if (playsEpisodes) 1 else 0,
                        detail.title,
                        detail.poster
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (playsEpisodes) "Assistir episódio 1" else "Assistir agora",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun EpisodeButton(number: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MetaText(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.86f)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(126.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(470.dp)
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
                            1f to MaterialTheme.colorScheme.background
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 18.dp, vertical = 20.dp)
                    .fillMaxWidth()
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(10.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(14.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(18.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(22.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(Modifier.height(10.dp))
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(if (it == 2) 0.62f else 0.94f)
                        .height(13.dp),
                    shape = RoundedCornerShape(5.dp)
                )
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}
