package com.nuvio.app.features.player

import co.touchlab.kermit.Logger
import com.nuvio.app.features.streams.AddonStreamGroup
import com.nuvio.app.features.streams.StreamsUiState
import com.nuvio.app.features.xtream.XtreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerStreamsRepository {
    private val log = Logger.withTag("PlayerStreamsRepo")

    private val _sourceState = MutableStateFlow(StreamsUiState())
    val sourceState: StateFlow<StreamsUiState> = _sourceState.asStateFlow()

    private val _episodeStreamsState = MutableStateFlow(StreamsUiState())
    val episodeStreamsState: StateFlow<StreamsUiState> = _episodeStreamsState.asStateFlow()

    fun loadSources(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        forceRefresh: Boolean = false,
    ) {
        val streams = XtreamRepository.getStreamItems(videoId)
        if (streams.isNotEmpty()) {
            val group = AddonStreamGroup(
                addonName = streams.first().addonName,
                addonId = "xtream",
                streams = streams,
                isLoading = false,
            )
            _sourceState.value = StreamsUiState(
                groups = listOf(group),
                isAnyLoading = false,
                autoPlayDecided = true,
            )
        } else {
            _sourceState.value = StreamsUiState(
                groups = emptyList(),
                isAnyLoading = false,
                autoPlayDecided = true,
            )
        }
    }

    fun loadEpisodeStreams(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        forceRefresh: Boolean = false,
    ) {
        val streams = XtreamRepository.getStreamItems(videoId)
        if (streams.isNotEmpty()) {
            val group = AddonStreamGroup(
                addonName = streams.first().addonName,
                addonId = "xtream",
                streams = streams,
                isLoading = false,
            )
            _episodeStreamsState.value = StreamsUiState(
                groups = listOf(group),
                isAnyLoading = false,
                autoPlayDecided = true,
            )
        } else {
            _episodeStreamsState.value = StreamsUiState(
                groups = emptyList(),
                isAnyLoading = false,
                autoPlayDecided = true,
            )
        }
    }

    fun clearSources() {
        _sourceState.value = StreamsUiState()
    }

    fun clearEpisodeStreams() {
        _episodeStreamsState.value = StreamsUiState()
    }

    fun clearAll() {
        clearSources()
        clearEpisodeStreams()
    }

    fun pauseSearchForPlayback() {}
    fun stopSourcesLoading() {}
    fun selectSourceFilter(filter: String?) {}
    fun selectEpisodeStreamsFilter(filter: String?) {}
}
