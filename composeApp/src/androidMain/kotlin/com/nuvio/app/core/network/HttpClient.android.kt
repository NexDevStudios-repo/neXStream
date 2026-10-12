package com.nuvio.app.core.network

import android.content.Context
import com.nuvio.app.core.diagnostics.SentryNetworkBreadcrumbInterceptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.network_empty_response_body
import nuvio.composeapp.generated.resources.network_request_failed_http
import org.jetbrains.compose.resources.getString
import okhttp3.Cache
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Proxy
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.text.Charsets
import java.util.concurrent.TimeUnit

object AppHttpClientProvider {
    private const val cacheSizeBytes = 50L * 1024L * 1024L
    private var client = buildAppHttpClient()

    fun initialize(context: Context) {
        if (client.cache != null) return
        client = buildAppHttpClient(
            cache = Cache(
                directory = File(context.cacheDir, "http_cache"),
                maxSize = cacheSizeBytes,
            ),
        )
    }

    fun get(): OkHttpClient = client
}

private fun buildAppHttpClient(cache: Cache? = null): OkHttpClient =
    OkHttpClient.Builder()
        .dns(IPv4FirstDns())
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(SentryNetworkBreadcrumbInterceptor())
        .proxy(Proxy.NO_PROXY)
        .apply {
            if (cache != null) {
                cache(cache)
            }
        }
        .build()

private data class LimitedReadResult(
    val bytes: ByteArray,
    val truncated: Boolean,
)

private fun requestAllowsBody(method: String): Boolean =
    when (method.uppercase()) {
        "POST", "PUT", "PATCH", "DELETE" -> true
        else -> false
    }

private fun Map<String, String>.withoutAcceptEncoding(): Map<String, String> =
    entries
        .filterNot { (key, _) -> key.equals("Accept-Encoding", ignoreCase = true) }
        .associate { (key, value) -> key to value }

private fun Map<String, String>.getHeaderIgnoreCase(name: String): String? =
    entries.firstOrNull { (key, _) -> key.equals(name, ignoreCase = true) }?.value

private fun readAtMostBytes(stream: InputStream, maxBytes: Int): LimitedReadResult {
    val out = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
    val buffer = ByteArray(8 * 1024)
    var remaining = maxBytes
    var truncated = false

    while (remaining > 0) {
        val read = stream.read(buffer, 0, minOf(buffer.size, remaining))
        if (read <= 0) break
        out.write(buffer, 0, read)
        remaining -= read
    }

    if (remaining == 0) {
        truncated = stream.read() != -1
    }

    return LimitedReadResult(out.toByteArray(), truncated)
}

private fun readResponseBody(body: ResponseBody?): String {
    if (body == null) return ""
    val bytes = body.bytes()
    return runCatching {
        val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        String(bytes, charset)
    }.getOrElse {
        String(bytes, Charsets.UTF_8)
    }
}

private suspend fun executeTextRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: String = "",
): String = withContext(Dispatchers.IO) {
    val normalizedMethod = method.uppercase()
    val sanitizedHeaders = headers.withoutAcceptEncoding()
    val builder = Request.Builder().url(url)
    sanitizedHeaders.forEach { (key, value) ->
        builder.header(key, value)
    }

    val request = if (requestAllowsBody(normalizedMethod)) {
        val contentType = sanitizedHeaders.getHeaderIgnoreCase("Content-Type")
            ?: if (normalizedMethod == "POST") "application/x-www-form-urlencoded" else "application/json"
        val requestBody = body.toByteArray(Charsets.UTF_8).toRequestBody(contentType.toMediaType())
        builder.method(normalizedMethod, requestBody)
    } else {
        builder.method(normalizedMethod, null)
    }.build()

    AppHttpClientProvider.get().newCall(request).execute().use { response ->
        val payload = readResponseBody(response.body)
        if (!response.isSuccessful) {
            error(runBlocking { getString(Res.string.network_request_failed_http, response.code) })
        }
        if (payload.isBlank()) {
            throw IllegalStateException(runBlocking { getString(Res.string.network_empty_response_body) })
        }
        payload
    }
}

actual suspend fun httpGetText(url: String): String =
    executeTextRequest(
        method = "GET",
        url = url,
        headers = mapOf("Accept" to "application/json"),
    )

actual suspend fun httpPostJson(url: String, body: String): String =
    executeTextRequest(
        method = "POST",
        url = url,
        headers = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
        ),
        body = body,
    )

actual suspend fun httpGetTextWithHeaders(
    url: String,
    headers: Map<String, String>,
): String =
    executeTextRequest(
        method = "GET",
        url = url,
        headers = mapOf("Accept" to "application/json") + headers,
    )

actual suspend fun httpPostJsonWithHeaders(
    url: String,
    body: String,
    headers: Map<String, String>,
): String =
    executeTextRequest(
        method = "POST",
        url = url,
        headers = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
        ) + headers,
        body = body,
    )

actual suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    followRedirects: Boolean,
    maxResponseBodyBytes: Int,
    bodyBytes: ByteArray?,
): RawHttpResponse =
    withContext(Dispatchers.IO) {
        val normalizedMethod = method.uppercase()
        val sanitizedHeaders = headers.withoutAcceptEncoding().toMutableMap()
        if (sanitizedHeaders.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            val customUa = com.nuvio.app.features.xtream.XtreamPreferencesRepository.state.value.userAgent
            if (customUa.isNotBlank()) {
                sanitizedHeaders["User-Agent"] = customUa
            }
        }
        val builder = Request.Builder().url(url)
        sanitizedHeaders.forEach { (key, value) ->
            builder.header(key, value)
        }

        val request = if (requestAllowsBody(normalizedMethod)) {
            val contentType = sanitizedHeaders.getHeaderIgnoreCase("Content-Type")
                ?: if (normalizedMethod == "POST") "application/x-www-form-urlencoded" else "application/json"
            val requestBody = (bodyBytes ?: body.toByteArray(Charsets.UTF_8))
                .toRequestBody(contentType.toMediaType())
            builder.method(normalizedMethod, requestBody)
        } else {
            builder.method(normalizedMethod, null)
        }.build()

        val client = if (followRedirects) {
            AppHttpClientProvider.get()
        } else {
            AppHttpClientProvider.get().newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        }

        val call = client.newCall(request)
        val cancelHandle = coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                call.cancel()
            }
        }
        try {
            call.execute().use { response ->
                val contentType = response.body?.contentType()
                val readResult = response.body?.byteStream()?.use { stream ->
                    readAtMostBytes(stream, maxResponseBodyBytes.coerceAtLeast(0))
                } ?: LimitedReadResult(ByteArray(0), truncated = false)
                val charset = contentType?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val decoded = runCatching { String(readResult.bytes, charset) }
                    .getOrElse { String(readResult.bytes, Charsets.UTF_8) }
                RawHttpResponse(
                    status = response.code,
                    statusText = response.message,
                    url = response.request.url.toString(),
                    body = if (readResult.truncated) "$decoded\n...[truncated]" else decoded,
                    bodyBytes = readResult.bytes,
                    headers = response.headers.toMultimap().mapValues { (_, values) ->
                        values.joinToString(",")
                    }.mapKeys { (name, _) ->
                        name.lowercase()
                    },
                )
            }
        } catch (error: IOException) {
            if (call.isCanceled()) throw CancellationException("Cancelled HTTP request", error)
            throw error
        } finally {
            cancelHandle?.dispose()
        }
    }
