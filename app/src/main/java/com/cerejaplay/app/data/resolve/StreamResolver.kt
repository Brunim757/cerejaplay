package com.cerejaplay.app.data.resolve

import com.cerejaplay.app.data.model.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
 * O Blogger amarra a URL final do googlevideo ao IP de quem abriu o embed.
 * O VixSrc bloqueia IPs de datacenter (como o da Vercel). Nos dois casos a
 * resolução acontece aqui, com o IP do usuário, e o ExoPlayer recebe a URL
 * pronta — sem WebView e sem abrir navegador.
 */
object StreamResolver {

    private const val VIXSRC_BASE = "https://vixsrc.to"

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val googlevideoPattern =
        Regex("""https?://[^"'\s\\]*googlevideo\.com/videoplayback\?[^"'\s\\]*""")

    private val itagPattern = Regex("""[?&]itag=(\d+)""")

    private val vixsrcEmbedPattern = Regex(""""src"\s*:\s*"((?:[^"\\]|\\.)*)"""")

    private val vixsrcPlaylistPattern = Regex("""url\s*:\s*['"]([^'"]+)['"]""")

    suspend fun resolve(stream: Stream): ResolvedStream = withContext(Dispatchers.IO) {
        when (stream.resolver) {
            "blogger" -> resolveBlogger(stream)
            "vixsrc" -> resolveVixsrc(stream)
            else -> throw IOException("Esta fonte não é compatível com o player nativo.")
        }
    }

    private fun resolveBlogger(stream: Stream): ResolvedStream {
        val page = stream.embed?.takeIf { it.isNotBlank() }
            ?: throw IOException("Fonte sem endereço para preparar o vídeo.")

        val body = fetch(
            url = page,
            headers = mapOf(
                "User-Agent" to MOBILE_UA,
                "Referer" to "https://animesonlinecc.to/"
            ) + stream.headers
        )
        val plain = unescapeHtml(body)
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

    private fun resolveVixsrc(stream: Stream): ResolvedStream {
        val entry = stream.embed?.takeIf { it.isNotBlank() }
            ?: throw IOException("Fonte sem endereço para preparar o vídeo.")
        val lang = queryParam(entry, "lang") ?: "en"

        val embedUrl = if (entry.contains("/api/")) {
            val json = fetch(
                url = entry,
                headers = mapOf(
                    "User-Agent" to DESKTOP_UA,
                    "Referer" to "$VIXSRC_BASE/",
                    "Origin" to VIXSRC_BASE,
                    "Accept" to "application/json, text/plain, */*"
                ) + stream.headers
            )
            val src = vixsrcEmbedPattern.find(json)?.groupValues?.getOrNull(1)
                ?: throw IOException("O VixSrc não retornou o endereço do player.")
            absolute(unescapeJson(src), VIXSRC_BASE)
        } else {
            entry
        }

        val page = fetch(
            url = embedUrl,
            headers = mapOf(
                "User-Agent" to DESKTOP_UA,
                "Referer" to "$VIXSRC_BASE/",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            )
        )

        val masterIndex = page.indexOf("masterPlaylist")
        if (masterIndex < 0) {
            throw IOException("O VixSrc não expôs a playlist do vídeo.")
        }
        val objectStart = page.indexOf('{', masterIndex)
        val objectSource = if (objectStart >= 0) balancedObject(page, objectStart) else null
        if (objectSource == null) {
            throw IOException("O VixSrc não expôs a playlist do vídeo.")
        }

        val playlistRaw = vixsrcPlaylistPattern.find(objectSource)?.groupValues?.getOrNull(1)
            ?: throw IOException("O VixSrc não retornou a URL da playlist.")
        val playlist = absolute(playlistRaw, VIXSRC_BASE)

        val masterUrl = buildMasterUrl(
            playlist = playlist,
            token = paramValue(objectSource, "token"),
            expires = paramValue(objectSource, "expires"),
            lang = lang
        )

        return ResolvedStream(
            url = masterUrl,
            headers = mapOf(
                "User-Agent" to DESKTOP_UA,
                "Referer" to embedUrl,
                "Origin" to VIXSRC_BASE
            )
        )
    }

    private fun fetch(url: String, headers: Map<String, String>): String {
        val request = Request.Builder()
            .url(url)
            .apply {
                headers.forEach { (name, value) ->
                    if (name.isNotBlank() && value.isNotBlank()) header(name, value)
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("O servidor de vídeo respondeu ${response.code}.")
            }
            return response.body?.string()
                ?: throw IOException("O servidor de vídeo não retornou conteúdo.")
        }
    }

    private fun buildMasterUrl(playlist: String, token: String?, expires: String?, lang: String): String {
        val base = playlist.toHttpUrlOrNull()
            ?: throw IOException("O VixSrc retornou uma playlist inválida.")
        val builder = base.newBuilder()
        if (!token.isNullOrBlank()) builder.setQueryParameter("token", token)
        if (!expires.isNullOrBlank()) builder.setQueryParameter("expires", expires)
        builder.setQueryParameter("h", "1")
        builder.setQueryParameter("lang", lang)
        return builder.build().toString()
    }

    private fun queryParam(url: String, name: String): String? =
        url.toHttpUrlOrNull()?.queryParameter(name)

    private fun paramValue(source: String, name: String): String? =
        Regex("""['"]$name['"]\s*:\s*['"]([^'"]*)['"]""")
            .find(source)
            ?.groupValues
            ?.getOrNull(1)

    private fun absolute(value: String, base: String): String {
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        if (value.startsWith("//")) return "https:$value"
        return base.trimEnd('/') + "/" + value.trimStart('/')
    }

    private fun balancedObject(text: String, start: Int): String? {
        var depth = 0
        var quote: Char? = null
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (quote != null) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == quote -> quote = null
                }
                continue
            }
            when (char) {
                '\'', '"' -> quote = char
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun unescapeJson(value: String): String = value
        .replace("\\/", "/")
        .replace("\\u003d", "=")
        .replace("\\u0026", "&")
        .replace("&amp;", "&")

    private fun unescapeHtml(body: String): String = body
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
