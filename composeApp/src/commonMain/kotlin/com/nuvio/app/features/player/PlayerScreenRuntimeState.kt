package com.nuvio.app.features.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsUiState
import com.nuvio.app.features.details.MetaScreenSettingsUiState
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.player.skip.NextEpisodeInfo
import com.nuvio.app.features.player.skip.SkipInterval
import com.nuvio.app.features.streams.StreamsUiState
import com.nuvio.app.features.tracking.TrackingMediaReference
import com.nuvio.app.features.watched.WatchedUiState
import com.nuvio.app.features.watchprogress.WatchProgressUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

internal class PlayerScreenRuntime(
    args: PlayerScreenArgs,
) {
    var args by mutableStateOf(args)

    var activeTitle by mutableStateOf(args.title)
    var activeLogo by mutableStateOf(args.logo)
    var activePoster by mutableStateOf(args.poster)
    var activeBackground by mutableStateOf(args.background)
    var activeParentMetaId by mutableStateOf(args.parentMetaId)
    var activeParentMetaType by mutableStateOf(args.parentMetaType)

    val title: String get() = activeTitle
    val profileId: Int get() = args.profileId
    val sourceUrl: String get() = activeSourceUrl
    val sourceAudioUrl: String? get() = activeSourceAudioUrl
    val sourceHeaders: Map<String, String> get() = activeSourceHeaders
    val sourceResponseHeaders: Map<String, String> get() = activeSourceResponseHeaders
    val streamType: String? get() = activeStreamType
    val providerName: String get() = activeProviderName
    val streamTitle: String get() = activeStreamTitle
    val streamSubtitle: String? get() = activeStreamSubtitle
    val initialBingeGroup: String? get() = currentStreamBingeGroup
    val pauseDescription: String? get() = activePauseDescription
    val logo: String? get() = activeLogo
    val poster: String? get() = activePoster
    val background: String? get() = activeBackground
    val seasonNumber: Int? get() = activeSeasonNumber
    val episodeNumber: Int? get() = activeEpisodeNumber
    val episodeTitle: String? get() = activeEpisodeTitle
    val episodeThumbnail: String? get() = activeEpisodeThumbnail
    val contentType: String? get() = args.contentType
    val videoId: String? get() = activeVideoId
    val parentMetaId: String get() = activeParentMetaId
    val parentMetaType: String get() = activeParentMetaType
    val providerAddonId: String? get() = activeProviderAddonId
    val initialPositionMs: Long get() = activeInitialPositionMs
    val initialProgressFraction: Float? get() = activeInitialProgressFraction
    var externalSubtitles by mutableStateOf(args.externalSubtitles)
    val isSeries: Boolean get() = parentMetaType == "series"
    val isLive: Boolean
        get() = parentMetaType in listOf("channel", "tv", "live") ||
                contentType in listOf("channel", "tv", "live") ||
                parentMetaId.startsWith("xtream:channel") ||
                parentMetaId.startsWith("xtream:live") ||
                parentMetaId.startsWith("xtream:m3u")

    lateinit var scope: CoroutineScope
    lateinit var hapticFeedback: HapticFeedback

    var playerSettingsUiState: PlayerSettingsUiState = PlayerSettingsUiState()
    var metaScreenSettingsUiState: MetaScreenSettingsUiState = MetaScreenSettingsUiState()
    var watchedUiState: WatchedUiState = WatchedUiState()
    var watchProgressUiState: WatchProgressUiState = WatchProgressUiState()
    var sourceStreamsState by mutableStateOf(StreamsUiState())
    var episodeStreamsRepoState by mutableStateOf(StreamsUiState())
    var metaUiState: MetaDetailsUiState = MetaDetailsUiState()
    var addonSubtitles: List<AddonSubtitle> = emptyList()
    var isLoadingAddonSubtitles: Boolean = false

    var horizontalSafePadding: Dp = 0.dp
    var metrics: PlayerLayoutMetrics = PlayerLayoutMetrics.fromWidth(0.dp)
    var sliderEdgePadding: Dp = 0.dp
    var overlayBottomPadding: Dp = 0.dp
    var sideGestureSystemEdgeExclusionPx: Float = 0f
    var resizeModeFitLabel: String = ""
    var resizeModeFillLabel: String = ""
    var resizeModeZoomLabel: String = ""
    var downloadedLabel: String = ""
    var airsPrefix: String = ""
    var tbaLabel: String = ""
    var genericUnknownLabel: String = ""
    var parentalGuideLabels: ParentalGuideLabels = ParentalGuideLabels("", "", "", "", "", "", "", "")

    var gestureController: PlayerGestureController? = null

    var controlsActivityTick by mutableStateOf(0)
    var controlsVisible by mutableStateOf(false)
    var showRemainingTime by mutableStateOf(false)
    var playerControlsLocked by mutableStateOf(false)
    var activeSourceUrl by mutableStateOf(args.sourceUrl)
    var activeSourceAudioUrl by mutableStateOf(args.sourceAudioUrl)
    var activeSourceHeaders by mutableStateOf(sanitizePlaybackHeaders(args.sourceHeaders))
    var activeSourceResponseHeaders by mutableStateOf(sanitizePlaybackResponseHeaders(args.sourceResponseHeaders))
    var activeStreamType by mutableStateOf(args.streamType)
    var activeSourceIdentityKey by mutableStateOf(
        args.sourceUrl.trim().takeIf { it.isNotBlank() }?.let { url -> "url:$url" },
    )
    var activeStreamTitle by mutableStateOf(args.streamTitle)
    var activeStreamSubtitle by mutableStateOf(args.streamSubtitle)
    var activeProviderName by mutableStateOf(args.providerName)
    var activeProviderAddonId by mutableStateOf(args.providerAddonId)
    var currentStreamBingeGroup by mutableStateOf(args.initialBingeGroup)
    var activeSeasonNumber by mutableStateOf(args.seasonNumber)
    var activeEpisodeNumber by mutableStateOf(args.episodeNumber)
    var activeEpisodeTitle by mutableStateOf(args.episodeTitle)
    var activeEpisodeThumbnail by mutableStateOf(args.episodeThumbnail)
    var activePauseDescription by mutableStateOf(args.pauseDescription)
    var activeVideoId by mutableStateOf(args.videoId)
    var activeInitialPositionMs by mutableStateOf(args.initialPositionMs)
    var activeInitialProgressFraction by mutableStateOf(args.initialProgressFraction)
    var shouldPlay by mutableStateOf(true)
    var resizeMode by mutableStateOf(playerSettingsUiState.resizeMode)
    var layoutSize by mutableStateOf(IntSize.Zero)
    var playbackSnapshot by mutableStateOf(PlayerPlaybackSnapshot())
    var playbackSnapshotKey by mutableStateOf<PlaybackKey?>(null)
    var playerController by mutableStateOf<PlayerEngineController?>(null)
    var playerControllerSourceUrl by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var isScrubbingTimeline by mutableStateOf(false)
    var scrubbingPositionMs by mutableStateOf<Long?>(null)
    var pausedOverlayVisible by mutableStateOf(false)
    var gestureFeedback by mutableStateOf<GestureFeedbackState?>(null)
    var liveGestureFeedback by mutableStateOf<GestureFeedbackState?>(null)
    var renderedGestureFeedback by mutableStateOf<GestureFeedbackState?>(null)
    var lockedOverlayVisible by mutableStateOf(false)
    var gestureMessageJob by mutableStateOf<Job?>(null)
    var accumulatedSeekResetJob by mutableStateOf<Job?>(null)
    var seekProgressSyncJob by mutableStateOf<Job?>(null)
    var accumulatedSeekState by mutableStateOf<PlayerAccumulatedSeekState?>(null)
    var initialLoadCompleted by mutableStateOf(false)
    var speedBoostRestoreSpeed by mutableStateOf<Float?>(null)
    var isHoldToSpeedGestureActive by mutableStateOf(false)
    var initialSeekApplied by mutableStateOf(
        initialPositionMs <= 0L && ((initialProgressFraction ?: 0f) <= 0f),
    )
    var lastProgressPersistEpochMs by mutableStateOf(0L)
    var previousIsPlaying by mutableStateOf(false)
    var hasRequestedScrobbleStartForCurrentItem by mutableStateOf(false)
    var scrobbleStartRequestGeneration by mutableStateOf(0L)
    var pendingSeekScrobbleRestart by mutableStateOf(false)
    var hasSentCompletionScrobbleForCurrentItem by mutableStateOf(false)
    var currentTrackingMedia by mutableStateOf<TrackingMediaReference?>(null)

    var showSourcesPanel by mutableStateOf(false)
    var showEpisodesPanel by mutableStateOf(false)
    var showChannelsPanel by mutableStateOf(false)
    var showSubmitIntroModal by mutableStateOf(false)
    var submitIntroSegmentType by mutableStateOf("intro")
    var submitIntroStartTimeStr by mutableStateOf("00:00")
    var submitIntroEndTimeStr by mutableStateOf("00:00")
    var episodeStreamsPanelState by mutableStateOf(EpisodeStreamsPanelState())
    var playerMetaVideos by mutableStateOf<List<MetaVideo>>(emptyList())
    var playerMeta by mutableStateOf<MetaDetails?>(null)
    var skipIntervals by mutableStateOf<List<SkipInterval>>(emptyList())
    val autoSkippedIntervals = mutableSetOf<SkipInterval>()
    var lastManualSkipSeekPositions by mutableStateOf<Pair<Long, Long>?>(null)
    var activeSkipInterval by mutableStateOf<SkipInterval?>(null)
    var skipIntervalDismissed by mutableStateOf(false)
    var parentalWarnings by mutableStateOf<List<ParentalWarning>>(emptyList())
    var showParentalGuide by mutableStateOf(false)
    var parentalGuideHasShown by mutableStateOf(false)
    var playbackStartedForParentalGuide by mutableStateOf(false)
    var nextEpisodeInfo by mutableStateOf<NextEpisodeInfo?>(null)
    var showNextEpisodeCard by mutableStateOf(false)
    var nextEpisodeCardDismissed by mutableStateOf(false)
    var nextEpisodeAutoPlaySearching by mutableStateOf(false)
    var nextEpisodeAutoPlaySourceName by mutableStateOf<String?>(null)
    var nextEpisodeAutoPlayCountdown by mutableStateOf<Int?>(null)
    var nextEpisodeAutoPlayJob by mutableStateOf<Job?>(null)
    var nextEpisodeAutoPlayAutomatic by mutableStateOf(false)
    var nextEpisodePreloadJob by mutableStateOf<Job?>(null)
    var nextEpisodePreloadTriggered by mutableStateOf(false)
    var credentialRefreshJob by mutableStateOf<Job?>(null)
    var credentialRefreshAttemptedSourceUrl by mutableStateOf<String?>(null)

    var showAudioModal by mutableStateOf(false)
    var showSubtitleModal by mutableStateOf(false)
    var showVideoSettingsModal by mutableStateOf(false)
    var audioTracks by mutableStateOf<List<AudioTrack>>(emptyList())
    var subtitleTracks by mutableStateOf<List<SubtitleTrack>>(emptyList())
    var selectedAudioIndex by mutableStateOf(-1)
    var selectedSubtitleIndex by mutableStateOf(-1)
    var selectedAddonSubtitleId by mutableStateOf<String?>(null)
    var useCustomSubtitles by mutableStateOf(false)
    var preferredAudioSelectionApplied by mutableStateOf(false)
    var appliedAudioPreferences: AppliedAudioPreferences? = null
    var preferredSubtitleSelectionApplied by mutableStateOf(false)
    var isUserExplicitAudioSelection by mutableStateOf(false)
    var isUserExplicitSubtitleSelection by mutableStateOf(false)
    var hasScannedTextTracksOnce by mutableStateOf(false)
    var autoFetchedAddonSubtitlesForKey by mutableStateOf<String?>(null)
    var trackPreferenceRestoreApplied by mutableStateOf(false)
    var subtitleDelayMs by mutableStateOf(0)
    var subtitleAutoSyncState by mutableStateOf(SubtitleAutoSyncUiState())

    var lastSyncedSettingsResizeMode: PlayerResizeMode? = null
    var lastResetPlaybackIdentity: PlaybackKey? = null
    var lastResetVideoIdentity: String? = null
}
