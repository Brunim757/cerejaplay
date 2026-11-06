package com.cerejaplay.app.data.resolve

import com.cerejaplay.app.data.model.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ResolvedStream(
    val url: String,
    val headers: Map<String, String>,
    val quality: String? = null
)

/**
 * Resolve fontes que precisam ser abertas no próprio aparelho.
 *
 * O Blogger gera a URL final do googlevideo amarrada ao IP de quem abriu o
 * embed. Por isso a resolução acontece aqui, e não no servidor da API.
 */
object StreamResolver {

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val googlevideoPattern =
        Regex("""https?://[^"'\s\\]*googlevideo\.com/videoplayback\?[^"'\s\\]*""")

    private val itagPattern = Regex("""[?&]itag=(\d+)""")

    suspend fun resolve(stream: Stream): ResolvedStream = withContext(Dispatchers.IO) {
        when (stream.resolver) {
            "blogger" -> resolveBlogger(stream)
            else -> throw IOException("Esta fonte não é compatível com o player nativo.")
        }
    }

    private fun resolveBlogger(stream: Stream): ResolvedStream {
        val page = stream.embed?.takeIf { it.isNotBlank() }
            ?: throw IOException("Fonte sem endereço para preparar o vídeo.")

        val request = Request.Builder()
            .url(page)
            .header("User-Agent", MOBILE_UA)
            .header("Referer", "https://animesonlinecc.to/")
            .apply {
                stream.headers.forEach { (name, value) ->
                    if (name.isNotBlank() && value.isNotBlank()) header(name, value)
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("O servidor de vídeo respondeu ${response.code}.")
            }
            val body = response.body?.string()
                ?: throw IOException("O servidor de vídeo não retornou conteúdo.")
            val plain = unescape(body)
            val candidates = googlevideoPattern.findAll(plain)
                .map { it.value.trimEnd(',', ';') }
                .filter { it.contains("mime=video/mp4", ignoreCase = true) }
                .distinct()
                .toList()

            if (candidates.isEmpty()) {
                throw IOException("Nenhum vídeo MP4 foi encontrado nesta fonte.")
            }

            val best = candidates.maxByOrNull { score(it) } ?: candidates.first()
            return ResolvedStream(
                url = best,
                headers = mapOf(
                    "User-Agent" to MOBILE_UA,
                    "Referer" to "https://www.blogger.com/"
                ),
                quality = qualityOf(best)
            )
        }
    }

    private fun unescape(body: String): String = body
        .replace("\\u003d", "=")
        .replace("\\u003D", "=")
        .replace("\\u0026", "&")
        .replace("\\u0026amp;", "&")
        .replace("\\/", "/")
        .replace("&amp;", "&")

    private fun score(url: String): Int {
        val itag = itagPattern.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val height = when (itag) {
            37, 38 -> 1080
            22, 84 -> 720
            59, 35, 44 -> 480
            18, 34, 43 -> 360
            6, 83 -> 270
            5, 36, 13 -> 240
            17 -> 144
            else -> 0
        }
        // Dentro da mesma resolução, prefere o arquivo de vídeo mais completo.
        val size = Regex("""[?&]clen=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        return height * 1_000_000 + (size / 1_000_000).toInt()
    }

    private fun qualityOf(url: String): String? {
        val itag = itagPattern.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        return when (itag) {
            37, 38 -> "1080p"
            22, 84 -> "720p"
            59, 35, 44 -> "480p"
            18, 34, 43 -> "360p"
            6, 83 -> "270p"
            5, 36, 13 -> "240p"
            17 -> "144p"
            else -> null
        }
    }
}