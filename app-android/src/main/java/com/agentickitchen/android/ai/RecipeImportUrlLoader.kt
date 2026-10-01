package com.agentickitchen.android.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.InetAddress
import java.net.URI

internal data class LoadedRecipeSource(
    val finalUrl: String,
    val sourceLabel: String,
    val body: String
)

internal class RecipeImportUrlLoader(
    private val client: HttpClient = defaultClient(),
    private val ownsClient: Boolean = true,
    private val hostAllowed: suspend (String) -> Boolean = ::isPublicHost
) : Closeable {
    suspend fun load(rawUrl: String): Result<LoadedRecipeSource> = runCatching {
        var current = validateUri(rawUrl)
        repeat(MAX_REDIRECTS + 1) { redirectIndex ->
            check(hostAllowed(current.host)) { "Blocked recipe URL host" }
            val response = client.get(current.toString())
            if (response.status.value in 300..399) {
                check(redirectIndex < MAX_REDIRECTS) { "Too many redirects" }
                val location = response.headers[HttpHeaders.Location] ?: error("Redirect without location")
                current = validateUri(current.resolve(location).toString())
            } else {
                check(response.status.value in 200..299) { "Recipe URL request failed" }
                val declared = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                check(declared == null || declared <= MAX_BODY_BYTES) { "Recipe page too large" }
                val body = response.bodyAsText()
                check(body.toByteArray().size <= MAX_BODY_BYTES) { "Recipe page too large" }
                return@runCatching LoadedRecipeSource(current.toString(), current.host, body)
            }
        }
        error("Too many redirects")
    }

    override fun close() {
        if (ownsClient) client.close()
    }

    companion object {
        const val MAX_BODY_BYTES = 1_500_000L
        const val MAX_REDIRECTS = 3
        const val MAX_AI_TEXT_CHARS = 45_000

        internal fun validateUri(rawUrl: String): URI {
            val uri = URI(rawUrl.trim())
            require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) { "Only HTTP(S) recipe URLs are allowed" }
            require(!uri.host.isNullOrBlank()) { "Recipe URL host is required" }
            require(uri.userInfo == null) { "Recipe URL credentials are not allowed" }
            return uri
        }

        internal fun visibleRecipeText(html: String): String = html
            .replace(Regex("""(?is)<script\b[^>]*>.*?</script>"""), " ")
            .replace(Regex("""(?is)<style\b[^>]*>.*?</style>"""), " ")
            .replace(Regex("""(?is)<[^>]+>"""), "\n")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace(Regex("""[\t ]+"""), " ")
            .replace(Regex("""\n\s*\n+"""), "\n")
            .trim()
            .take(MAX_AI_TEXT_CHARS)

        private suspend fun isPublicHost(host: String): Boolean = withContext(Dispatchers.IO) {
            if (host.equals("localhost", true) || host.endsWith(".localhost", true)) return@withContext false
            runCatching { InetAddress.getAllByName(host).all(::isPublicAddress) }.getOrDefault(false)
        }

        private fun isPublicAddress(address: InetAddress): Boolean {
            if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) return false
            val bytes = address.address
            if (bytes.size == 16 && (bytes[0].toInt() and 0xfe) == 0xfc) return false
            if (bytes.size == 4) {
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                if (first == 100 && second in 64..127) return false
                if (first == 169 && second == 254) return false
            }
            return true
        }

        private fun defaultClient() = HttpClient(OkHttp) {
            followRedirects = false
            install(HttpTimeout) {
                connectTimeoutMillis = 8_000
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
        }
    }
}
