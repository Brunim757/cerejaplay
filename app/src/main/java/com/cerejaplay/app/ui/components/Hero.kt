package com.cerejaplay.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val configuration = LocalConfiguration.current
    val heroHeight = (configuration.screenHeightDp.dp * 0.70f).coerceIn(480.dp, 630.dp)

    LaunchedEffect(pagerState, items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(8_000)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
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
                                0f to Color.Black.copy(alpha = 0.62f),
                                0.28f to Color.Black.copy(alpha = 0.12f),
                                0.58f to Color.Transparent,
                                0.82f to MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.64f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f)
                        ) {
                            Text(
                                text = typeLabel(item.type),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (item.rating > 0.0) {
                            Spacer(Modifier.width(7.dp))
                            RatingBadge(item.rating)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "HD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.62f),
                            fontWeight = FontWeight.Bold
                        )
                        if (item.year != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item.year.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.80f)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = item.title.cleanMediaTitle(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.overview.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = item.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onPlay(item) },
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0A0A0E)
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(23.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "Assistir",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = Color.White.copy(alpha = 0.13f),
                            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.20f)),
                            modifier = Modifier
                                .height(48.dp)
                                .clickable { onDetails(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 17.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "Detalhes",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(items.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (selected) 22.dp else 6.dp,
                    animationSpec = tween(220),
                    label = "hero-indicator"
                )
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.32f)
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
