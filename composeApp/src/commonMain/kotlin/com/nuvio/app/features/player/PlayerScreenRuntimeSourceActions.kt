package com.nuvio.app.features.player

import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.downloads.DownloadItem
import com.nuvio.app.features.downloads.DownloadSubtitles
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamLinkCacheRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.buildPlaybackVideoId
import kotlinx.coroutines.launch

internal fun PlayerScreenRuntime.openExternalSourceUrl(stream: StreamItem): Boolean {
    if (!stream.shouldOpenExternally) return false
    val url = stream.externalOpenUrl ?: return false
    val openExternalUrl = args.onOpenExternalUrl ?: return false
    openExternalUrl(url)
    showSourcesPanel = false
    showEpisodesPanel = false
    controlsVisible = true
    return true
}

internal fun StreamItem.playerSourceIdentityKey(): String? {
    playableDirectUrl?.trim()?.takeIf { it.isNotBlank() }?.let { url ->
        return "url:$url"
    }
    return null
}

internal fun PlayerScreenRuntime.switchToSource(stream: StreamItem) {
    if (openExternalSourceUrl(stream)) return
    val url = stream.playableDirectUrl ?: stream.url ?: return
    val sourceIdentityKey = stream.playerSourceIdentityKey()
    if (url == activeSourceUrl) {
        activeSourceIdentityKey = sourceIdentityKey ?: activeSourceIdentityKey
        return
    }
    val currentPositionMs = playbackSnapshot.positionMs.coerceAtLeast(0L)
    flushWatchProgress()
    val currentVideoId = activeVideoId
    if (playerSettingsUiState.streamReuseLastLinkEnabled && currentVideoId != null) {
        saveDirectStreamForReuse(stream, url, currentVideoId, activeSeasonNumber, activeEpisodeNumber)
    }
    externalSubtitles = stream.externalSubtitles
    activeSourceUrl = url
    activeSourceAudioUrl = null
    activeSourceHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request)
    activeSourceResponseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response)
    activeStreamType = stream.streamType
    activeSourceIdentityKey = sourceIdentityKey
    activeStreamTitle = stream.streamLabel
    activeStreamSubtitle = stream.streamSubtitle
    activeProviderName = stream.addonName
    activeProviderAddonId = stream.addonId
    currentStreamBingeGroup = stream.behaviorHints.bingeGroup
    activeInitialPositionMs = currentPositionMs
    activeInitialProgressFraction = null
    showSourcesPanel = false
    controlsVisible = true
}

internal fun PlayerScreenRuntime.switchToEpisodeStream(stream: StreamItem, episode: MetaVideo) {
    if (openExternalSourceUrl(stream)) return
    val url = stream.playableDirectUrl ?: stream.url ?: return
    resetEpisodePanelAndNextEpisodeState()
    flushWatchProgress()
    val epVideoId = episode.id
    val resume = resolveEpisodeResume(epVideoId, episode)
    if (playerSettingsUiState.streamReuseLastLinkEnabled) {
        saveDirectStreamForReuse(stream, url, epVideoId, episode.season, episode.episode)
    }
    externalSubtitles = stream.externalSubtitles
    activeSourceUrl = url
    activeSourceAudioUrl = null
    activeSourceHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request)
    activeSourceResponseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response)
    activeStreamType = stream.streamType
    applyEpisodeStreamMetadata(stream, episode, resume)
}

internal fun PlayerScreenRuntime.switchToDownloadedEpisode(downloadItem: DownloadItem, episode: MetaVideo) {
    val localFileUri = DownloadsRepository.playableLocalFileUri(downloadItem) ?: return
    resetEpisodePanelAndNextEpisodeState()
    flushWatchProgress()

    val fallbackVideoId = buildPlaybackVideoId(
        parentMetaId = parentMetaId,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
        fallbackVideoId = episode.id,
    )
    val resolvedVideoId = episode.id.takeIf { it.isNotBlank() } ?: fallbackVideoId
    val epEntry = WatchProgressRepository.progressForVideo(
        videoId = resolvedVideoId,
        parentMetaId = parentMetaId,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
    )?.takeIf { !it.isCompleted }
    val epResumeFraction = epEntry?.progressPercent
        ?.takeIf { it > 0f }
        ?.let { (it / 100f).coerceIn(0f, 1f) }
    val epResumePositionMs = epEntry?.lastPositionMs?.takeIf { it > 0L } ?: 0L

    externalSubtitles = DownloadSubtitles.localSubtitles(localFileUri)
    activeSourceUrl = localFileUri
    activeSourceAudioUrl = null
    activeSourceHeaders = emptyMap()
    activeSourceResponseHeaders = emptyMap()
    activeStreamType = null
    activeSourceIdentityKey = null
    activeStreamTitle = downloadItem.streamTitle.ifBlank {
        episode.title.ifBlank { title }
    }
    activeStreamSubtitle = downloadItem.streamSubtitle
    activeProviderName = downloadItem.providerName.ifBlank { downloadedLabel }
    activeProviderAddonId = downloadItem.providerAddonId
    currentStreamBingeGroup = null
    activeSeasonNumber = episode.season
    activeEpisodeNumber = episode.episode
    activeEpisodeTitle = episode.title
    activeEpisodeThumbnail = episode.thumbnail
    activePauseDescription = episode.overview
    activeVideoId = resolvedVideoId
    activeInitialPositionMs = epResumePositionMs
    activeInitialProgressFraction = epResumeFraction
    controlsVisible = true
}

internal fun PlayerScreenRuntime.playNextEpisode(automatic: Boolean = false) {
    if (nextEpisodeAutoPlaySearching || nextEpisodeAutoPlayCountdown != null) return
    val playbackKey = activePlaybackKey
    val nextVideoId = nextEpisodeInfo?.takeIf { it.hasAired }?.videoId ?: return
    fun isCurrentRequest(): Boolean = playbackKey == activePlaybackKey &&
        nextEpisodeInfo?.videoId == nextVideoId &&
        (!automatic || (
            playerSettingsUiState.streamAutoPlayNextEpisodeEnabled &&
                !nextEpisodeCardDismissed && isAtNextEpisodeThreshold()
            ))
    if (!isCurrentRequest()) return
    nextEpisodeAutoPlayAutomatic = automatic

    scope.launchPlayerNextEpisodeAutoPlay(
        previousJob = nextEpisodeAutoPlayJob,
        nextEpisodeInfo = nextEpisodeInfo,
        allEpisodes = playerMetaVideos,
        parentMetaId = parentMetaId,
        parentMetaType = parentMetaType,
        contentType = contentType,
        settings = playerSettingsUiState,
        currentStreamBingeGroup = currentStreamBingeGroup,
        onDownloadedEpisodeSelected = { item, episode ->
            if (isCurrentRequest()) switchToDownloadedEpisode(item, episode)
        },
        onEpisodeStreamSelected = { stream, episode ->
            if (isCurrentRequest()) switchToEpisodeStream(stream, episode)
        },
        onManualSelectionRequired = { nextVideo ->
            if (isCurrentRequest()) {
                nextEpisodeCardDismissed = true
                episodeStreamsPanelState = EpisodeStreamsPanelState(
                    showStreams = true,
                    selectedEpisode = nextVideo,
                )
                showEpisodesPanel = true
            }
        },
        onSearchingChanged = {
            if (playbackKey == activePlaybackKey) nextEpisodeAutoPlaySearching = it
        },
        onSourceNameChanged = {
            if (playbackKey == activePlaybackKey) nextEpisodeAutoPlaySourceName = it
        },
        onCountdownChanged = {
            if (playbackKey == activePlaybackKey) nextEpisodeAutoPlayCountdown = it
        },
        onNextEpisodeCardVisibleChanged = {
            if (playbackKey == activePlaybackKey) showNextEpisodeCard = it
        },
    )?.let { job ->
        nextEpisodeAutoPlayJob = job
    }
}

internal fun PlayerScreenRuntime.openSourcesPanel() {
    val vid = activeVideoId ?: return
    PlayerStreamsRepository.loadSources(
        type = contentType ?: parentMetaType,
        videoId = vid,
        season = activeSeasonNumber,
        episode = activeEpisodeNumber,
    )
    showSourcesPanel = true
    showEpisodesPanel = false
    controlsVisible = false
}

internal fun PlayerScreenRuntime.openEpisodesPanel() {
    showEpisodesPanel = true
    showSourcesPanel = false
    controlsVisible = false
}

private fun PlayerScreenRuntime.saveDirectStreamForReuse(
    stream: StreamItem,
    url: String,
    videoId: String,
    season: Int?,
    episode: Int?,
) {
    val cacheKey = StreamLinkCacheRepository.contentKey(
        type = contentType ?: parentMetaType,
        videoId = videoId,
        parentMetaId = parentMetaId,
        season = season,
        episode = episode,
    )
    StreamLinkCacheRepository.save(
        contentKey = cacheKey,
        url = url,
        streamName = stream.streamLabel,
        addonName = stream.addonName,
        addonId = stream.addonId,
        requestHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request),
        responseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
        bingeGroup = stream.behaviorHints.bingeGroup,
        streamType = stream.streamType,
    )
}

private data class EpisodeResumeInfo(
    val positionMs: Long,
    val progressFraction: Float?,
    val resolvedVideoId: String,
)

private fun PlayerScreenRuntime.resetEpisodePanelAndNextEpisodeState() {
    showEpisodesPanel = false
    episodeStreamsPanelState = EpisodeStreamsPanelState()
    cancelNextEpisodeAutoPlay()
    showNextEpisodeCard = false
    nextEpisodeCardDismissed = false
}

private fun PlayerScreenRuntime.resolveEpisodeResume(
    epVideoId: String,
    episode: MetaVideo,
): EpisodeResumeInfo {
    val fallbackVideoId = buildPlaybackVideoId(
        parentMetaId = parentMetaId,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
        fallbackVideoId = episode.id,
    )
    val resolvedVideoId = epVideoId.takeIf { it.isNotBlank() } ?: fallbackVideoId
    val epEntry = WatchProgressRepository.progressForVideo(
        videoId = resolvedVideoId,
        parentMetaId = parentMetaId,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
    )?.takeIf { !it.isCompleted }
    val epResumeFraction = epEntry?.progressPercent
        ?.takeIf { it > 0f }
        ?.let { (it / 100f).coerceIn(0f, 1f) }
    val epResumePositionMs = epEntry?.lastPositionMs?.takeIf { it > 0L } ?: 0L
    return EpisodeResumeInfo(
        positionMs = epResumePositionMs,
        progressFraction = epResumeFraction,
        resolvedVideoId = resolvedVideoId,
    )
}

private fun PlayerScreenRuntime.applyEpisodeStreamMetadata(
    stream: StreamItem,
    episode: MetaVideo,
    resume: EpisodeResumeInfo,
) {
    activeSourceIdentityKey = stream.playerSourceIdentityKey()
    activeStreamTitle = stream.streamLabel
    activeStreamSubtitle = stream.streamSubtitle
    activeProviderName = stream.addonName
    activeProviderAddonId = stream.addonId
    currentStreamBingeGroup = stream.behaviorHints.bingeGroup
    activeSeasonNumber = episode.season
    activeEpisodeNumber = episode.episode
    activeEpisodeTitle = episode.title
    activeEpisodeThumbnail = episode.thumbnail
    activePauseDescription = episode.overview
    activeVideoId = resume.resolvedVideoId
    activeInitialPositionMs = resume.positionMs
    activeInitialProgressFraction = resume.progressFraction
    controlsVisible = true
}

internal fun PlayerScreenRuntime.switchToChannel(
    id: String,
    name: String,
    streamUrl: String,
    logoUrl: String?,
) {
    flushWatchProgress()
    activeParentMetaId = id
    activeParentMetaType = "channel"
    activeTitle = name
    activeStreamTitle = name
    activeStreamSubtitle = null
    activeLogo = logoUrl
    activePoster = logoUrl
    activeBackground = logoUrl
    activeVideoId = id
    activeSourceUrl = streamUrl
    activeSourceAudioUrl = null
    activeSourceIdentityKey = "url:$streamUrl"
    activeSourceHeaders = emptyMap()
    activeSourceResponseHeaders = emptyMap()
    activeInitialPositionMs = 0L
    activeInitialProgressFraction = null
    initialLoadCompleted = false
    playbackSnapshot = PlayerPlaybackSnapshot()
    errorMessage = null
    showChannelsPanel = false
    controlsVisible = true
    shouldPlay = true
}

internal fun PlayerScreenRuntime.zapChannel(forward: Boolean) {
    if (!isLive) return
    scope.launch {
        val next = com.nuvio.app.features.xtream.XtreamRepository.zapChannel(
            currentChannelId = parentMetaId,
            currentChannelName = activeStreamTitle,
            forward = forward,
        ) ?: return@launch
        switchToChannel(
            id = next.id,
            name = next.name,
            streamUrl = next.streamUrl,
            logoUrl = next.logoUrl,
        )
    }
}

