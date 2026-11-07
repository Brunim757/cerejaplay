package com.cerejaplay.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cerejaplay.app.ui.CerejaPlayRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CerejaPlayApp).container
        handleIntent(intent)
        setContent {
            CerejaPlayRoot(container = container)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (!data.scheme.equals("cerejaplay", ignoreCase = true)) return
        deepLinkRoute(data)?.let(DeepLinkBus::emit)
    }

    private fun deepLinkRoute(uri: Uri): String? {
        val segments = uri.pathSegments
        return when (uri.host) {
            "detail" -> {
                if (segments.size < 2) return null
                "detail/${Uri.encode(segments[0])}/${Uri.encode(segments[1])}"
            }
            "watch", "player" -> {
                if (segments.size < 2) return null
                val season = uri.getQueryParameter("season")?.toIntOrNull() ?: 0
                val episode = uri.getQueryParameter("episode")?.toIntOrNull() ?: 0
                "player/${Uri.encode(segments[0])}/${Uri.encode(segments[1])}/$season/$episode"
            }
            else -> null
        }
    }
}
