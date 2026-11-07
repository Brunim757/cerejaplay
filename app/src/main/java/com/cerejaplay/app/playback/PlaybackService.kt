package com.cerejaplay.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Cabeçalhos HTTP da fonte atual. Compartilhado entre a UI (que escolhe a
 * fonte) e o serviço de reprodução (que cria o player), já que cada provedor
 * exige Referer/User-Agent próprios.
 */
object PlaybackHeaders {
    @Volatile
    var headers: Map<String, String> = emptyMap()
}

/**
 * Cria um DataSource novo para cada mídia, aplicando os cabeçalhos vigentes
 * no momento da abertura. Assim o mesmo player serve qualquer provedor.
 */
class HeaderDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource =
        DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(PlaybackHeaders.headers)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .createDataSource()
}

/**
 * Serviço que mantém a reprodução viva em segundo plano e fornece a sessão de
 * mídia usada pelo sistema (tela de bloqueio, notificação e fones de ouvido).
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val loadControl = DefaultLoadControl.Builder()
            // Mais folga contra engasgos em HLS de provedores lentos, sem
            // atrasar o inicio da reproducao por muito tempo.
            .setBufferDurationsMs(30_000, 120_000, 2_500, 5_000)
            .build()
        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(HeaderDataSourceFactory()))
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val activityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.cerejaplay.app.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(activityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
