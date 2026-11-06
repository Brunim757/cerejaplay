package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.HeroCarousel
import com.cerejaplay.app.ui.components.LandscapeCard
import com.cerejaplay.app.ui.components.LoadingState
import com.cerejaplay.app.ui.components.PosterCard
import com.cerejaplay.app.ui.components.SectionHeader
import com.cerejaplay.app.ui.components.SettingsSheet
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
            row.items.filter { it.stableId.isNotBlank() && !it.backdrop.isNullOrBlank() }.take(4)
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

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading && state.rows.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        CerejaTopBar(title = config.appName, showBrand = true)
                    }
                    LoadingState(message = "Preparando seu catálogo...")
                }
            }

            state.error != null && state.rows.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        CerejaTopBar(title = config.appName, showBrand = true)
                    }
                    ErrorState(message = state.error ?: "", onRetry = { viewModel.load(config.endpoints.home, true) })
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
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
                            Column(modifier = Modifier.statusBarsPadding()) {
                                CerejaTopBar(
                                    title = config.appName,
                                    showBrand = true,
                                    actions = {
                                        IconButton(onClick = { showSettings = true }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = "Preferências",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (config.features.continueWatching && state.history.isNotEmpty()) {
                        item(key = "history-header") {
                            SectionHeader(title = "Continuar assistindo")
                        }
                        item(key = "history-row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(state.history, key = { index, item -> "history-$index-${item.key}" }) { _, item ->
                                    LandscapeCard(
                                        title = item.title,
                                        subtitle = item.episodeLabel,
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
                        }
                    }

                    state.rows.forEach { row ->
                        val targetTab = config.tabs.firstOrNull { tab ->
                            tab.type == "catalog" && tab.mediaType != null && tab.mediaType == row.mediaType
                        }
                        item(key = "header-${row.id}") {
                            SectionHeader(
                                title = row.title,
                                actionLabel = if (targetTab != null) "Ver tudo" else null,
                                onAction = targetTab?.let { tab -> { onOpenTab(tab.id) } }
                            )
                        }
                        item(key = "row-${row.id}") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(row.items, key = { index, card -> "$index:${card.type}:${card.stableId}" }) { _, card ->
                                    PosterCard(
                                        card = card,
                                        onClick = { onOpenDetail(card.type, card.stableId) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                        }
                    }

                    item(key = "legal") {
                        Text(
                            text = config.legal.disclaimer,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                }
        }
    }
}