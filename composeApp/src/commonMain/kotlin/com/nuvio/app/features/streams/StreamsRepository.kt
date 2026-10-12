package com.nuvio.app.features.streams

import co.touchlab.kermit.Logger
import com.nuvio.app.features.xtream.XtreamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object StreamsRepository {
    private val log = Logger.withTag("StreamsRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(StreamsUiState())
    val uiState: StateFlow<StreamsUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var activeRequestKey: String? = null

    fun requestToken(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        manualSelection: Boolean = false,
    ): String =
        "$type::$videoId::$season::$episode::$manualSelection"

    fun load(type: String, videoId: String, parentMetaId: String? = null, season: Int? = null, episode: Int? = null, manualSelection: Boolean = false) {
        load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = season,
            episode = episode,
            manualSelection = manualSelection,
            forceRefresh = false,
        )
    }

    fun reload(type: String, videoId: String, parentMetaId: String? = null, season: Int? = null, episode: Int? = null, manualSelection: Boolean = false) {
        load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = season,
            episode = episode,
            manualSelection = manualSelection,
            forceRefresh = true,
        )
    }

    private fun load(type: String, videoId: String, parentMetaId: String?, season: Int?, episode: Int?, manualSelection: Boolean, forceRefresh: Boolean) {
        val requestToken = requestToken(
            type = type,
            videoId = videoId,
            season = season,
            episode = episode,
            manualSelection = manualSelection,
        )
        val requestKey = requestToken
        val currentState = _uiState.value
        if (
            !forceRefresh &&
            activeRequestKey == requestKey &&
            (currentState.groups.isNotEmpty() || currentState.emptyStateReason != null || currentState.isAnyLoading)
        ) {
            log.d { "Skipping stream reload for unchanged request type=$type id=$videoId" }
            return
        }

        activeRequestKey = requestKey
        activeJob?.cancel()

        val xtreamStreams = XtreamRepository.getStreamItems(videoId)
        if (xtreamStreams.isNotEmpty()) {
            val group = AddonStreamGroup(
                addonName = xtreamStreams.first().addonName,
                addonId = "xtream",
                streams = xtreamStreams,
                isLoading = false,
            )
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                groups = listOf(group),
                autoPlayStream = xtreamStreams.firstOrNull(),
                autoPlayDecided = true,
                activeAddonIds = setOf("xtream"),
                isAnyLoading = false,
                isDirectAutoPlayFlow = !manualSelection,
                showDirectAutoPlayOverlay = !manualSelection,
            )
        } else {
            _uiState.value = StreamsUiState(
                requestToken = requestToken,
                isAnyLoading = false,
                emptyStateReason = StreamsEmptyStateReason.NoCompatibleAddons,
                autoPlayDecided = true,
            )
        }
    }

    fun selectFilter(filter: String?) {
        _uiState.update { current ->
            current.copy(selectedFilter = if (current.selectedFilter == filter) null else filter)
        }
    }

    fun cancelLoading() {
        activeJob?.cancel()
        _uiState.update { it.copy(isAnyLoading = false) }
    }

    fun setOverlayVisible(visible: Boolean, message: String? = null) {
        _uiState.update { it.copy(showDirectAutoPlayOverlay = visible, overlayMessage = message) }
    }

    fun skipAutoPlayStream(stream: StreamItem) {
        _uiState.update { it.copy(showDirectAutoPlayOverlay = false) }
    }

    fun consumeAutoPlay() {
        _uiState.update { it.copy(showDirectAutoPlayOverlay = false) }
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        activeRequestKey = null
        _uiState.value = StreamsUiState()
    }
}
