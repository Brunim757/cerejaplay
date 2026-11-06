package com.cerejaplay.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.ui.components.CerejaTopBar
import com.cerejaplay.app.ui.components.EmptyState
import com.cerejaplay.app.ui.components.PosterCard
import kotlinx.coroutines.launch

@Composable
fun MyListScreen(
    repository: CerejaRepository,
    onBack: () -> Unit,
    onOpenDetail: (String, String) -> Unit
) {
    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.statusBarsPadding()) {
            CerejaTopBar(
                title = "Minha lista",
                onBack = onBack
            )
        }

        if (favorites.isEmpty()) {
            EmptyState(
                title = "Sua lista está vazia",
                message = "Toque no marcador em qualquer título para guardar aqui e assistir depois.",
                icon = Icons.Rounded.BookmarkBorder,
                actionLabel = "Explorar catálogo",
                onAction = onBack
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "collection-header") {
                    CollectionHeader(count = favorites.size)
                }

                itemsIndexed(
                    favorites,
                    key = { index, card -> "favorite-$index-${card.type}:${card.stableId}" }
                ) { _, card ->
                    PosterCard(
                        card = card,
                        width = null,
                        showType = true,
                        onRemove = {
                            scope.launch { repository.toggleFavorite(card) }
                        },
                        onClick = { onOpenDetail(card.type, card.stableId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionHeader(count: Int) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(
            0.6.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(
                        text = "Sua coleção",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (count == 1) "1 título salvo" else "$count títulos salvos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
