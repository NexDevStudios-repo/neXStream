package com.nuvio.app.features.xtream

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpRequestRaw
import kotlinx.serialization.json.Json

object XtreamApi {
    private val log = Logger.withTag("XtreamApi")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun normalizeBaseUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }
        return url.trimEnd('/')
    }

    suspend fun authenticate(serverUrl: String, username: String, pass: String): Result<XtreamAuthResponse> = runCatching {
        val base = normalizeBaseUrl(serverUrl)
        val url = "$base/player_api.php?username=$username&password=$pass"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) {
            error("HTTP error ${resp.status} authenticating Xtream server")
        }
        val auth = json.decodeFromString<XtreamAuthResponse>(resp.body)
        if (auth.userInfo?.status?.equals("Disabled", ignoreCase = true) == true ||
            auth.userInfo?.status?.equals("Banned", ignoreCase = true) == true ||
            auth.userInfo?.status?.equals("Expired", ignoreCase = true) == true
        ) {
            error("Xtream account status: ${auth.userInfo?.status}")
        }
        auth
    }

    suspend fun getLiveCategories(account: XtreamServerAccount): Result<List<XtreamCategory>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_live_categories"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<List<XtreamCategory>>(resp.body)
    }

    suspend fun getLiveStreams(account: XtreamServerAccount, categoryId: String? = null): Result<List<XtreamLiveStream>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_live_streams$catParam"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<List<XtreamLiveStream>>(resp.body)
    }

    suspend fun getVodCategories(account: XtreamServerAccount): Result<List<XtreamCategory>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_vod_categories"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<List<XtreamCategory>>(resp.body)
    }

    suspend fun getVodStreams(account: XtreamServerAccount, categoryId: String? = null): Result<List<XtreamVodStream>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_vod_streams$catParam"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<List<XtreamVodStream>>(resp.body)
    }

    suspend fun getSeriesCategories(account: XtreamServerAccount): Result<List<XtreamCategory>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_series_categories"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<List<XtreamCategory>>(resp.body)
    }

    suspend fun getSeries(account: XtreamServerAccount, categoryId: String? = null): Result<List<XtreamSeriesItem>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_series$catParam"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<List<XtreamSeriesItem>>(resp.body)
    }

    suspend fun getSeriesInfo(account: XtreamServerAccount, seriesId: Int): Result<XtreamSeriesInfo> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_series_info&series_id=$seriesId"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<XtreamSeriesInfo>(resp.body)
    }

    suspend fun getVodInfo(account: XtreamServerAccount, vodId: Int): Result<XtreamVodInfo> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_vod_info&vod_id=$vodId"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<XtreamVodInfo>(resp.body)
    }

    suspend fun getShortEpg(account: XtreamServerAccount, streamId: Int): Result<XtreamShortEpg> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_short_epg&stream_id=$streamId"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<XtreamShortEpg>(resp.body)
    }

    fun buildLiveStreamUrl(account: XtreamServerAccount, streamId: Int): String {
        val base = normalizeBaseUrl(account.serverUrl)
        return "$base/live/${account.username}/${account.password}/$streamId.m3u8"
    }

    fun buildVodStreamUrl(account: XtreamServerAccount, streamId: Int, containerExtension: String? = "mp4"): String {
        val base = normalizeBaseUrl(account.serverUrl)
        val ext = containerExtension?.trimStart('.')?.ifBlank { "mp4" } ?: "mp4"
        return "$base/movie/${account.username}/${account.password}/$streamId.$ext"
    }

    fun buildSeriesStreamUrl(account: XtreamServerAccount, episodeId: String, containerExtension: String? = "mp4"): String {
        val base = normalizeBaseUrl(account.serverUrl)
        val ext = containerExtension?.trimStart('.')?.ifBlank { "mp4" } ?: "mp4"
        return "$base/series/${account.username}/${account.password}/$episodeId.$ext"
    }
}
