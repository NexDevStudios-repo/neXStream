package com.nuvio.app.features.xtream

import co.touchlab.kermit.Logger
import com.nuvio.app.core.network.httpRequestRaw
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object XtreamApi {
    private val log = Logger.withTag("XtreamApi")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Normalizes an Xtream server URL by ensuring scheme and stripping any API endpoints or query parameters.
     */
    fun normalizeBaseUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.isEmpty()) return url
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }
        url = url.substringBefore("/player_api.php")
            .substringBefore("/panel_api.php")
            .substringBefore("/get.php")
            .substringBefore("/xmltv.php")
            .substringBefore("?")
        return url.trimEnd('/')
    }

    /**
     * Extracts Xtream credentials if pasted inside the URL (e.g., from get.php or player_api.php links).
     */
    fun extractCredentialsFromUrl(rawUrl: String): Triple<String, String, String>? {
        val trimmed = rawUrl.trim()
        val uriParts = trimmed.split("?", limit = 2)
        if (uriParts.size < 2) return null
        val baseUrl = normalizeBaseUrl(uriParts[0])
        val queryParams = uriParts[1].split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) parts[0].lowercase() to parts[1] else parts[0].lowercase() to ""
        }
        val user = queryParams["username"] ?: queryParams["user"]
        val pass = queryParams["password"] ?: queryParams["pass"]
        if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
            return Triple(baseUrl, user, pass)
        }
        return null
    }

    suspend fun authenticate(serverUrl: String, username: String, pass: String): Result<XtreamAuthResponse> = runCatching {
        val creds = extractCredentialsFromUrl(serverUrl)
        val finalBase = creds?.first ?: normalizeBaseUrl(serverUrl)
        val finalUser = if (username.isNotBlank()) username.trim() else creds?.second ?: ""
        val finalPass = if (pass.isNotBlank()) pass.trim() else creds?.third ?: ""

        if (finalUser.isBlank() || finalPass.isBlank()) {
            error("Debes proporcionar usuario y contraseña.")
        }

        val url = "$finalBase/player_api.php?username=$finalUser&password=$finalPass"
        log.d { "Authenticating Xtream server: $finalBase" }
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "")
        if (resp.status !in 200..299) {
            error("Error HTTP ${resp.status} al conectar al servidor Xtream.")
        }

        val element = runCatching { json.parseToJsonElement(resp.body) }.getOrElse {
            error("El servidor no devolvió una respuesta JSON válida.")
        }
        val root = element as? JsonObject
            ?: error("Respuesta no válida del servidor Xtream.")

        val userInfo = root["user_info"] as? JsonObject
            ?: error("El servidor no devolvió información de usuario (user_info).")

        val authInt = userInfo["auth"]?.jsonPrimitive?.intOrNull
            ?: userInfo["auth"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        if (authInt == 0) {
            error("Usuario o contraseña incorrectos.")
        }

        val status = userInfo["status"]?.jsonPrimitive?.contentOrNull
        if (status != null && (status.equals("Disabled", ignoreCase = true) ||
                    status.equals("Banned", ignoreCase = true) ||
                    status.equals("Expired", ignoreCase = true))) {
            error("Estado de la cuenta: $status")
        }

        val serverInfo = root["server_info"] as? JsonObject
        XtreamAuthResponse(
            userInfo = XtreamUserInfo(
                username = userInfo["username"]?.jsonPrimitive?.contentOrNull ?: finalUser,
                status = status ?: "Active",
                exp_date = userInfo["exp_date"]?.jsonPrimitive?.contentOrNull,
                is_trial = userInfo["is_trial"]?.jsonPrimitive?.contentOrNull,
                active_cons = userInfo["active_cons"]?.jsonPrimitive?.contentOrNull,
                max_connections = userInfo["max_connections"]?.jsonPrimitive?.contentOrNull,
            ),
            serverInfo = XtreamServerInfo(
                url = serverInfo?.get("url")?.jsonPrimitive?.contentOrNull,
                port = serverInfo?.get("port")?.jsonPrimitive?.contentOrNull,
                server_protocol = serverInfo?.get("server_protocol")?.jsonPrimitive?.contentOrNull,
                timezone = serverInfo?.get("timezone")?.jsonPrimitive?.contentOrNull,
            ),
        )
    }

    suspend fun getLiveCategories(account: XtreamServerAccount): Result<List<XtreamCategory>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_live_categories"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "", maxResponseBodyBytes = 15 * 1024 * 1024)
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        parseCategories(resp.body)
    }

    suspend fun getLiveStreams(account: XtreamServerAccount, categoryId: String? = null): Result<List<XtreamLiveStream>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_live_streams$catParam"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "", maxResponseBodyBytes = 15 * 1024 * 1024)
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        parseLiveStreams(resp.body)
    }

    suspend fun getVodCategories(account: XtreamServerAccount): Result<List<XtreamCategory>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_vod_categories"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "", maxResponseBodyBytes = 15 * 1024 * 1024)
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        parseCategories(resp.body)
    }

    suspend fun getVodStreams(account: XtreamServerAccount, categoryId: String? = null): Result<List<XtreamVodStream>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_vod_streams$catParam"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "", maxResponseBodyBytes = 15 * 1024 * 1024)
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        parseVodStreams(resp.body)
    }

    suspend fun getSeriesCategories(account: XtreamServerAccount): Result<List<XtreamCategory>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_series_categories"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "", maxResponseBodyBytes = 15 * 1024 * 1024)
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        parseCategories(resp.body)
    }

    suspend fun getSeries(account: XtreamServerAccount, categoryId: String? = null): Result<List<XtreamSeriesItem>> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val catParam = if (categoryId != null) "&category_id=$categoryId" else ""
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_series$catParam"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "", maxResponseBodyBytes = 15 * 1024 * 1024)
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        parseSeries(resp.body)
    }

    suspend fun getSeriesInfo(account: XtreamServerAccount, seriesId: Int): Result<XtreamSeriesInfo> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_series_info&series_id=$seriesId"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<XtreamSeriesInfo>(resp.body)
    }

    suspend fun getVodInfo(account: XtreamServerAccount, vodId: Int): Result<XtreamVodInfo> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_vod_info&vod_id=$vodId"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<XtreamVodInfo>(resp.body)
    }

    suspend fun getShortEpg(account: XtreamServerAccount, streamId: Int): Result<XtreamShortEpg> = runCatching {
        val base = normalizeBaseUrl(account.serverUrl)
        val url = "$base/player_api.php?username=${account.username}&password=${account.password}&action=get_short_epg&stream_id=$streamId"
        val resp = httpRequestRaw("GET", url, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "")
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
        json.decodeFromString<XtreamShortEpg>(resp.body)
    }

    fun buildLiveStreamUrl(account: XtreamServerAccount, streamId: Int, formatOverride: String? = null): String {
        val base = normalizeBaseUrl(account.serverUrl)
        val ext = formatOverride ?: XtreamPreferencesRepository.state.value.outputFormat.ifBlank { "m3u8" }
        return "$base/live/${account.username}/${account.password}/$streamId.$ext"
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

    private fun parseCategories(body: String): List<XtreamCategory> {
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["category_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["category_name"]?.jsonPrimitive?.contentOrNull ?: id
            val parent = obj["parent_id"]?.jsonPrimitive?.intOrNull
                ?: obj["parent_id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            XtreamCategory(categoryId = id, categoryName = name, parentId = parent)
        }
    }

    private fun parseLiveStreams(body: String): List<XtreamLiveStream> {
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["stream_id"]?.jsonPrimitive?.intOrNull
                ?: obj["stream_id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Canal $id"
            val icon = obj["stream_icon"]?.jsonPrimitive?.contentOrNull
            val epg = obj["epg_channel_id"]?.jsonPrimitive?.contentOrNull
            val catId = obj["category_id"]?.jsonPrimitive?.contentOrNull
            val num = obj["num"]?.jsonPrimitive?.intOrNull
                ?: obj["num"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            XtreamLiveStream(
                streamId = id,
                name = name,
                streamIcon = icon,
                epgChannelId = epg,
                categoryId = catId,
                num = num,
            )
        }
    }

    private fun parseVodStreams(body: String): List<XtreamVodStream> {
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["stream_id"]?.jsonPrimitive?.intOrNull
                ?: obj["stream_id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Película $id"
            val icon = obj["stream_icon"]?.jsonPrimitive?.contentOrNull
            val catId = obj["category_id"]?.jsonPrimitive?.contentOrNull
            val rating = obj["rating"]?.jsonPrimitive?.contentOrNull
            val ext = obj["container_extension"]?.jsonPrimitive?.contentOrNull ?: "mp4"
            XtreamVodStream(
                streamId = id,
                name = name,
                streamIcon = icon,
                categoryId = catId,
                rating = rating,
                containerExtension = ext,
            )
        }
    }

    private fun parseSeries(body: String): List<XtreamSeriesItem> {
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["series_id"]?.jsonPrimitive?.intOrNull
                ?: obj["series_id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Serie $id"
            val cover = obj["cover"]?.jsonPrimitive?.contentOrNull
            val catId = obj["category_id"]?.jsonPrimitive?.contentOrNull
            val rating = obj["rating"]?.jsonPrimitive?.contentOrNull
            val plot = obj["plot"]?.jsonPrimitive?.contentOrNull
            XtreamSeriesItem(
                seriesId = id,
                name = name,
                cover = cover,
                categoryId = catId,
                rating = rating,
                plot = plot,
            )
        }
    }
}
