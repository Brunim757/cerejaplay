package com.cerejaplay.app.ui.screens

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
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.cerejaplay.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    repository: CerejaRepository,
    onOpenDetail: (String, String) -> Unit,
    onPlay: (String, String, Int, Int, String, String?) -> Unit,
    onOpenTab: (String) -> Unit
) {
    val config = LocalAppConfig.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(config.endpoints.home) {
        viewModel.load(config.endpoints.home)
    }

    if (showSettings) {
        SettingsSheet(
            preferences = config.preferences,
            settings = settings,
            legalNotice = config.legal.notice,
            onAudioSelected = viewModel::setAudio,
            onQualitySelected = viewModel::setQuality,
            onLanguageSelected = viewModel::setLanguage,
            onDismiss = { showSettings = false }
        )
    }

    val heroItems = remember(state.rows) {
        val buckets = state.rows.map { row ->
            row.items
                .filter { it.stableId.isNotBlank() && !it.backdrop.isNullOrBlank() }
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

    val catalogTabs = remember(config.tabs) {
        config.tabs.filter { it.type == "catalog" && it.mediaType != null }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading && state.rows.isEmpty() -> {
                HomeSkeleton(
                    appName = config.appName,
                    onSettings = { showSettings = true }
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
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
                                        card.title,
                                        card.poster
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
                                            onClick = {
                                                config.tabs.firstOrNull { it.type == "search" }
                                                    ?.let { onOpenTab(it.id) }
                                            }
                                        )
                                        Spacer(Modifier.width(4.dp))
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
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
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
                                item(key = "chip-search") {
                                    CategoryChip(
                                        label = "Buscar",
                                        icon = Icons.Rounded.Search,
                                        onClick = {
                                            config.tabs.firstOrNull { it.type == "search" }
                                                ?.let { onOpenTab(it.id) }
                                        }
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
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    state.history,
                                    key = { index, item -> "history-$index-${item.key}" }
                                ) { _, item ->
                                    LandscapeCard(
                                        title = item.title,
                                        subtitle = item.episodeLabel ?: "Continuar",
                                        image = item.backdrop ?: item.poster,
                                        onClick = {
                                            val seriesLike = item.type != "movie"
                                            onPlay(
                                                item.type,
                                                item.id,
                                                item.season ?: if (seriesLike) 1 else 0,
                                                item.episode ?: if (seriesLike) 1 else 0,
                                                item.title,
                                                item.poster
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
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
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    row.items,
                                    key = { index, card -> "$index:${card.type}:${card.stableId}" }
                                ) { _, card ->
                                    PosterCard(
                                        card = card,
                                        width = 132.dp,
                                        onClick = { onOpenDetail(card.type, card.stableId) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    item(key = "legal") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CerejaWordmark(fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = config.legal.disclaimer,
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
            .background(Color.Black.copy(alpha = 0.32f), CircleShape)
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = androidx.compose.foundation.BorderStroke(
            0.6.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
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
    else -> null
}

@Composable
private fun HomeSkeleton(appName: String, onSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
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
                            0f to Color.Black.copy(alpha = 0.35f),
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
                    .padding(horizontal = 20.dp, vertical = 30.dp)
                    .fillMaxWidth()
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(96.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(9.dp)
                )
                Spacer(Modifier.height(14.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(12.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(13.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.64f)
                        .height(13.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(Modifier.height(18.dp))
                SkeletonBox(
                    modifier = Modifier
                        .width(154.dp)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier
                        .width(98.dp)
                        .height(36.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
        repeat(2) {
            Spacer(Modifier.height(10.dp))
            SkeletonBox(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(190.dp)
                    .height(24.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    PosterCardSkeleton(modifier = Modifier.width(112.dp))
                }
            }
        }
    }
}
