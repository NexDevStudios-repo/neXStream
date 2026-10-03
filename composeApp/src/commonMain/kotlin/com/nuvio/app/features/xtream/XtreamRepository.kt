package com.nuvio.app.features.xtream

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.sourcemode.SourceModeRepository
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

    fun initialize() {
        loadServers()
    }

    private fun loadServers() {
        val raw = XtreamStorage.loadServersJson().orEmpty().trim()
        val list = if (raw.isNotEmpty()) {
            runCatching { json.decodeFromString<List<XtreamServerAccount>>(raw) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        _servers.value = list
        val activeId = SourceModeRepository.activeXtreamServerId.value
        val active = list.find { it.id == activeId } ?: list.firstOrNull()
        _activeServer.value = active
        if (active != null && active.id != activeId) {
            SourceModeRepository.setActiveXtreamServerId(active.id)
        }
    }

    private fun persistServers(list: List<XtreamServerAccount>) {
        _servers.value = list
        XtreamStorage.saveServersJson(json.encodeToString(list))
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addServer(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
    ): Result<XtreamServerAccount> {
        val authResult = XtreamApi.authenticate(serverUrl, username, password)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Authentication failed"))
        }

        val id = Uuid.random().toString()
        val account = XtreamServerAccount(
            id = id,
            name = name.ifBlank { "Xtream Server" },
            serverUrl = XtreamApi.normalizeBaseUrl(serverUrl),
            username = username.trim(),
            password = password.trim(),
            isActive = true,
        )

        val updated = _servers.value + account
        persistServers(updated)
        selectServer(account.id)
        return Result.success(account)
    }

    fun selectServer(serverId: String) {
        val server = _servers.value.find { it.id == serverId }
        _activeServer.value = server
        SourceModeRepository.setActiveXtreamServerId(server?.id)
        clearCache()
    }

    fun deleteServer(serverId: String) {
        val updated = _servers.value.filterNot { it.id == serverId }
        persistServers(updated)
        if (_activeServer.value?.id == serverId) {
            val next = updated.firstOrNull()
            _activeServer.value = next
            SourceModeRepository.setActiveXtreamServerId(next?.id)
        }
        clearCache()
    }

    fun clearCache() {
        liveStreamsCache.clear()
        vodStreamsCache.clear()
        seriesStreamsCache.clear()
    }

    suspend fun fetchHomeCatalogSections(): List<HomeCatalogSection> {
        val server = _activeServer.value ?: return emptyList()
        val sections = mutableListOf<HomeCatalogSection>()

        try {
            val liveCatsDeferred = scope.async { XtreamApi.getLiveCategories(server).getOrDefault(emptyList()) }
            val vodCatsDeferred = scope.async { XtreamApi.getVodCategories(server).getOrDefault(emptyList()) }
            val seriesCatsDeferred = scope.async { XtreamApi.getSeriesCategories(server).getOrDefault(emptyList()) }

            val liveCats = liveCatsDeferred.await().take(4)
            val vodCats = vodCatsDeferred.await().take(5)
            val seriesCats = seriesCatsDeferred.await().take(5)

            val sectionJobs = mutableListOf<suspend () -> HomeCatalogSection?>()

            for (cat in liveCats) {
                sectionJobs.add {
                    val streams = XtreamApi.getLiveStreams(server, cat.categoryId).getOrDefault(emptyList())
                    for (s in streams) liveStreamsCache[s.streamId] = s
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

    suspend fun search(query: String): List<MetaPreview> {
        val server = _activeServer.value ?: return emptyList()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

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
        val targetId = parts[2].toIntOrNull() ?: return null

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
                val cached = liveStreamsCache[targetId]
                if (cached != null) {
                    cached.toMetaDetails()
                } else {
                    MetaDetails(
                        id = id,
                        type = "tv",
                        name = "Canal $targetId",
                        videos = listOf(com.nuvio.app.features.details.MetaVideo(id = id, title = "Canal $targetId", available = true)),
                    )
                }
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
}
