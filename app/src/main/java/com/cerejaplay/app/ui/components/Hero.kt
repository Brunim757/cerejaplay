package com.cerejaplay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cerejaplay.app.data.model.MediaCard
import kotlinx.coroutines.delay

@Composable
fun HeroCarousel(
    items: List<MediaCard>,
    onPlay: (MediaCard) -> Unit,
    onDetails: (MediaCard) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(pagerState, items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(6500)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.backdrop ?: item.poster,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.45f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                0.6f to Color.Transparent
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 30.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.rating > 0.0) {
                            RatingBadge(item.rating)
                            Spacer(Modifier.width(8.dp))
                        }
                        if (item.year != null) {
                            Text(
                                text = item.year.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = typeLabel(item.type),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.overview.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = item.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onPlay(item) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.background
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Assistir", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { onDetails(item) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Detalhes", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

fun typeLabel(type: String): String = when (type) {
    "movie" -> "FILME"
    "series" -> "SÉRIE"
    "anime" -> "ANIME"
    else -> "TÍTULO"
}
