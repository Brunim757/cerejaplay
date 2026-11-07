package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.model.UserSettings
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.CerejaWordmark
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.HeroCarousel
import com.cerejaplay.app.ui.components.LandscapeCard
import com.cerejaplay.app.ui.components.PosterCard
import com.cerejaplay.app.ui.components.PosterCardSkeleton
import com.cerejaplay.app.ui.components.SectionHeader
import com.cerejaplay.app.ui.components.SettingsSheet
import com.cerejaplay.app.ui.components.SkeletonBox
import com.cerejaplay.app.ui.components.TopRankCard
import com.cerejaplay.app.ui.components.cleanMediaTitle
import com.cerejaplay.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: CerejaRepository,
    onOpenDetail: (String, String) -> Unit,
    onPlay: (String, String, Int, Int, String, String?, String?) -> Unit,
    onOpenTab: (String) -> Unit,
    onOpenMyList: () -> Unit,
    settings: UserSettings = UserSettings(),
    onThemeSelected: (String) -> Unit = {},
    onProfileSelected: (String) -> Unit = {}
) {
    val config = LocalAppConfig.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(config.endpoints.home) {
        viewModel.load(config.endpoints.home)
    }

    LaunchedEffect(config.endpoints.becauseYouWatched) {
        viewModel.configureBecauseYouWatched(config.endpoints.becauseYouWatched)
    }

    if (showSettings) {
        SettingsSheet(
            preferences = config.preferences,
            settings = settings,
            legalNotice = config.legal.notice,
            onAudioSelected = viewModel::setAudio,
            onQualitySelected = viewModel::setQuality,
            onLanguageSelected = viewModel::setLanguage,
            onThemeSelected = onThemeSelected,
            onProfileSelected = onProfileSelected,
            onClearHistory = viewModel::clearHistory,
            onDismiss = { showSettings = false }
        )
    }

    val heroItems = remember(state.rows) {
        val buckets = state.rows.map { row ->
            row.items
                .filter {
                    it.stableId.isNotBlank() &&
                        (!it.backdrop.isNullOrBlank() || !it.poster.isNullOrBlank())
                }
                .take(4)
        }
        val picked = mutableListOf<MediaCard>()
        var round = 0
        while (picked.size < 6 && buckets.any { round < it.size }) {
            buckets.forEach { bucket ->
                if (round < bucket.size && picked.size < 6) picked.add(bucket[round])
            }
            round += 1
        }
        picked.distinctBy { "${it.type}:${it.stableId}" }.take(6)
    }

    val top10 = remember(state.rows) {
        state.rows
            .firstOrNull { it.mediaType == "movie" }
            ?.items
            ?.distinctBy { "${it.type}:${it.stableId}" }
            ?.take(10)
            .orEmpty()
    }

    val acclaimed = remember(state.rows) {
        state.rows
            .flatMap { it.items }
            .filter { it.rating >= 7.8 && it.stableId.isNotBlank() }
            .distinctBy { "${it.type}:${it.stableId}" }
            .sortedByDescending { it.rating }
            .take(14)
    }

    val catalogTabs = remember(config.tabs) {
        config.tabs.filter { it.type == "catalog" && it.mediaType != null }
    }
    val searchTab = remember(config.tabs) { config.tabs.firstOrNull { it.type == "search" } }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading && state.rows.isEmpty() -> {
                HomeSkeleton(
                    appName = config.appName,
                    onSettings = { showSettings = true },
                    onOpenMyList = onOpenMyList,
                    onSearch = { searchTab?.let { onOpenTab(it.id) } }
                )
            }

            state.error != null && state.rows.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        CerejaTopBar(
                            title = config.appName,
                            showBrand = true,
                            actions = {
                                RoundIconButton(
                                    icon = Icons.Rounded.Settings,
                                    description = "Preferências",
                                    onClick = { showSettings = true }
                                )
                            }
                        )
                    }
                    ErrorState(
                        message = state.error ?: "",
                        onRetry = { viewModel.load(config.endpoints.home, true) }
                    )
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 34.dp)
                ) {
                    item(key = "hero") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            HeroCarousel(
                                items = heroItems,
                                onPlay = { card ->
                                    val seriesLike = card.type != "movie"
                                    onPlay(
                                        card.type,
                                        card.stableId,
                                        if (seriesLike) 1 else 0,
                                        if (seriesLike) 1 else 0,
                                        card.title.cleanMediaTitle(),
                                        card.poster,
                                        card.backdrop
                                    )
                                },
                                onDetails = { card -> onOpenDetail(card.type, card.stableId) }
                            )
                            Column(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent)
                                        )
                                    )
                            ) {
                                CerejaTopBar(
                                    title = config.appName,
                                    showBrand = true,
                                    contentColor = Color.White,
                                    actions = {
                                        RoundIconButton(
                                            icon = Icons.Rounded.Search,
                                            description = "Buscar",
                                            onClick = { searchTab?.let { onOpenTab(it.id) } }
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        RoundIconButton(
                                            icon = Icons.Rounded.BookmarkBorder,
                                            description = "Minha lista",
                                            onClick = onOpenMyList
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        RoundIconButton(
                                            icon = Icons.Rounded.Settings,
                                            description = "Preferências",
                                            onClick = { showSettings = true }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (catalogTabs.isNotEmpty()) {
                        item(key = "category-chips") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                            ) {
                                itemsIndexed(catalogTabs, key = { _, tab -> "chip-${tab.id}" }) { _, tab ->
                                    CategoryChip(
                                        label = tab.title,
                                        icon = when (tab.mediaType) {
                                            "movie" -> Icons.Rounded.Movie
                                            "series" -> Icons.Rounded.Tv
                                            "anime" -> Icons.Rounded.AutoAwesome
                                            else -> Icons.Rounded.Movie
                                        },
                                        onClick = { onOpenTab(tab.id) }
                                    )
                                }
                                item(key = "chip-my-list") {
                                    CategoryChip(
                                        label = "Minha lista",
                                        icon = Icons.Rounded.Bookmark,
                                        onClick = onOpenMyList
                                    )
                                }
                            }
                        }
                    }

                    if (config.features.continueWatching && state.history.isNotEmpty()) {
                        item(key = "history-header") {
                            SectionHeader(
                                title = "Continuar assistindo",
                                subtitle = "Retome de onde parou"
                            )
                        }
                        item(key = "history-row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    state.history,
                                    key = { index, item -> "history-$index-${item.key}" }
                                ) { _, item ->
                                    LandscapeCard(
                                        title = item.title.cleanMediaTitle(),
                                        subtitle = item.episodeLabel ?: "Assistir novamente",
                                        image = item.backdrop ?: item.poster,
                                        progress = item.progress,
                                        badge = if (item.progress > 0f) "CONTINUAR" else null,
                                        onClick = {
                                            val seriesLike = item.type != "movie"
                                            onPlay(
                                                item.type,
                                                item.id,
                                                item.season ?: if (seriesLike) 1 else 0,
                                                item.episode ?: if (seriesLike) 1 else 0,
                                                item.title.cleanMediaTitle(),
                                                item.poster,
                                                item.backdrop
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    if (state.favorites.isNotEmpty()) {
                        item(key = "favorites-header") {
                            SectionHeader(
                                title = "Minha lista",
                                subtitle = "${state.favorites.size} títulos salvos",
                                actionLabel = "Ver tudo",
                                onAction = onOpenMyList
                            )
                        }
                        item(key = "favorites-row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    state.favorites,
                                    key = { index, card -> "favorite-$index-${card.type}:${card.stableId}" }
                                ) { _, card ->
                                    PosterCard(
                                        card = card,
                                        width = 126.dp,
                                        onClick = { onOpenDetail(card.type, card.stableId) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    if (top10.isNotEmpty()) {
                        item(key = "top10-header") {
                            SectionHeader(
                                title = "Top 10 da semana",
                                subtitle = "Os filmes mais assistidos agora"
                            )
                        }
                        item(key = "top10-row") {
                            LazyRow(
                                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(
                                    top10,
                                    key = { index, card -> "top10-$index-${card.type}:${card.stableId}" }
                                ) { index, card ->
                                    TopRankCard(
                                        rank = index + 1,
                                        card = card,
                                        onClick = { onOpenDetail(card.type, card.stableId) }
                                    )
                                }
                            }
                        }
                    }

                    state.rows.forEach { row ->
                        val targetTab = config.tabs.firstOrNull { tab ->
                            tab.type == "catalog" && tab.mediaType == row.mediaType
                        }
                        item(key = "header-${row.id}") {
                            SectionHeader(
                                title = row.title,
                                subtitle = rowSubtitle(row.title),
                                actionLabel = if (targetTab != null) "Ver tudo" else null,
                                onAction = targetTab?.let { tab -> { onOpenTab(tab.id) } }
                            )
                        }
                        item(key = "row-${row.id}") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    row.items,
                                    key = { index, card -> "$index:${card.type}:${card.stableId}" }
                                ) { _, card ->
                                    PosterCard(
                                        card = card,
                                        width = 138.dp,
                                        onClick = { onOpenDetail(card.type, card.stableId) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    if (acclaimed.isNotEmpty()) {
                        item(key = "acclaimed-header") {
                            SectionHeader(
                                title = "Aclamados pelo público",
                                subtitle = "Notas altas em todo o catálogo"
                            )
                        }
                        item(key = "acclaimed-row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    acclaimed,
                                    key = { index, card -> "acclaimed-$index-${card.type}:${card.stableId}" }
                                ) { _, card ->
                                    PosterCard(
                                        card = card,
                                        width = 138.dp,
                                        onClick = { onOpenDetail(card.type, card.stableId) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    state.becauseRows.forEach { row ->
                        item(key = "because-header-${row.id}") {
                            SectionHeader(
                                title = row.title,
                                subtitle = "Recomendado para você"
                            )
                        }
                        item(key = "because-row-${row.id}") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    row.items,
                                    key = { index, card -> "because-$index-${card.type}:${card.stableId}" }
                                ) { _, card ->
                                    PosterCard(
                                        card = card,
                                        width = 138.dp,
                                        onClick = { onOpenDetail(card.type, card.stableId) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    item(key = "brand-footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CerejaWordmark(fontSize = 13.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(
                                text = config.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.30f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(
            0.6.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun rowSubtitle(title: String): String? = when {
    title.contains("popular", ignoreCase = true) -> "O que todo mundo está vendo"
    title.contains("alta", ignoreCase = true) -> "Em alta agora"
    title.contains("top", ignoreCase = true) -> "Seleção da semana"
    title.contains("anime", ignoreCase = true) -> "Histórias que marcaram época"
    title.contains("filme", ignoreCase = true) -> "Grandes histórias para assistir hoje"
    title.contains("série", ignoreCase = true) || title.contains("serie", ignoreCase = true) ->
        "Maratonas que valem a pena"
    else -> null
}

@Composable
private fun HomeSkeleton(
    appName: String,
    onSettings: () -> Unit,
    onOpenMyList: () -> Unit,
    onSearch: () -> Unit
) {
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
                            0f to Color.Black.copy(alpha = 0.30f),
                            1f to MaterialTheme.colorScheme.background
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
            ) {
                CerejaTopBar(
                    title = appName,
                    showBrand = true,
                    contentColor = Color.White,
                    actions = {
                        RoundIconButton(
                            icon = Icons.Rounded.Search,
                            description = "Buscar",
                            onClick = onSearch
                        )
                        Spacer(Modifier.width(6.dp))
                        RoundIconButton(
                            icon = Icons.Rounded.BookmarkBorder,
                            description = "Minha lista",
                            onClick = onOpenMyList
                        )
                        Spacer(Modifier.width(6.dp))
                        RoundIconButton(
                            icon = Icons.Rounded.Settings,
                            description = "Preferências",
                            onClick = onSettings
                        )
                    }
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 34.dp)
                    .fillMaxWidth()
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(110.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(9.dp)
                )
                Spacer(Modifier.height(14.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(38.dp),
                    shape = RoundedCornerShape(9.dp)
                )
                Spacer(Modifier.height(12.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .height(13.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .height(13.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(18.dp))
                SkeletonBox(
                    modifier = Modifier
                        .width(154.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(13.dp)
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
                        .width(98.dp)
                        .height(36.dp),
                    shape = RoundedCornerShape(13.dp)
                )
            }
        }
        repeat(2) {
            Spacer(Modifier.height(12.dp))
            SkeletonBox(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .width(190.dp)
                    .height(24.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    PosterCardSkeleton(modifier = Modifier.width(118.dp))
                }
            }
        }
    }
}
