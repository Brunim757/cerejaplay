package com.cerejaplay.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cerejaplay.app.data.model.AudioChoice
import com.cerejaplay.app.data.model.Stream
import com.cerejaplay.app.data.model.Subtitle
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.data.resolve.ResolvedStream
import com.cerejaplay.app.data.resolve.StreamResolver
import com.cerejaplay.app.playback.PlaybackHeaders
import com.cerejaplay.app.playback.PlaybackService
import com.cerejaplay.app.ui.LocalAppConfig
import com.cerejaplay.app.ui.components.ErrorState
import com.cerejaplay.app.ui.components.LoadingState
import com.cerejaplay.app.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val NEXT_EPISODE_WINDOW_MS = 8_000L
private const val NEXT_EPISODE_COUNTDOWN = 8

@Composable
fun PlayerScreen(
    repository: CerejaRepository,
    type: String,
    id: String,
    season: Int,
    episode: Int,
    title: String,
    poster: String?,
    backdrop: String?,
    onBack: () -> Unit,
    onNextEpisode: ((Int) -> Unit)? = null
) {
    val config = LocalAppConfig.current
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSources by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showAudio by remember { mutableStateOf(false) }
    var showSubtitles by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var speed by remember { mutableFloatStateOf(1f) }
    var locked by remember { mutableStateOf(false) }
    // null = legendas desativadas; índice dentro de stream.subtitles (apenas os
    // que possuem URL válida).
    var subtitleIndex by remember { mutableStateOf<Int?>(null) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(type, id, season, episode, config.endpoints.watch) {
        viewModel.load(
            endpoint = config.endpoints.watch,
            type = type,
            id = id,
            season = season.takeIf { it > 0 },
            episode = episode.takeIf { it > 0 },
            title = title,
            poster = poster,
            backdrop = backdrop
        )
    }

    // Ao trocar de fonte/stream, reinicia a legenda escolhida.
    LaunchedEffect(state.selected?.url, state.selected?.embed, state.selected?.type) {
        subtitleIndex = null
    }

    val subtitleOptions = remember(state.selected) {
        state.selected?.subtitles?.filter { !it.url.isNullOrBlank() }.orEmpty()
    }

    if (showSources && state.watch != null) {
        SourcesSheet(
            streams = state.visibleStreams,
            selected = state.selected,
            requestedAudio = state.requestedAudio,
            unstableCount = state.unstableCount,
            showUnstable = state.showUnstable,
            onToggleUnstable = viewModel::toggleUnstable,
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

    // Fallback de áudio: nunca tocamos dublado (ou original) sem o usuário
    // confirmar. Quando o áudio pedido não existe, aparece este aviso.
    if (state.audioNotice != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissAudioFallback,
            title = { Text("Áudio diferente do escolhido") },
            text = { Text(state.audioNotice ?: "") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmAudioFallback) {
                    Text("Assistir mesmo assim")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAudioFallback) {
                    Text("Escolher fonte")
                }
            }
        )
    }

    if (showSpeed) {
        SpeedSheet(
            selected = speed,
            onSelect = {
                speed = it
                showSpeed = false
            },
            onDismiss = { showSpeed = false }
        )
    }

    if (showAudio) {
        AudioSheet(
            options = config.preferences.audioOptions,
            selected = settings.audio,
            onSelect = {
                viewModel.setAudio(it)
                showAudio = false
            },
            onDismiss = { showAudio = false }
        )
    }

    if (showSubtitles) {
        SubtitleSheet(
            subtitles = subtitleOptions,
            selected = subtitleIndex,
            onSelect = {
                subtitleIndex = it
                showSubtitles = false
            },
            onDismiss = { showSubtitles = false }
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

    val enterPip: () -> Unit = {
        val target = context as? Activity
        if (target != null) {
            runCatching {
                val params = PictureInPictureParams.Builder()
                    .apply {
                        if (videoWidth > 0 && videoHeight > 0) {
                            setAspectRatio(Rational(videoWidth, videoHeight))
                        }
                    }
                    .build()
                target.enterPictureInPictureMode(params)
            }
        }
    }

    val watchEpisode = state.watch?.episode ?: episode.takeIf { it > 0 }
    val episodeCount = state.watch?.episodeCount
    val canGoNext = onNextEpisode != null &&
        watchEpisode != null &&
        episodeCount != null &&
        watchEpisode > 0 &&
        watchEpisode < episodeCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.loading -> {
                LoadingState(message = "Testando as melhores fontes...")
            }

            state.watch == null -> {
                ErrorState(
                    message = state.error ?: "Não foi possível carregar o vídeo.",
                    onRetry = viewModel::retry
                )
            }

            state.selected == null -> {
                ErrorState(
                    message = state.error
                        ?: if (state.audioNotice != null) {
                            "Só encontrei este título em outro áudio. Confirme no aviso ou escolha outra fonte."
                        } else {
                            "Nenhuma fonte compatível com o player nativo para este título."
                        },
                    onRetry = viewModel::retry
                )
            }

            else -> {
                DirectPlayer(
                    stream = state.selected!!,
                    mediaTitle = state.watch?.title ?: title,
                    mediaArtwork = backdrop ?: poster,
                    autoplay = config.player.autoplay,
                    startPositionMs = state.startPositionMs,
                    startSpeed = speed,
                    controlsVisible = showControls,
                    onControlsVisibleChange = { showControls = it },
                    resize = resizeMode,
                    locked = locked,
                    onLockedChange = { locked = it },
                    subtitleIndex = subtitleIndex,
                    intro = state.watch?.intro,
                    outro = state.watch?.outro,
                    hasNextEpisode = canGoNext,
                    nextCountdown = NEXT_EPISODE_COUNTDOWN,
                    onNextEpisode = {
                        watchEpisode?.let { next -> onNextEpisode?.invoke(next + 1) }
                    },
                    onProgress = viewModel::saveProgress,
                    onStarted = viewModel::onPlaybackStarted,
                    onError = viewModel::onPlaybackError,
                    onVideoSize = { width, height ->
                        videoWidth = width
                        videoHeight = height
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // A barra superior só aparece junto com os controles, para não ficar
        // cobrindo o vídeo o tempo todo. Em estados de erro/carregamento ela
        // permanece para dar acesso ao botão Voltar.
        if ((state.selected == null || showControls) && !locked) {
            PlayerTopBar(
                title = state.watch?.title ?: title,
                subtitle = episodeLabel(type, state.watch?.season, state.watch?.episode, state.watch?.episodeLabel),
                speedLabel = formatSpeed(speed),
                resizeLabel = resizeModeLabel(resizeMode),
                hasSubtitles = subtitleOptions.isNotEmpty(),
                subtitlesOff = subtitleIndex == null,
                onBack = onBack,
                onSpeed = { showSpeed = true },
                onAudio = { showAudio = true },
                onSubtitles = { showSubtitles = true },
                onResize = { resizeMode = nextResizeMode(resizeMode) },
                onPip = enterPip,
                onLock = {
                    locked = true
                    showControls = false
                },
                onSources = { showSources = true }
            )
        }

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
    speedLabel: String,
    resizeLabel: String,
    hasSubtitles: Boolean,
    subtitlesOff: Boolean,
    onBack: () -> Unit,
    onSpeed: () -> Unit,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    onResize: () -> Unit,
    onPip: () -> Unit,
    onLock: () -> Unit,
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
            if (hasSubtitles) {
                PlayerPill(
                    label = if (subtitlesOff) "Legenda" else "Legenda on",
                    icon = Icons.Rounded.Subtitles,
                    description = "Escolher legenda",
                    onClick = onSubtitles
                )
            }
            PlayerPill(
                label = resizeLabel,
                icon = Icons.Rounded.AspectRatio,
                description = "Ajuste de tela",
                onClick = onResize
            )
            PlayerPill(
                label = speedLabel,
                icon = Icons.Rounded.Speed,
                description = "Velocidade de reprodução",
                onClick = onSpeed
            )
            PlayerPill(
                label = "Áudio",
                icon = Icons.Rounded.VolumeUp,
                description = "Escolher áudio",
                onClick = onAudio
            )
            IconButton(onClick = onPip) {
                Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Picture-in-picture", tint = Color.White)
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Rounded.Lock, contentDescription = "Travar controles", tint = Color.White)
            }
            IconButton(onClick = onSources) {
                Icon(Icons.Rounded.FormatListBulleted, contentDescription = "Fontes", tint = Color.White)
            }
        }
    }
}

private data class DragHint(val icon: ImageVector, val label: String)

@Composable
private fun DirectPlayer(
    stream: Stream,
    mediaTitle: String?,
    mediaArtwork: String?,
    autoplay: Boolean,
    startPositionMs: Long,
    startSpeed: Float,
    controlsVisible: Boolean,
    onControlsVisibleChange: (Boolean) -> Unit,
    resize: Int,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    subtitleIndex: Int?,
    intro: com.cerejaplay.app.data.model.SkipInterval?,
    outro: com.cerejaplay.app.data.model.SkipInterval?,
    hasNextEpisode: Boolean,
    nextCountdown: Int,
    onNextEpisode: () -> Unit,
    onProgress: (Long, Long) -> Unit,
    onStarted: (Stream) -> Unit,
    onError: (Stream, String) -> Unit,
    onVideoSize: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    }
    val originalBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }

    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(startPositionMs) }
    var duration by remember { mutableLongStateOf(0L) }
    var buffering by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    var volume by remember {
        mutableFloatStateOf(
            audioManager
                ?.let { it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume }
                ?: 0.5f
        )
    }
    var brightness by remember {
        mutableFloatStateOf(if (originalBrightness in 0f..1f) originalBrightness else 0.5f)
    }
    var dragHint by remember { mutableStateOf<DragHint?>(null) }
    var showNext by remember { mutableStateOf(false) }
    var nextDismissed by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(nextCountdown) }

    val showControls = controlsVisible
    fun setShowControls(value: Boolean) = onControlsVisibleChange(value)

    fun applyVolume(value: Float) {
        val manager = audioManager ?: return
        val level = (value * maxVolume).roundToInt().coerceIn(0, maxVolume)
        runCatching { manager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0) }
    }

    fun applyBrightness(value: Float) {
        val window = activity?.window ?: return
        runCatching {
            window.attributes = window.attributes.apply { screenBrightness = value }
        }
    }

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
    // Fonte que esta de fato no player agora. Usada para saber de quem e um
    // erro de reproducao: um erro atrasado da fonte antiga nao pode derrubar
    // a fonte nova que o usuario acabou de escolher.
    var activeMediaStream by remember { mutableStateOf<Stream?>(null) }
    var hasAppliedMedia by remember { mutableStateOf(false) }
    // Avisa a API (uma vez por fonte) que a reprodução realmente começou.
    var notifiedStarted by remember(stream) { mutableStateOf(false) }

    LaunchedEffect(stream) {
        if (stream.type != "resolver") {
            resolving = false
            return@LaunchedEffect
        }
        resolving = true
        resolveError = null
        resolved = null
        try {
            resolved = StreamResolver.resolve(stream)
        } catch (cancelled: CancellationException) {
            // Trocar de fonte cancela este efeito: nao e falha de reproducao.
            throw cancelled
        } catch (error: Exception) {
            val message = error.message ?: "Não foi possível preparar esta fonte."
            resolveError = message
            onError(stream, message)
        } finally {
            resolving = false
        }
    }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var controllerError by remember { mutableStateOf<String?>(null) }

    // Conecta ao serviço de reprodução. O player vive no serviço para que os
    // controles de tela de bloqueio/notificação do sistema funcionem.
    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { mediaController = it }
                    .onFailure {
                        controllerError = it.message ?: "Não foi possível iniciar o player."
                    }
            },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            mediaController = null
            runCatching { MediaController.releaseFuture(future) }
        }
    }

    val media = resolved
    val exoPlayer: MediaController? = mediaController

    LaunchedEffect(media?.url, media?.headers, stream.type, startPositionMs, exoPlayer) {
        val target = media ?: return@LaunchedEffect
        val player = exoPlayer ?: return@LaunchedEffect
        // Ao trocar de fonte dentro da mesma tela, continua de onde o usuario
        // ja estava. Na primeira midia (episodio novo), usa a posicao salva.
        val resumeAt = runCatching {
            if (hasAppliedMedia && player.currentMediaItem != null && player.currentPosition > 0L) {
                player.currentPosition
            } else {
                startPositionMs
            }
        }.getOrDefault(startPositionMs)
        runCatching {
            // Cancela downloads/erros pendentes da fonte anterior antes de
            // montar a nova, evitando que um erro atrasado troque a fonte boa.
            activeMediaStream = stream
            player.stop()
            PlaybackHeaders.headers = target.headers
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
            val isHls = media.isHls ||
                stream.type == "hls" ||
                target.url.contains(".m3u8", ignoreCase = true)
            if (isHls) itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            val metadata = MediaMetadata.Builder()
                .setTitle(mediaTitle)
                .setArtist("CerejaPlay")
                .apply { mediaArtwork?.let { setArtworkUri(Uri.parse(it)) } }
                .build()
            itemBuilder.setMediaMetadata(metadata)
            player.setMediaItem(itemBuilder.build(), resumeAt)
            player.setPlaybackSpeed(startSpeed)
            applyAudioPreference(player, stream)
            player.prepare()
            player.playWhenReady = autoplay
            hasAppliedMedia = true
        }
    }

    val currentSubtitleIndex = rememberUpdatedState(subtitleIndex)
    val latestOnError = rememberUpdatedState(onError)
    val latestOnStarted = rememberUpdatedState(onStarted)
    val latestOnProgress = rememberUpdatedState(onProgress)
    val latestOnVideoSize = rememberUpdatedState(onVideoSize)
    val latestHasNextEpisode = rememberUpdatedState(hasNextEpisode)
    val latestNextDismissed = rememberUpdatedState(nextDismissed)

    LaunchedEffect(startSpeed, exoPlayer) {
        exoPlayer?.setPlaybackSpeed(startSpeed)
    }

    LaunchedEffect(subtitleIndex, exoPlayer) {
        exoPlayer?.let { applySubtitleSelection(it, subtitleIndex) }
    }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer
        if (player == null) return@LaunchedEffect
        var lastSaved = position
        while (true) {
            runCatching {
                position = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.coerceAtLeast(0L)
                // Salva a cada 5 segundos de reprodução real. Isso permite
                // retomar exatamente de onde parou mesmo sem sair do player.
                if (player.isPlaying && duration > 0L && kotlin.math.abs(position - lastSaved) >= 5_000L) {
                    onProgress(position, duration)
                    lastSaved = position
                }
                // Dispara o cartão de "próximo episódio" perto do fim.
                if (hasNextEpisode && !nextDismissed && duration > 0L) {
                    val remaining = duration - position
                    if (remaining in 1..NEXT_EPISODE_WINDOW_MS) showNext = true
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(showControls, isPlaying, buffering, locked) {
        if (showControls && isPlaying && !buffering && !locked) {
            delay(3500)
            setShowControls(false)
        }
    }

    LaunchedEffect(locked) {
        if (locked) setShowControls(false)
    }

    LaunchedEffect(showNext) {
        if (!showNext) return@LaunchedEffect
        countdown = nextCountdown
        while (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
        onNextEpisode()
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window
            if (window != null && originalBrightness != -1f) {
                runCatching {
                    window.attributes = window.attributes.apply { screenBrightness = originalBrightness }
                }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing && !notifiedStarted) {
                    val source = activeMediaStream
                    if (source != null) {
                        notifiedStarted = true
                        latestOnStarted.value(source)
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                latestOnVideoSize.value(videoSize.width, videoSize.height)
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                applySubtitleSelection(player, currentSubtitleIndex.value)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                ended = playbackState == Player.STATE_ENDED
                if (playbackState == Player.STATE_READY) {
                    buffering = false
                    player.setPlaybackSpeed(startSpeed)
                    applySubtitleSelection(player, currentSubtitleIndex.value)
                }
                if (playbackState == Player.STATE_ENDED) {
                    val total = player.duration.coerceAtLeast(0L)
                    if (total > 0L) latestOnProgress.value(total, total)
                    if (latestHasNextEpisode.value && !latestNextDismissed.value) showNext = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val source = activeMediaStream ?: return
                latestOnError.value(
                    source,
                    error.message ?: "Falha ao reproduzir esta fonte."
                )
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            val total = runCatching { player.duration }.getOrDefault(0L)
            val current = runCatching { player.currentPosition }.getOrDefault(0L)
            if (total > 0L && current > 0L) latestOnProgress.value(current, total)
            // O player vive no serviço; ao sair da tela paramos a reprodução e
            // liberamos a mídia, deixando o serviço ocioso.
            runCatching { player.stop() }
            runCatching { player.clearMediaItems() }
        }
    }

    val activeSkip: Pair<Long, String>? = run {
        val ms = position
        val introActive = intro != null &&
            ms >= (intro.start * 1000).toLong() &&
            ms < (intro.end * 1000).toLong()
        val outroActive = outro != null &&
            ms >= (outro.start * 1000).toLong() &&
            ms < (outro.end * 1000).toLong()
        when {
            introActive -> ((intro!!.end * 1000).toLong() + 500L) to "Pular abertura"
            outroActive -> ((outro!!.end * 1000).toLong() + 500L) to "Pular encerramento"
            else -> null
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

        media == null -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "Não foi possível abrir esta fonte. Toque em Fontes para escolher outra.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        exoPlayer == null -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                if (controllerError != null) {
                    Text(
                        text = controllerError ?: "Não foi possível iniciar o player. Volte e tente novamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .pointerInput(exoPlayer, duration, locked) {
                        if (locked) return@pointerInput
                        detectTapGestures(
                            onTap = { setShowControls(!showControls) },
                            onDoubleTap = { offset ->
                                val forward = offset.x > size.width / 2f
                                val delta = if (forward) 10_000L else -10_000L
                                val target = (exoPlayer.currentPosition + delta)
                                    .coerceIn(0L, duration.coerceAtLeast(0L))
                                exoPlayer.seekTo(target)
                                position = target
                                setShowControls(true)
                            }
                        )
                    }
                    .pointerInput(exoPlayer, duration, locked) {
                        if (locked) return@pointerInput
                        var mode = 0 // 0 = indefinido, 1 = seek, 2 = volume/brilho
                        var accumX = 0f
                        var accumY = 0f
                        var startPosition = 0L
                        var seekTarget = 0L
                        var startVolume = 0f
                        var startBrightness = 0f
                        var verticalStartX = 0f
                        detectDragGestures(
                            onDragStart = { offset ->
                                mode = 0
                                accumX = 0f
                                accumY = 0f
                                startPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                                seekTarget = startPosition
                                startVolume = volume
                                startBrightness = brightness
                                verticalStartX = offset.x
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumX += dragAmount.x
                                accumY += dragAmount.y
                                if (mode == 0) {
                                    if (abs(accumX) > abs(accumY) && abs(accumX) > 14f) mode = 1
                                    else if (abs(accumY) > abs(accumX) && abs(accumY) > 14f) mode = 2
                                }
                                if (mode == 1) {
                                    val width = size.width.coerceAtLeast(1)
                                    // Arrastar a largura inteira equivale a ~2 min.
                                    val deltaMs = (accumX / width) * 120_000f
                                    seekTarget = (startPosition + deltaMs.toLong())
                                        .coerceIn(0L, duration.coerceAtLeast(0L))
                                    position = seekTarget
                                    dragHint = DragHint(
                                        Icons.Rounded.Forward10,
                                        "${formatTime(seekTarget)} / ${formatTime(duration)}"
                                    )
                                    setShowControls(true)
                                } else if (mode == 2) {
                                    val height = size.height.coerceAtLeast(1)
                                    val fraction = accumY / height
                                    if (verticalStartX < size.width / 2f) {
                                        brightness = (startBrightness - fraction).coerceIn(0.02f, 1f)
                                        applyBrightness(brightness)
                                        dragHint = DragHint(
                                            Icons.Rounded.Brightness6,
                                            "Brilho ${(brightness * 100).roundToInt()}%"
                                        )
                                    } else {
                                        volume = (startVolume - fraction).coerceIn(0f, 1f)
                                        applyVolume(volume)
                                        dragHint = DragHint(
                                            if (volume <= 0f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                            "Volume ${(volume * 100).roundToInt()}%"
                                        )
                                    }
                                }
                            },
                            onDragEnd = {
                                if (mode == 1 && duration > 0L) {
                                    exoPlayer.seekTo(seekTarget)
                                    position = seekTarget
                                    setShowControls(true)
                                }
                                dragHint = null
                                mode = 0
                            },
                            onDragCancel = {
                                dragHint = null
                                mode = 0
                            }
                        )
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = resize
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                        if (view.resizeMode != resize) view.resizeMode = resize
                    },
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

                dragHint?.let { hint ->
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = hint.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = hint.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                activeSkip?.let { (target, label) ->
                    if (!locked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.68f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(end = 18.dp, bottom = 88.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    exoPlayer.seekTo(target)
                                    position = target
                                    setShowControls(true)
                                }
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                if (locked) {
                    IconButton(
                        onClick = {
                            onLockedChange(false)
                            setShowControls(true)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(12.dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = "Destravar controles", tint = Color.White)
                    }
                }

                if (showNext && hasNextEpisode && !locked) {
                    NextEpisodeCard(
                        countdown = countdown,
                        onPlayNow = {
                            showNext = false
                            onNextEpisode()
                        },
                        onDismiss = {
                            nextDismissed = true
                            showNext = false
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 18.dp, bottom = 76.dp)
                    )
                }

                if (showControls && !locked) {
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
                                setShowControls(true)
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
                                    setShowControls(true)
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
                                setShowControls(true)
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
                                    setShowControls(true)
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
private fun NextEpisodeCard(
    countdown: Int,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.82f)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "A seguir em ${countdown}s",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Próximo episódio",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onPlayNow) {
                Text("Assistir", color = Color.White, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Dispensar", tint = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

private fun applySubtitleSelection(player: Player, index: Int?) {
    runCatching {
        val builder = player.trackSelectionParameters.buildUpon()
        if (index == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        } else {
            val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            if (index in groups.indices) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                builder.setOverrideForType(
                    TrackSelectionOverride(groups[index].mediaTrackGroup, 0)
                )
            }
        }
        player.trackSelectionParameters = builder.build()
    }
}

/**
 * Prefere a faixa de áudio certa quando a fonte carrega mais de um idioma:
 * japonês/original em fontes legendadas e português em fontes dubladas. Se a
 * faixa preferida não existir, o ExoPlayer mantém a padrão (sem tela preta).
 * Não sobrescreve a escolha manual porque roda só ao aplicar a mídia.
 */
private fun applyAudioPreference(player: Player, stream: Stream) {
    val preferred = when (stream.audio?.lowercase()) {
        "dub" -> "pt"
        "original", "sub", "leg" -> "ja"
        else -> null
    } ?: return
    runCatching {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setPreferredAudioLanguage(preferred)
            .build()
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
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

@Composable
private fun PlayerPill(
    label: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.38f),
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(
    selected: Float,
    onSelect: (Float) -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = "Velocidade de reprodução",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { option ->
                val active = kotlin.math.abs(option - selected) < 0.01f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(option) }
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = formatSpeed(option),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleSheet(
    subtitles: List<Subtitle>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = "Legendas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            SubtitleRow(
                label = "Desativadas",
                active = selected == null,
                icon = Icons.Rounded.VolumeOff,
                onClick = { onSelect(null) }
            )
            subtitles.forEachIndexed { index, subtitle ->
                SubtitleRow(
                    label = subtitle.label.ifBlank { "Legenda ${index + 1}" },
                    active = selected == index,
                    icon = Icons.Rounded.Subtitles,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun SubtitleRow(
    label: String,
    active: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (active) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioSheet(
    options: List<AudioChoice>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val choices = options.ifEmpty {
        listOf(
            AudioChoice("dub", "Dublado"),
            AudioChoice("sub", "Legendado"),
            AudioChoice("original", "Original")
        )
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = "Áudio preferido",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "O app busca fontes nesse idioma ao reproduzir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            choices.forEach { choice ->
                val active = choice.id == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(choice.id) }
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = choice.label.ifBlank { choice.id },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourcesSheet(
    streams: List<Stream>,
    selected: Stream?,
    requestedAudio: String?,
    unstableCount: Int,
    showUnstable: Boolean,
    onToggleUnstable: () -> Unit,
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
                .heightIn(max = 460.dp)
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
            Text(
                text = "${streams.size} fonte(s) • toque para trocar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (unstableCount > 0) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onToggleUnstable,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (showUnstable) {
                            "Ocultar $unstableCount fonte(s) instável(is)"
                        } else {
                            "Mostrar $unstableCount fonte(s) instável(is)"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(count = streams.size) { index ->
                    val stream = streams[index]
                    val active = stream.url == selected?.url &&
                        stream.embed == selected?.embed &&
                        stream.type == selected?.type
                    val mismatched = !stream.matchesAudio(requestedAudio)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                stream.audioBadge?.let { badge ->
                                    AudioBadge(badge)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = stream.label ?: stream.server ?: stream.provider,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = sourceSubtitle(stream),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!stream.isAlive) {
                            Text(
                                text = "instável",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        if (mismatched) {
                            Text(
                                text = "outro áudio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp)
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
                }
            }
            Text(
                text = "Se uma fonte falhar, o app tenta a próxima automaticamente. Role para ver todas.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun AudioBadge(label: String) {
    val color = when (label) {
        "DUB" -> Color(0xFF3B82F6)
        "LEG" -> Color(0xFF22C55E)
        "ORIG" -> Color(0xFFEAB308)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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

private fun formatSpeed(speed: Float): String {
    return when (speed) {
        0.5f -> "0,5x"
        0.75f -> "0,75x"
        1f -> "1x"
        1.25f -> "1,25x"
        1.5f -> "1,5x"
        1.75f -> "1,75x"
        2f -> "2x"
        else -> "${speed}x"
    }
}

// Alterna o modo de ajuste da imagem. "Ajustar" mostra o vídeo inteiro (pode
// sobrar tarja quando a proporção da fonte difere da tela); "Zoom" preenche a
// tela cortando as bordas; "Preencher" estica. Assim cada provedor pode ser
// exibido do jeito que fica melhor no aparelho.
private fun nextResizeMode(current: Int): Int = when (current) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

private fun resizeModeLabel(mode: Int): String = when (mode) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Ajustar"
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Preencher"
    else -> "Ajustar"
}
