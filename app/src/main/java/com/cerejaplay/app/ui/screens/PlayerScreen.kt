package com.cerejaplay.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cerejaplay.app.data.model.Stream
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.data.resolve.ResolvedStream
import com.cerejaplay.app.data.resolve.StreamResolver
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.LoadingState
import com.cerejaplay.app.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    repository: CerejaRepository,
    type: String,
    id: String,
    season: Int,
    episode: Int,
    title: String,
    poster: String?,
    onBack: () -> Unit
) {
    val config = LocalAppConfig.current
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSources by remember { mutableStateOf(false) }

    LaunchedEffect(type, id, season, episode, config.endpoints.watch) {
        viewModel.load(
            endpoint = config.endpoints.watch,
            type = type,
            id = id,
            season = season.takeIf { it > 0 },
            episode = episode.takeIf { it > 0 },
            title = title,
            poster = poster
        )
    }

    if (showSources && state.watch != null) {
        SourcesSheet(
            streams = state.watch?.streams ?: emptyList(),
            selected = state.selected,
            onSelect = {
                viewModel.selectStream(it)
                showSources = false
            },
            onRefresh = {
                showSources = false
                viewModel.retry()
            },
            onDismiss = { showSources = false }
        )
    }

    BackHandler(onBack = onBack)

    val activity = context as? Activity
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null) {
                val controller = WindowInsetsControllerCompat(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            if (originalOrientation != null) activity?.requestedOrientation = originalOrientation
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.loading -> {
                LoadingState(message = "Localizando a melhor fonte...")
            }

            state.watch == null -> {
                ErrorState(
                    message = state.error ?: "Não foi possível carregar o vídeo.",
                    onRetry = viewModel::retry
                )
            }

            state.selected == null -> {
                ErrorState(
                    message = state.error ?: "Nenhuma fonte compatível com o player nativo para este título.",
                    onRetry = viewModel::retry
                )
            }

            else -> {
                DirectPlayer(
                    stream = state.selected!!,
                    autoplay = config.player.autoplay,
                    onError = viewModel::onPlaybackError,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        PlayerTopBar(
            title = state.watch?.title ?: title,
            subtitle = episodeLabel(type, state.watch?.season, state.watch?.episode, state.watch?.episodeLabel),
            onBack = onBack,
            onSources = { showSources = true }
        )

        if (state.error != null && state.watch != null && state.selected != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = viewModel::dismissError) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    onSources: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onSources) {
                Icon(Icons.Rounded.FormatListBulleted, contentDescription = "Fontes", tint = Color.White)
            }
        }
    }
}

@Composable
private fun DirectPlayer(
    stream: Stream,
    autoplay: Boolean,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var buffering by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }

    var resolved by remember(stream) {
        mutableStateOf(
            stream.url?.takeIf { stream.isDirect }?.let {
                ResolvedStream(
                    url = it,
                    headers = stream.headers,
                    quality = stream.quality,
                    isHls = stream.type == "hls"
                )
            }
        )
    }
    var resolving by remember(stream) { mutableStateOf(stream.type == "resolver") }
    var resolveError by remember(stream) { mutableStateOf<String?>(null) }

    LaunchedEffect(stream) {
        if (stream.type != "resolver") {
            resolving = false
            return@LaunchedEffect
        }
        resolving = true
        resolveError = null
        resolved = null
        runCatching { StreamResolver.resolve(stream) }
            .onSuccess { resolved = it }
            .onFailure {
                val message = it.message ?: "Não foi possível preparar esta fonte."
                resolveError = message
                onError(message)
            }
        resolving = false
    }

    val media = resolved
    val exoPlayer = remember(media?.url, media?.headers, stream.type) {
        val target = media ?: return@remember null
        runCatching {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(target.headers)
                .setAllowCrossProtocolRedirects(true)

            val itemBuilder = MediaItem.Builder().setUri(Uri.parse(target.url))
            val subtitleConfigs = stream.subtitles.mapNotNull { subtitle ->
                val url = subtitle.url ?: return@mapNotNull null
                val mime = when {
                    url.contains(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    url.contains(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                    url.contains(".ass", ignoreCase = true) -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.TEXT_VTT
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                    .setLabel(subtitle.label)
                    .setLanguage(subtitle.language ?: "und")
                    .setMimeType(mime)
                    .build()
            }
            if (subtitleConfigs.isNotEmpty()) itemBuilder.setSubtitleConfigurations(subtitleConfigs)

            val mediaItem = itemBuilder.build()
            val isHls = media.isHls ||
                stream.type == "hls" ||
                target.url.contains(".m3u8", ignoreCase = true)
            val source = if (isHls) {
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }

            ExoPlayer.Builder(context).build().apply {
                setMediaSource(source)
                prepare()
                playWhenReady = autoplay
            }
        }.getOrNull()
    }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer
        if (player == null) return@LaunchedEffect
        while (true) {
            runCatching {
                position = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    LaunchedEffect(showControls, isPlaying, buffering) {
        if (showControls && isPlaying && !buffering) {
            delay(3500)
            showControls = false
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                ended = playbackState == Player.STATE_ENDED
                if (playbackState == Player.STATE_READY) buffering = false
            }

            override fun onPlayerError(error: PlaybackException) {
                onError(error.message ?: "Falha ao reproduzir esta fonte.")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    when {
        resolving -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Preparando o vídeo...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        resolveError != null -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "Não foi possível preparar esta fonte. Toque em Fontes para escolher outra.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        exoPlayer == null -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "Não foi possível abrir esta fonte. Toque em Fontes para escolher outra.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .pointerInput(Unit) {
                        detectTapGestures { showControls = !showControls }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { view -> view.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )

                if (buffering && !ended) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(46.dp)
                    )
                }

                if (showControls) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.65f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.78f)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(26.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ControlButton(Icons.Rounded.Replay10, "Voltar 10s") {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                showControls = true
                            }
                            IconButton(
                                onClick = {
                                    if (ended) {
                                        exoPlayer.seekTo(0L)
                                        exoPlayer.play()
                                        ended = false
                                    } else if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                    showControls = true
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = if (ended) Icons.Rounded.Replay else if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            ControlButton(Icons.Rounded.Forward10, "Avançar 10s") {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(duration.coerceAtLeast(0L)))
                                showControls = true
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(position),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Slider(
                                value = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                                onValueChange = { fraction ->
                                    if (duration > 0) position = (fraction * duration).toLong()
                                },
                                onValueChangeFinished = {
                                    exoPlayer.seekTo(position)
                                    showControls = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourcesSheet(
    streams: List<Stream>,
    selected: Stream?,
    onSelect: (Stream) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Fontes disponíveis",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRefresh) {
                    Text("Atualizar", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            streams.forEach { stream ->
                val active = stream.url == selected?.url &&
                    stream.embed == selected?.embed &&
                    stream.type == selected?.type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(stream) }
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stream.label ?: stream.server ?: stream.provider,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = sourceSubtitle(stream),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (active) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = "Se uma fonte falhar, o app tenta a próxima automaticamente.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

private fun sourceSubtitle(stream: Stream): String {
    val parts = mutableListOf<String>()
    when {
        stream.isDirect -> parts.add(if (stream.type == "hls") "HLS" else "MP4")
        stream.type == "resolver" -> parts.add(if (stream.resolver == "vixsrc") "HLS" else "MP4")
        else -> parts.add("Embed")
    }
    stream.quality?.let { parts.add(it) }
    stream.audio?.let {
        parts.add(
            when (it) {
                "dub" -> "Dublado"
                "sub" -> "Legendado"
                "original" -> "Original"
                else -> it
            }
        )
    }
    parts.add(stream.provider)
    return parts.joinToString(" • ")
}

private fun episodeLabel(type: String, season: Int?, episode: Int?, label: String?): String? {
    if (!label.isNullOrBlank()) return label
    return when {
        type == "series" && season != null && episode != null -> "Temporada $season • Episódio $episode"
        type == "anime" && episode != null -> "Episódio $episode"
        else -> null
    }
}

private fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0) return "0:00"
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}