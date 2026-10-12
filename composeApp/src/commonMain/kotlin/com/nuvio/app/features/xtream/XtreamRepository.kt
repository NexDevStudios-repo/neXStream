package com.nuvio.app.features.xtream

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.streams.StreamItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object XtreamRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("XtreamRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _servers = MutableStateFlow<List<XtreamServerAccount>>(emptyList())
    val servers: StateFlow<List<XtreamServerAccount>> = _servers.asStateFlow()

    private val _activeServer = MutableStateFlow<XtreamServerAccount?>(null)
    val activeServer: StateFlow<XtreamServerAccount?> = _activeServer.asStateFlow()

    private val liveStreamsCache = mutableMapOf<Int, XtreamLiveStream>()
    private val vodStreamsCache = mutableMapOf<Int, XtreamVodStream>()
    private val seriesStreamsCache = mutableMapOf<Int, XtreamSeriesItem>()

    private val m3uPlaylistsCache = mutableMapOf<String, ParsedM3uPlaylist>()
    private val m3uChannelsCache = mutableMapOf<Int, ParsedM3uChannel>()
    private val drawerChannelsCache = mutableMapOf<String, List<ChannelDrawerItem>>()
    private val channelCategoryCache = mutableMapOf<String, String>()

    private var currentProfileIndex: Int = 1
    private var usesPrimaryServers: Boolean = false

    private val profileKey: String
        get() = if (currentProfileIndex <= 1 || usesPrimaryServers) "1" else currentProfileIndex.toString()

    fun initialize() {
        loadServers()
    }

    fun onProfileChanged(profileIndex: Int, usesPrimaryServers: Boolean) {
        val wasEmpty = _servers.value.isEmpty()
        val indexChanged = currentProfileIndex != profileIndex
        val primarySettingChanged = this.usesPrimaryServers != usesPrimaryServers
        currentProfileIndex = profileIndex
        this.usesPrimaryServers = usesPrimaryServers
        if (wasEmpty || indexChanged || primarySettingChanged) {
            clearCache()
            loadServers()
            com.nuvio.app.features.home.HomeRepository.refresh(force = true)
            com.nuvio.app.features.search.SearchRepository.refreshDiscover(forceRefresh = true)
        }
    }

    fun reloadServers() {
        clearCache()
        loadServers()
    }

    private fun loadServers() {
        val raw = XtreamStorage.loadServersJson(profileKey).orEmpty().trim()
        val list = if (raw.isNotEmpty()) {
            runCatching { json.decodeFromString<List<XtreamServerAccount>>(raw) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        _servers.value = list
        val activeId = XtreamStorage.loadActiveServerId(profileKey)
        val active = list.find { it.id == activeId } ?: list.firstOrNull()
        _activeServer.value = active
        if (active != null && active.id != activeId) {
            XtreamStorage.saveActiveServerId(active.id, profileKey)
        }
        if (active != null && (active.serverType == "m3u_file" || active.serverType == "m3u_url")) {
            getOrLoadM3uPlaylist(active)
        }
    }

    private fun persistServers(list: List<XtreamServerAccount>) {
        _servers.value = list
        XtreamStorage.saveServersJson(json.encodeToString(list), profileKey)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addServer(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
    ): Result<XtreamServerAccount> {
        val creds = XtreamApi.extractCredentialsFromUrl(serverUrl)
        val finalBase = creds?.first ?: XtreamApi.normalizeBaseUrl(serverUrl)
        val finalUser = if (username.isNotBlank()) username.trim() else creds?.second ?: ""
        val finalPass = if (password.isNotBlank()) password.trim() else creds?.third ?: ""

        if (finalUser.isBlank() || finalPass.isBlank()) {
            return Result.failure(IllegalArgumentException("Debes ingresar usuario y contraseña."))
        }

        val authResult = XtreamApi.authenticate(finalBase, finalUser, finalPass)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Falló la autenticación con el servidor Xtream"))
        }

        val id = Uuid.random().toString()
        val account = XtreamServerAccount(
            id = id,
            name = name.ifBlank { "Xtream Server" },
            serverUrl = finalBase,
            username = finalUser,
            password = finalPass,
            isActive = true,
            serverType = "xtream",
        )

        val updated = _servers.value + account
        persistServers(updated)
        selectServer(account.id)
        return Result.success(account)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addM3uUrl(
        name: String,
        m3uUrl: String,
    ): Result<XtreamServerAccount> {
        val trimmed = m3uUrl.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("La URL de la lista M3U no puede estar vacía."))
        }
        val credentials = M3uParser.tryExtractXtreamCredentials(trimmed)
        if (credentials != null) {
            val (serverUrl, username, password) = credentials
            return addServer(name.ifBlank { "Lista IPTV" }, serverUrl, username, password)
        }

        val resp = runCatching {
            com.nuvio.app.core.network.httpRequestRaw("GET", trimmed, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "")
        }.getOrElse { e ->
            return Result.failure(Exception("Error de conexión al descargar la lista M3U: ${e.message}"))
        }

        if (resp.status !in 200..299) {
            return Result.failure(Exception("Error HTTP ${resp.status} al descargar la lista M3U."))
        }

        val parsed = M3uParser.parse(resp.body)
        if (parsed.channels.isEmpty()) {
            return Result.failure(Exception("No se encontraron canales válidos en la lista M3U."))
        }

        val id = Uuid.random().toString()
        val account = XtreamServerAccount(
            id = id,
            name = name.ifBlank { "Lista M3U (${parsed.channels.size} canales)" },
            serverUrl = trimmed,
            username = "m3u",
            password = "m3u",
            isActive = true,
            serverType = "m3u_url",
        )

        XtreamStorage.saveM3uContent(id, resp.body)
        m3uPlaylistsCache[id] = parsed
        cacheM3uChannels(parsed.channels)

        val updated = _servers.value + account
        persistServers(updated)
        selectServer(account.id)
        return Result.success(account)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addM3uContent(
        name: String,
        content: String,
    ): Result<XtreamServerAccount> {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("El archivo M3U no puede estar vacío."))
        }
        val parsed = M3uParser.parse(trimmed)
        if (parsed.channels.isEmpty()) {
            return Result.failure(IllegalArgumentException("No se encontraron canales válidos en el archivo M3U."))
        }

        val id = Uuid.random().toString()
        val account = XtreamServerAccount(
            id = id,
            name = name.ifBlank { "Lista Local (${parsed.channels.size} canales)" },
            serverUrl = "local://m3u",
            username = "local_m3u",
            password = "local_m3u",
            isActive = true,
            serverType = "m3u_file",
        )

        XtreamStorage.saveM3uContent(id, trimmed)
        m3uPlaylistsCache[id] = parsed
        cacheM3uChannels(parsed.channels)

        val updated = _servers.value + account
        persistServers(updated)
        selectServer(account.id)
        return Result.success(account)
    }

    fun selectServer(serverId: String) {
        val server = _servers.value.find { it.id == serverId }
        _activeServer.value = server
        XtreamStorage.saveActiveServerId(server?.id, profileKey)
        clearCache()
        if (server != null && (server.serverType == "m3u_file" || server.serverType == "m3u_url")) {
            getOrLoadM3uPlaylist(server)
        }
        com.nuvio.app.features.home.HomeRepository.refresh(force = true)
        com.nuvio.app.features.search.SearchRepository.refreshDiscover(forceRefresh = true)
    }

    fun deleteServer(serverId: String) {
        XtreamStorage.deleteM3uContent(serverId)
        m3uPlaylistsCache.remove(serverId)

        val updated = _servers.value.filterNot { it.id == serverId }
        persistServers(updated)
        if (_activeServer.value?.id == serverId) {
            val next = updated.firstOrNull()
            _activeServer.value = next
            XtreamStorage.saveActiveServerId(next?.id, profileKey)
            if (next != null && (next.serverType == "m3u_file" || next.serverType == "m3u_url")) {
                getOrLoadM3uPlaylist(next)
            }
        }
        clearCache()
        com.nuvio.app.features.watchprogress.WatchProgressRepository.clearAllProgress()
        com.nuvio.app.features.library.LibraryRepository.clearAllSaved()
        com.nuvio.app.features.watched.WatchedRepository.clearAllWatched()
        com.nuvio.app.features.home.HomeRepository.refresh(force = true)
        com.nuvio.app.features.search.SearchRepository.refreshDiscover(forceRefresh = true)
    }

    fun clearCache() {
        liveStreamsCache.clear()
        vodStreamsCache.clear()
        seriesStreamsCache.clear()
        m3uChannelsCache.clear()
        drawerChannelsCache.clear()
        channelCategoryCache.clear()
    }

    private fun getOrLoadM3uPlaylist(server: XtreamServerAccount): ParsedM3uPlaylist? {
        val cached = m3uPlaylistsCache[server.id]
        if (cached != null) {
            cacheM3uChannels(cached.channels)
            return cached
        }

        val savedContent = XtreamStorage.loadM3uContent(server.id)
        if (!savedContent.isNullOrBlank()) {
            val parsed = M3uParser.parse(savedContent)
            m3uPlaylistsCache[server.id] = parsed
            cacheM3uChannels(parsed.channels)
            return parsed
        }
        return null
    }

    private fun cacheM3uChannels(channels: List<ParsedM3uChannel>) {
        for (c in channels) {
            m3uChannelsCache[c.id] = c
        }
    }

    suspend fun getLiveCategories(server: XtreamServerAccount): List<XtreamCategory> {
        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            return getOrLoadM3uPlaylist(server)?.categories ?: emptyList()
        }
        return XtreamApi.getLiveCategories(server).getOrDefault(emptyList())
    }

    suspend fun getVodCategories(server: XtreamServerAccount): List<XtreamCategory> {
        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            val playlist = getOrLoadM3uPlaylist(server) ?: return emptyList()
            val vodGroups = playlist.channels.filter { it.type == "movie" }.mapNotNull { it.groupTitle }.distinct()
            return vodGroups.mapIndexed { idx, name -> XtreamCategory(categoryId = name, categoryName = name, parentId = idx) }
        }
        return XtreamApi.getVodCategories(server).getOrDefault(emptyList())
    }

    suspend fun getSeriesCategories(server: XtreamServerAccount): List<XtreamCategory> {
        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            val playlist = getOrLoadM3uPlaylist(server) ?: return emptyList()
            val seriesGroups = playlist.channels.filter { it.type == "series" }.mapNotNull { it.groupTitle }.distinct()
            return seriesGroups.mapIndexed { idx, name -> XtreamCategory(categoryId = name, categoryName = name, parentId = idx) }
        }
        return XtreamApi.getSeriesCategories(server).getOrDefault(emptyList())
    }

    suspend fun fetchHomeCatalogSections(): List<HomeCatalogSection> {
        val server = _activeServer.value ?: return emptyList()

        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            return fetchM3uHomeSections(server)
        }

        val sections = mutableListOf<HomeCatalogSection>()
        try {
            val settings = com.nuvio.app.features.home.HomeCatalogSettingsRepository.uiState.value
            val liveCatsDeferred = if (settings.liveStreamsEnabled) {
                scope.async { XtreamApi.getLiveCategories(server).getOrDefault(emptyList()) }
            } else null
            val vodCatsDeferred = if (settings.vodEnabled) {
                scope.async { XtreamApi.getVodCategories(server).getOrDefault(emptyList()) }
            } else null
            val seriesCatsDeferred = if (settings.seriesEnabled) {
                scope.async { XtreamApi.getSeriesCategories(server).getOrDefault(emptyList()) }
            } else null

            val liveCats = liveCatsDeferred?.await()?.take(4) ?: emptyList()
            val vodCats = vodCatsDeferred?.await()?.take(5) ?: emptyList()
            val seriesCats = seriesCatsDeferred?.await()?.take(5) ?: emptyList()

            val sectionJobs = mutableListOf<suspend () -> HomeCatalogSection?>()

            for (cat in liveCats) {
                sectionJobs.add {
                    val streams = XtreamApi.getLiveStreams(server, cat.categoryId).getOrDefault(emptyList())
                    for (s in streams) {
                        liveStreamsCache[s.streamId] = s
                        registerChannelCategory("xtream:live:${s.streamId}", cat.categoryId)
                        registerChannelCategory(s.name, cat.categoryId)
                    }
                    val previews = streams.take(25).map { it.toMetaPreview() }
                    if (previews.isNotEmpty()) cat.toHomeCatalogSection(previews, "live") else null
                }
            }

            for (cat in vodCats) {
                sectionJobs.add {
                    val streams = XtreamApi.getVodStreams(server, cat.categoryId).getOrDefault(emptyList())
                    for (s in streams) vodStreamsCache[s.streamId] = s
                    val previews = streams.take(25).map { it.toMetaPreview() }
                    if (previews.isNotEmpty()) cat.toHomeCatalogSection(previews, "vod") else null
                }
            }

            for (cat in seriesCats) {
                sectionJobs.add {
                    val items = XtreamApi.getSeries(server, cat.categoryId).getOrDefault(emptyList())
                    for (s in items) seriesStreamsCache[s.seriesId] = s
                    val previews = items.take(25).map { it.toMetaPreview() }
                    if (previews.isNotEmpty()) cat.toHomeCatalogSection(previews, "series") else null
                }
            }

            val builtSections = sectionJobs.map { scope.async { it() } }.awaitAll().filterNotNull()
            sections.addAll(builtSections)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Error fetching Xtream home catalog" }
        }

        return sections
    }

    private suspend fun fetchM3uHomeSections(server: XtreamServerAccount): List<HomeCatalogSection> {
        var playlist = getOrLoadM3uPlaylist(server)
        if (playlist == null && server.serverType == "m3u_url") {
            val resp = runCatching {
                com.nuvio.app.core.network.httpRequestRaw("GET", server.serverUrl, mapOf("User-Agent" to "neXStream", "Accept" to "*/*"), "")
            }.getOrNull()
            if (resp != null && resp.status in 200..299) {
                playlist = M3uParser.parse(resp.body)
                XtreamStorage.saveM3uContent(server.id, resp.body)
                m3uPlaylistsCache[server.id] = playlist
                cacheM3uChannels(playlist.channels)
            }
        }
        if (playlist == null || playlist.channels.isEmpty()) return emptyList()

        val grouped = playlist.channels.groupBy { it.groupTitle ?: "General" }
        val sections = mutableListOf<HomeCatalogSection>()
        for ((groupName, channels) in grouped.entries.take(20)) {
            for (c in channels) m3uChannelsCache[c.id] = c
            val previews = channels.take(30).map { it.toMetaPreview() }
            if (previews.isNotEmpty()) {
                sections.add(
                    HomeCatalogSection(
                        key = "m3u:$groupName",
                        title = groupName,
                        subtitle = "Lista IPTV",
                        addonName = server.name,
                        target = com.nuvio.app.features.catalog.CatalogTarget.XtreamCategory(
                            contentType = "tv",
                            categoryId = groupName,
                        ),
                        items = previews,
                    )
                )
            }
        }
        return sections
    }

    private fun ParsedM3uChannel.toMetaPreview(): MetaPreview = MetaPreview(
        id = "xtream:m3u:$id",
        type = when (type) {
            "movie" -> "movie"
            "series" -> "series"
            else -> "tv"
        },
        name = name,
        poster = logoUrl,
        logo = logoUrl,
        description = groupTitle ?: "IPTV",
        releaseInfo = null,
        imdbRating = null,
        genres = listOfNotNull(groupTitle),
    )

    suspend fun fetchCategoryItems(contentType: String, categoryId: String): List<MetaPreview> {
        val server = _activeServer.value ?: return emptyList()

        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            val playlist = getOrLoadM3uPlaylist(server) ?: return emptyList()
            val filtered = playlist.channels.filter { ch ->
                ch.groupTitle.equals(categoryId, ignoreCase = true) ||
                        (contentType in listOf("tv", "live") && ch.type == "live") ||
                        (contentType in listOf("movie", "vod") && ch.type == "movie") ||
                        (contentType == "series" && ch.type == "series")
            }
            for (c in filtered) m3uChannelsCache[c.id] = c
            return filtered.map { it.toMetaPreview() }
        }

        return try {
            when (contentType) {
                "tv", "live" -> {
                    val streams = XtreamApi.getLiveStreams(server, categoryId).getOrDefault(emptyList())
                    for (s in streams) liveStreamsCache[s.streamId] = s
                    streams.map { it.toMetaPreview() }
                }
                "movie", "vod" -> {
                    val streams = XtreamApi.getVodStreams(server, categoryId).getOrDefault(emptyList())
                    for (s in streams) vodStreamsCache[s.streamId] = s
                    streams.map { it.toMetaPreview() }
                }
                "series" -> {
                    val items = XtreamApi.getSeries(server, categoryId).getOrDefault(emptyList())
                    for (s in items) seriesStreamsCache[s.seriesId] = s
                    items.map { it.toMetaPreview() }
                }
                else -> emptyList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Error fetching Xtream category items" }
            emptyList()
        }
    }

    suspend fun search(query: String): List<MetaPreview> {
        val server = _activeServer.value ?: return emptyList()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            val playlist = getOrLoadM3uPlaylist(server) ?: return emptyList()
            val matches = playlist.channels.filter { it.name.lowercase().contains(q) }
            for (c in matches) m3uChannelsCache[c.id] = c
            return matches.take(50).map { it.toMetaPreview() }
        }

        val results = mutableListOf<MetaPreview>()
        try {
            val vodStreams = XtreamApi.getVodStreams(server).getOrDefault(emptyList())
            for (s in vodStreams) vodStreamsCache[s.streamId] = s
            results.addAll(
                vodStreams.filter { it.name.lowercase().contains(q) }
                    .take(20)
                    .map { it.toMetaPreview() }
            )

            val series = XtreamApi.getSeries(server).getOrDefault(emptyList())
            for (s in series) seriesStreamsCache[s.seriesId] = s
            results.addAll(
                series.filter { it.name.lowercase().contains(q) }
                    .take(20)
                    .map { it.toMetaPreview() }
            )

            val live = XtreamApi.getLiveStreams(server).getOrDefault(emptyList())
            for (s in live) liveStreamsCache[s.streamId] = s
            results.addAll(
                live.filter { it.name.lowercase().contains(q) }
                    .take(15)
                    .map { it.toMetaPreview() }
            )
        } catch (e: Exception) {
            log.e(e) { "Error searching Xtream" }
        }

        return results
    }

    suspend fun getMetaDetails(id: String): MetaDetails? {
        val server = _activeServer.value ?: return null
        val parts = id.split(":")
        if (parts.size < 3 || parts[0] != "xtream") return null

        val kind = parts[1]
        val target = parts[2]

        if (kind == "m3u") {
            val channelId = target.toIntOrNull() ?: return null
            val channel = m3uChannelsCache[channelId] ?: run {
                getOrLoadM3uPlaylist(server)?.channels?.find { it.id == channelId }
            } ?: return null

            return MetaDetails(
                id = id,
                type = if (channel.type == "movie") "movie" else if (channel.type == "series") "series" else "tv",
                name = channel.name,
                poster = channel.logoUrl,
                background = channel.logoUrl,
                logo = channel.logoUrl,
                description = channel.groupTitle ?: "Canal IPTV",
                genres = listOfNotNull(channel.groupTitle),
                videos = listOf(
                    com.nuvio.app.features.details.MetaVideo(
                        id = id,
                        title = channel.name,
                        thumbnail = channel.logoUrl,
                        available = true,
                    )
                ),
            )
        }

        val targetId = target.toIntOrNull() ?: return null

        return when (kind) {
            "vod" -> {
                val vodInfo = XtreamApi.getVodInfo(server, targetId).getOrNull()
                val cached = vodStreamsCache[targetId]
                if (vodInfo != null) {
                    vodInfo.toMetaDetails(targetId, cached?.name ?: "Película")
                } else if (cached != null) {
                    MetaDetails(
                        id = id,
                        type = "movie",
                        name = cached.name,
                        poster = cached.streamIcon,
                        videos = listOf(com.nuvio.app.features.details.MetaVideo(id = id, title = cached.name, available = true)),
                    )
                } else null
            }
            "series" -> {
                val seriesInfo = XtreamApi.getSeriesInfo(server, targetId).getOrNull()
                val cached = seriesStreamsCache[targetId]
                if (seriesInfo != null) {
                    seriesInfo.toMetaDetails(targetId, cached?.name ?: "Serie")
                } else if (cached != null) {
                    MetaDetails(
                        id = id,
                        type = "series",
                        name = cached.name,
                        poster = cached.cover,
                        description = cached.plot,
                    )
                } else null
            }
            "live" -> {
                val cached = liveStreamsCache[targetId] ?: run {
                    val all = XtreamApi.getLiveStreams(server).getOrDefault(emptyList())
                    for (s in all) liveStreamsCache[s.streamId] = s
                    liveStreamsCache[targetId]
                } ?: return null
                MetaDetails(
                    id = id,
                    type = "tv",
                    name = cached.name,
                    poster = cached.streamIcon,
                    background = cached.streamIcon,
                    logo = cached.streamIcon,
                    genres = listOfNotNull(cached.streamType),
                    videos = listOf(
                        com.nuvio.app.features.details.MetaVideo(
                            id = id,
                            title = cached.name,
                            thumbnail = cached.streamIcon,
                            available = true,
                        ),
                    ),
                )
            }
            else -> null
        }
    }

    fun getStreamItems(mediaId: String): List<StreamItem> {
        val server = _activeServer.value ?: return emptyList()
        val parts = mediaId.split(":")
        if (parts.size < 3 || parts[0] != "xtream") return emptyList()

        val kind = parts[1]
        val target = parts[2]

        if (kind == "m3u") {
            val channelId = target.toIntOrNull() ?: return emptyList()
            val channel = m3uChannelsCache[channelId] ?: run {
                getOrLoadM3uPlaylist(server)?.channels?.find { it.id == channelId }
            } ?: return emptyList()

            return listOf(
                StreamItem(
                    name = server.name,
                    title = channel.name,
                    description = channel.groupTitle ?: "Directo IPTV",
                    url = channel.streamUrl,
                    addonName = server.name,
                    addonId = "xtream",
                    streamType = "http",
                )
            )
        }

        val url: String = when (kind) {
            "live" -> {
                val streamId = target.toIntOrNull() ?: return emptyList()
                XtreamApi.buildLiveStreamUrl(server, streamId)
            }
            "vod" -> {
                val streamId = target.toIntOrNull() ?: return emptyList()
                val cached = vodStreamsCache[streamId]
                XtreamApi.buildVodStreamUrl(server, streamId, cached?.containerExtension ?: "mp4")
            }
            "series_ep" -> {
                val episodeId = target
                val ext = parts.getOrNull(3) ?: "mp4"
                XtreamApi.buildSeriesStreamUrl(server, episodeId, ext)
            }
            else -> return emptyList()
        }

        return listOf(
            StreamItem(
                name = server.name,
                title = "Reproducción Directa",
                description = server.serverUrl,
                url = url,
                addonName = server.name,
                addonId = "xtream",
                streamType = "http",
            ),
        )
    }

    fun registerChannelCategory(channelIdOrName: String, categoryId: String) {
        val clean = channelIdOrName.trim()
        if (clean.isNotEmpty() && categoryId.isNotBlank()) {
            channelCategoryCache[clean] = categoryId
            channelCategoryCache[clean.lowercase()] = categoryId
            val stripped = clean.removePrefix("xtream:live:").removePrefix("xtream:m3u:")
            if (stripped != clean && stripped.isNotEmpty()) {
                channelCategoryCache[stripped] = categoryId
            }
        }
    }

    suspend fun getDrawerCategories(): List<XtreamCategory> {
        val server = _activeServer.value ?: return emptyList()
        return getLiveCategories(server)
    }

    fun findChannelCategory(channelId: String?, channelName: String? = null): String? {
        val server = _activeServer.value ?: return null

        // 1. Direct memory cache
        if (!channelId.isNullOrBlank()) {
            channelCategoryCache[channelId]?.let { return it }
            val cleanId = channelId.removePrefix("xtream:live:").removePrefix("xtream:m3u:")
            channelCategoryCache[cleanId]?.let { return it }
        }
        if (!channelName.isNullOrBlank()) {
            channelCategoryCache[channelName.trim().lowercase()]?.let { return it }
        }

        // 2. M3U channels lookup
        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            val cleanId = channelId?.removePrefix("xtream:m3u:")?.toIntOrNull()
            val ch = (if (cleanId != null) m3uChannelsCache[cleanId] else null)
                ?: m3uChannelsCache.values.find { ch ->
                    (!channelId.isNullOrBlank() && (ch.id.toString() == channelId || "xtream:m3u:${ch.id}" == channelId)) ||
                        (!channelName.isNullOrBlank() && ch.name.equals(channelName, ignoreCase = true))
                }
            ch?.groupTitle?.let { group ->
                channelId?.let { registerChannelCategory(it, group) }
                channelName?.let { registerChannelCategory(it, group) }
                return group
            }
            return null
        }

        // 3. Xtream liveStreamsCache
        val cleanId = channelId?.removePrefix("xtream:live:")?.toIntOrNull()
        val stream = (if (cleanId != null) liveStreamsCache[cleanId] else null)
            ?: liveStreamsCache.values.find { s ->
                (!channelId.isNullOrBlank() && (s.streamId.toString() == channelId || "xtream:live:${s.streamId}" == channelId)) ||
                    (!channelName.isNullOrBlank() && s.name.equals(channelName, ignoreCase = true))
            }
        stream?.categoryId?.let { catId ->
            channelId?.let { registerChannelCategory(it, catId) }
            channelName?.let { registerChannelCategory(it, catId) }
            return catId
        }

        // 4. Check already loaded drawer categories
        for ((catId, channels) in drawerChannelsCache) {
            val found = channels.find { ch ->
                (!channelId.isNullOrBlank() && ch.id == channelId) ||
                    (!channelName.isNullOrBlank() && ch.name.equals(channelName, ignoreCase = true))
            }
            if (found != null) {
                val effectiveCat = found.categoryId ?: catId
                if (effectiveCat.isNotBlank() && effectiveCat != "all") {
                    channelId?.let { registerChannelCategory(it, effectiveCat) }
                    channelName?.let { registerChannelCategory(it, effectiveCat) }
                    return effectiveCat
                }
            }
        }

        return null
    }

    suspend fun resolveChannelCategory(
        channelId: String?,
        channelName: String?,
        categories: List<XtreamCategory>,
    ): String? {
        findChannelCategory(channelId, channelName)?.let { return it }
        if (categories.isEmpty()) return null

        val server = _activeServer.value ?: return null
        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            return findChannelCategory(channelId, channelName)
        }

        // Search in the first 15 categories to find matching channel quickly
        for (cat in categories.take(15)) {
            val channels = getChannelsForDrawer(cat.categoryId)
            val matched = channels.find { ch ->
                (!channelId.isNullOrBlank() && ch.id == channelId) ||
                    (!channelName.isNullOrBlank() && ch.name.equals(channelName, ignoreCase = true))
            }
            if (matched != null) {
                channelId?.let { registerChannelCategory(it, cat.categoryId) }
                channelName?.let { registerChannelCategory(it, cat.categoryId) }
                return cat.categoryId
            }
        }
        return null
    }

    suspend fun getChannelsForDrawer(categoryId: String? = null): List<ChannelDrawerItem> {
        val cacheKey = categoryId?.trim()?.ifEmpty { "all" } ?: "all"
        drawerChannelsCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) return cached
        }

        val server = _activeServer.value ?: return emptyList()
        if (server.serverType == "m3u_url" || server.serverType == "m3u_file") {
            val playlist = getOrLoadM3uPlaylist(server) ?: return emptyList()
            val channels = if (categoryId.isNullOrBlank() || categoryId == "all") {
                playlist.channels.filter { it.type == "live" || it.type == "tv" }
                    .ifEmpty { playlist.channels }
            } else {
                playlist.channels.filter { it.groupTitle.equals(categoryId, ignoreCase = true) }
            }
            val items = channels.map { ch ->
                m3uChannelsCache[ch.id] = ch
                ch.groupTitle?.let { grp ->
                    registerChannelCategory("xtream:m3u:${ch.id}", grp)
                    registerChannelCategory(ch.name, grp)
                }
                ChannelDrawerItem(
                    id = "xtream:m3u:${ch.id}",
                    name = ch.name,
                    streamUrl = ch.streamUrl,
                    logoUrl = ch.logoUrl,
                    categoryId = ch.groupTitle,
                    channelNumber = ch.id,
                )
            }
            if (items.isNotEmpty()) {
                drawerChannelsCache[cacheKey] = items
            }
            return items
        }

        // For Xtream: Never query all channels without category (slow, times out).
        val streams = if (categoryId.isNullOrBlank() || categoryId == "all") {
            if (liveStreamsCache.isNotEmpty()) {
                liveStreamsCache.values.toList()
            } else {
                val cats = getLiveCategories(server)
                val firstCat = cats.firstOrNull()?.categoryId
                if (firstCat != null) {
                    XtreamApi.getLiveStreams(server, firstCat).getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }
        } else {
            XtreamApi.getLiveStreams(server, categoryId).getOrDefault(emptyList())
        }

        val items = streams.map { s ->
            liveStreamsCache[s.streamId] = s
            val cat = s.categoryId ?: categoryId
            if (!cat.isNullOrBlank()) {
                registerChannelCategory("xtream:live:${s.streamId}", cat)
                registerChannelCategory(s.name, cat)
            }
            ChannelDrawerItem(
                id = "xtream:live:${s.streamId}",
                name = s.name,
                streamUrl = XtreamApi.buildLiveStreamUrl(server, s.streamId),
                logoUrl = s.streamIcon,
                categoryId = cat,
                channelNumber = s.num,
            )
        }
        if (items.isNotEmpty()) {
            drawerChannelsCache[cacheKey] = items
        }
        return items
    }

    suspend fun zapChannel(
        currentChannelId: String?,
        currentChannelName: String?,
        forward: Boolean,
    ): ChannelDrawerItem? {
        val activeCategory = findChannelCategory(currentChannelId, currentChannelName)
        val channels = getChannelsForDrawer(activeCategory).ifEmpty {
            if (activeCategory != null && activeCategory != "all") getChannelsForDrawer("all") else emptyList()
        }
        if (channels.isEmpty()) return null
        val currentIndex = channels.indexOfFirst {
            (!currentChannelId.isNullOrBlank() && it.id == currentChannelId) ||
                (!currentChannelName.isNullOrBlank() && it.name.equals(currentChannelName, ignoreCase = true))
        }
        val targetIndex = if (currentIndex < 0) {
            0
        } else if (forward) {
            (currentIndex + 1) % channels.size
        } else {
            (currentIndex - 1 + channels.size) % channels.size
        }
        return channels.getOrNull(targetIndex)
    }
}

data class ChannelDrawerItem(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val categoryId: String? = null,
    val channelNumber: Int? = null,
)
