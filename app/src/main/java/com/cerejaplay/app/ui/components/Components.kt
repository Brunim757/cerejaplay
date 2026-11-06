package com.cerejaplay.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cerejaplay.app.R
import com.cerejaplay.app.data.model.MediaCard
import com.cerejaplay.app.ui.theme.CondensedDisplay

/**
 * Marca da CerejaPlay: a cereja com play + o wordmark.
 */
@Composable
fun CerejaWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    tint: Color = Color.White
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_cereja_logo),
            contentDescription = null,
            modifier = Modifier.size((fontSize.value * 1.45f).dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "CEREJA",
            color = tint,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "PLAY",
            color = MaterialTheme.colorScheme.primary,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CerejaTopBar(
    title: String? = null,
    showBrand: Boolean = false,
    onBack: (() -> Unit)? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Voltar",
                    tint = contentColor
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        if (showBrand) {
            CerejaWordmark(tint = contentColor)
        } else {
            Text(
                text = title ?: "CerejaPlay",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (showBrand) Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onAction)
                    .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    if (rating <= 0.0) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.66f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color(0xFFF5C451),
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TypePill(type: String, modifier: Modifier = Modifier) {
    val label = when (type) {
        "movie" -> "FILME"
        "series" -> "SÉRIE"
        "anime" -> "ANIME"
        else -> "TÍTULO"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PosterCard(
    card: MediaCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 138.dp,
    showType: Boolean = true,
    onRemove: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "poster-scale"
    )
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        PosterArt(
            poster = card.poster,
            title = card.title,
            rating = card.rating,
            type = if (showType) card.type else null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = shape,
            onRemove = onRemove
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.title.cleanMediaTitle(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            minLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            card.year?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (card.year != null && card.seasons != null) {
                Text(
                    text = "  •  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            card.seasons?.takeIf { it > 0 }?.let {
                Text(
                    text = "$it temporadas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PosterArt(
    poster: String?,
    title: String,
    rating: Double,
    type: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    onRemove: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.6.dp, Color.White.copy(alpha = 0.07f), shape)
    ) {
        AsyncImage(
            model = poster,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxSize()
        )
        if (poster.isNullOrBlank()) {
            Icon(
                painter = painterResource(id = R.drawable.ic_cereja_logo),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.20f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.52f)
                    )
                )
        )
        RatingBadge(
            rating = rating,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(7.dp)
        )
        if (type != null) {
            TypePill(
                type = type,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF101016),
                modifier = Modifier.size(20.dp)
            )
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = "Remover da minha lista",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Card de ranking no estilo "Top 10": número gigante atrás do pôster.
 */
@Composable
fun TopRankCard(
    rank: Int,
    card: MediaCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(154.dp)
            .height(212.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = rank.toString(),
            fontFamily = CondensedDisplay,
            fontWeight = FontWeight.Black,
            fontSize = 124.sp,
            lineHeight = 118.sp,
            letterSpacing = (-4).sp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = 6.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 2.dp)
                .width(105.dp)
                .aspectRatio(2f / 3f)
                .graphicsLayer {
                    shadowElevation = 10.dp.toPx()
                    shape = RoundedCornerShape(13.dp)
                    clip = true
                }
        ) {
            PosterArt(
                poster = card.poster,
                title = card.title,
                rating = card.rating,
                type = null,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(13.dp)
            )
        }
    }
}

@Composable
fun LandscapeCard(
    title: String,
    subtitle: String?,
    image: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 246.dp,
    progress: Float = 0f,
    badge: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .width(width)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(width * 0.5625f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.6.dp, Color.White.copy(alpha = 0.07f), shape)
        ) {
            AsyncImage(
                model = image,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.62f)
                        )
                    )
            )
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(9.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(9.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF101016),
                    modifier = Modifier.size(20.dp)
                )
            }
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
    )
}

@Composable
fun PosterCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(8.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(12.dp),
            shape = RoundedCornerShape(5.dp)
        )
        Spacer(Modifier.height(5.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.44f)
                .height(9.dp),
            shape = RoundedCornerShape(5.dp)
        )
    }
}

@Composable
fun LoadingState(message: String = "Carregando...", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    title: String = "Não foi possível carregar"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Tentar novamente", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Nada por aqui",
    icon: ImageVector = Icons.Rounded.SearchOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Os catálogos costumam trazer sufixos de SEO no título ("Dublado",
 * "Todos os Episódios Online", etc.). Eles não fazem parte do nome da obra.
 */
fun String.cleanMediaTitle(): String {
    val cleaned = this
        .replace(
            Regex(
                """\s*[-–—|:]\s*(todos os epis[oó]dios.*|assistir.*|online.*|dublado.*|legendado.*)$""",
                RegexOption.IGNORE_CASE
            ),
            ""
        )
        .replace(
            Regex(
                """\s*\((?:dublado|legendado|online|completo|todos os epis[oó]dios[^)]*)\)\s*""",
                RegexOption.IGNORE_CASE
            ),
            " "
        )
        .replace(
            Regex(
                """\s+(?:dublado|legendado|online|completo|todos os epis[oó]dios(?: online)?)\s*$""",
                RegexOption.IGNORE_CASE
            ),
            ""
        )
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', '–', '—', '|', ':')
    return cleaned.ifBlank { this.trim() }
}
