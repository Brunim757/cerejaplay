package com.cerejaplay.app.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cerejaplay.app.data.model.MediaDetail
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.CerejaWordmark
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.SectionHeader
import com.cerejaplay.app.ui.components.SkeletonBox
import com.cerejaplay.app.ui.components.cleanMediaTitle
import com.cerejaplay.app.ui.components.typeLabel
import com.cerejaplay.app.ui.theme.CondensedDisplay
import com.cerejaplay.app.ui.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    repository: CerejaRepository,
    type: String,
    id: String,
    onBack: () -> Unit,
    onPlay: (season: Int, episode: Int, title: String, poster: String?, backdrop: String?) -> Unit
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
                    isFavorite = state.isFavorite,
                    onSelectSeason = viewModel::selectSeason,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onPlay = onPlay
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)
                    )
                )
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(10.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.46f))
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
    isFavorite: Boolean,
    onSelectSeason: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onPlay: (Int, Int, String, String?, String?) -> Unit
) {
    val title = detail.title.cleanMediaTitle()
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
                    ?: detail.totalEpisodes.takeIf { it in 1..80 }
                    ?: 24
                (1..count.coerceAtLeast(1)).toList()
            }
            else -> emptyList()
        }
    }
    val episodeTitles = remember(detail) {
        detail.episodes.associate { it.number to it.title }
    }
    var synopsisExpanded by remember(detail.id) { mutableStateOf(false) }
    val hasLongSynopsis = detail.overview.length > 240

    fun playEpisode(number: Int) {
        onPlay(
            if (isSeries) selectedSeason else if (isAnime) 1 else 0,
            if (playsEpisodes) number else 0,
            title,
            detail.poster,
            detail.backdrop
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item(key = "hero") {
            DetailHero(
                detail = detail,
                title = title,
                isFavorite = isFavorite,
                playsEpisodes = playsEpisodes,
                isSeries = isSeries,
                selectedSeason = selectedSeason,
                onPlayFirst = { playEpisode(1) },
                onToggleFavorite = onToggleFavorite
            )
        }

        if (detail.genres.isNotEmpty()) {
            item(key = "genres") {
                GenresSection(genres = detail.genres)
            }
        }

        item(key = "synopsis") {
            SynopsisSection(
                detail = detail,
                expanded = synopsisExpanded,
                canExpand = hasLongSynopsis,
                onToggle = { synopsisExpanded = !synopsisExpanded }
            )
        }

        if (isSeries && detail.seasons.isNotEmpty()) {
            item(key = "seasons-header") {
                SectionHeader(
                    title = "Temporadas",
                    subtitle = "${detail.seasons.size} temporadas disponíveis"
                )
            }
            item(key = "seasons") {
                SeasonsRow(
                    detail = detail,
                    selectedSeason = selectedSeason,
                    onSelectSeason = onSelectSeason
                )
            }
        }

        if (playsEpisodes) {
            item(key = "episodes-header") {
                SectionHeader(
                    title = "Episódios",
                    subtitle = when {
                        isAnime -> "${episodeNumbers.size} episódios disponíveis"
                        else -> "Temporada $selectedSeason • ${episodeNumbers.size} episódios"
                    }
                )
            }
            if (episodeNumbers.isEmpty()) {
                item(key = "no-episodes") {
                    Text(
                        text = "Nenhum episódio disponível.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            } else {
                itemsIndexed(
                    episodeNumbers,
                    key = { index, number -> "ep-$selectedSeason-$index-$number" }
                ) { _, number ->
                    EpisodeRow(
                        number = number,
                        title = episodeTitles[number],
                        subtitle = when {
                            isSeries -> "Temporada $selectedSeason • Episódio $number"
                            else -> "Episódio $number"
                        },
                        image = detail.backdrop ?: detail.poster,
                        onClick = { playEpisode(number) }
                    )
                }
            }
        }

        item(key = "about") {
            AboutSection(detail = detail, episodeCount = episodeNumbers.size)
        }

        item(key = "brand") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                CerejaWordmark(fontSize = 11.sp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailHero(
    detail: MediaDetail,
    title: String,
    isFavorite: Boolean,
    playsEpisodes: Boolean,
    isSeries: Boolean,
    selectedSeason: Int,
    onPlayFirst: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(510.dp)
    ) {
        AsyncImage(
            model = detail.backdrop ?: detail.poster,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.50f),
                        0.30f to Color.Transparent,
                        0.56f to Color.Transparent,
                        0.88f to MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.46f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypePill(type = detail.type)
                if (detail.rating > 0.0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = Color(0xFFF6C453),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", detail.rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "HD",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.62f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                detail.year?.let {
                    MetaText(it.toString())
                    MetaDot()
                }
                if (detail.runtime != null && detail.runtime > 0) {
                    MetaText("${detail.runtime} min")
                    MetaDot()
                }
                if (isSeries && detail.seasons.isNotEmpty()) {
                    MetaText("${detail.seasons.size} temporadas")
                    MetaDot()
                }
                if (playsEpisodes) {
                    val count = detail.episodes.size.takeIf { it > 0 } ?: detail.totalEpisodes
                    if (count > 0) {
                        MetaText("$count episódios")
                        MetaDot()
                    }
                }
                MetaText(typeLabel(detail.type).lowercase().replaceFirstChar { it.uppercase() })
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPlayFirst,
                    modifier = Modifier
                        .weight(1.35f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0A0A0E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = when {
                            isSeries -> "Assistir T$selectedSeason E1"
                            playsEpisodes -> "Assistir episódio 1"
                            else -> "Assistir agora"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(
                        0.7.dp,
                        if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        else Color.White.copy(alpha = 0.20f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(onClick = onToggleFavorite)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isFavorite) "Na lista" else "Minha lista",
                            color = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresSection(genres: List<String>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.take(8).forEach { genre ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
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

@Composable
private fun SynopsisSection(
    detail: MediaDetail,
    expanded: Boolean,
    canExpand: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(94.dp)
                .height(141.dp)
                .shadow(14.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = detail.poster,
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize()
        ) {
            Text(
                text = "Sinopse",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = detail.overview.ifBlank { "Sinopse não disponível para este título." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis
            )
            if (canExpand) {
                TextButton(
                    onClick = onToggle,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (expanded) "Ler menos" else "Ler mais",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonsRow(
    detail: MediaDetail,
    selectedSeason: Int,
    onSelectSeason: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        detail.seasons.forEach { item ->
            val selected = selectedSeason == item.number
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                border = if (selected) null else BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                ),
                modifier = Modifier.clickable { onSelectSeason(item.number) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Temporada ${item.number}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.episodeCount?.let { "$it episódios" } ?: "Ver episódios",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    number: Int,
    title: String?,
    subtitle: String,
    image: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(116.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.10f),
                            1f to Color.Black.copy(alpha = 0.64f)
                        )
                    )
            )
            Text(
                text = number.toString(),
                fontFamily = CondensedDisplay,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 2.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF0A0A0E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title?.takeIf { it.isNotBlank() } ?: "Episódio $number",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Reproduzir episódio $number",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AboutSection(detail: MediaDetail, episodeCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 22.dp)
    ) {
        Text(
            text = "Ficha técnica",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                InfoRow(
                    icon = Icons.Rounded.Movie,
                    label = "Tipo",
                    value = typeLabel(detail.type).lowercase().replaceFirstChar { it.uppercase() }
                )
                if (!detail.originalTitle.isNullOrBlank() && detail.originalTitle != detail.title) {
                    InfoRow(
                        icon = Icons.Rounded.Translate,
                        label = "Título original",
                        value = detail.originalTitle
                    )
                }
                if (!detail.status.isNullOrBlank()) {
                    InfoRow(
                        icon = Icons.Rounded.LiveTv,
                        label = "Situação",
                        value = statusLabel(detail.status)
                    )
                }
                detail.year?.let {
                    InfoRow(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Lançamento",
                        value = it.toString()
                    )
                }
                if (detail.runtime != null && detail.runtime > 0) {
                    InfoRow(
                        icon = Icons.Rounded.Schedule,
                        label = "Duração",
                        value = "${detail.runtime} min"
                    )
                }
                if (detail.seasons.size > 1) {
                    InfoRow(
                        icon = Icons.Rounded.LiveTv,
                        label = "Temporadas",
                        value = detail.seasons.size.toString()
                    )
                }
                if (episodeCount > 0) {
                    InfoRow(
                        icon = Icons.Rounded.PlayArrow,
                        label = "Episódios",
                        value = episodeCount.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TypePill(type: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    ) {
        Text(
            text = typeLabel(type),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MetaText(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.84f)
    )
}

@Composable
private fun MetaDot() {
    Text(
        text = "  •  ",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.36f)
    )
}

private fun statusLabel(status: String?): String = when (status?.lowercase()) {
    "ended", "finished" -> "Finalizado"
    "returning series", "returning" -> "Em exibição"
    "canceled", "cancelled" -> "Cancelado"
    "in production" -> "Em produção"
    "planned" -> "Planejado"
    else -> status ?: "—"
}

@Composable
private fun DetailSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(510.dp)
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
                    .padding(horizontal = 18.dp, vertical = 22.dp)
                    .fillMaxWidth()
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(56.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(12.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBox(
                        modifier = Modifier
                            .weight(1.35f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    SkeletonBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .width(94.dp)
                    .height(141.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(
                    modifier = Modifier
                        .width(90.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(10.dp))
                repeat(4) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(if (it == 3) 0.60f else 0.96f)
                            .height(12.dp),
                        shape = RoundedCornerShape(5.dp)
                    )
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}
