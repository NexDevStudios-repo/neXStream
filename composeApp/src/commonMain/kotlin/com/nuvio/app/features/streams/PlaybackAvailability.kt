package com.nuvio.app.features.streams

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.xtream.XtreamRepository

internal class PlaybackAvailability {
    fun canStream(type: String, videoId: String): Boolean =
        videoId.startsWith("xtream:") ||
            XtreamRepository.activeServer.value != null ||
            MetaDetailsRepository.findEmbeddedStreams(videoId).isNotEmpty()

    fun canPlay(
        type: String,
        videoId: String,
        parentMetaId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
    ): Boolean = canStream(type, videoId) || DownloadsRepository.findPlayableDownload(
        parentMetaId = parentMetaId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        videoId = videoId,
    ) != null

    companion object {
        fun current(): PlaybackAvailability = PlaybackAvailability()
    }
}

@Composable
internal fun rememberPlaybackAvailability(): PlaybackAvailability {
    val activeServer by remember {
        XtreamRepository.initialize()
        XtreamRepository.activeServer
    }.collectAsStateWithLifecycle()
    val downloads by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    return remember(activeServer, downloads) {
        PlaybackAvailability()
    }
}
