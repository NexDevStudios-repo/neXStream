package com.nuvio.app.features.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.ui.DisintegrationRequest
import com.nuvio.app.core.ui.DisintegratingContainer
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioViewAllPillSize
import com.nuvio.app.core.ui.ScopedDisintegrationTracker
import com.nuvio.app.core.ui.nuvioConsumePointerEvents
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.home.components.posterGridColumnCountForWidth
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onPosterClick: ((LibraryItem) -> Unit)? = null,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)? = null,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)? = null,
    onDownloadsClick: (() -> Unit)? = null,
    disintegrationRequest: DisintegrationRequest<String>? = null,
) {
    val uiState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val displaySettings by remember {
        LibraryDisplaySettingsRepository.ensureLoaded()
        LibraryDisplaySettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    var observedOfflineState by remember { mutableStateOf(false) }
    var selectedLibrarySectionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibraryType by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    ScreenActivityEffect(listState) { screenActive ->
        if (!screenActive) listState.stopScroll()
    }
    val isRemoteSource = uiState.sourceMode != LibrarySourceMode.LOCAL
    val effectiveSortOption = effectiveLibrarySortOption(
        selected = displaySettings.sortOption,
        sourceMode = uiState.sourceMode,
    )
    val orderListKeys = if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
        uiState.sections.map { it.type }
    } else {
        listOfNotNull(
            uiState.sections.firstOrNull { it.type == selectedLibrarySectionKey }?.type
                ?: uiState.sections.firstOrNull()?.type
        )
    }
    val providerOrders = rememberLibraryProviderOrders(uiState.sourceMode, orderListKeys, effectiveSortOption)
    val visibleSortOption = if (providerOrders.failed) LibrarySortOption.DEFAULT else effectiveSortOption
    val sortedSections = remember(uiState.sections, displaySettings, uiState.sourceMode, providerOrders) {
        if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
            sortLibrarySections(
                sections = uiState.sections,
                selected = visibleSortOption,
                sourceMode = uiState.sourceMode,
                providerOrders = providerOrders.ranks,
            )
        } else {
            emptyList()
        }
    }
    val verticalProjection = remember(
        uiState.sections,
        uiState.sourceMode,
        selectedLibrarySectionKey,
        selectedLibraryType,
        displaySettings,
        providerOrders,
    ) {
        if (displaySettings.layoutMode == LibraryLayoutMode.VERTICAL) {
            buildLibraryVerticalProjection(
                sections = uiState.sections,
                sourceMode = uiState.sourceMode,
                selectedSectionKey = selectedLibrarySectionKey,
                selectedType = selectedLibraryType,
                sortOption = visibleSortOption,
                providerOrders = providerOrders.ranks,
            )
        } else {
            LibraryVerticalProjection(
                availableSections = emptyList(),
                selectedSectionKey = null,
                availableTypes = emptyList(),
                selectedType = null,
                entries = emptyList(),
            )
        }
    }
    val retryLibraryLoad: () -> Unit = {
        NetworkStatusRepository.requestRefresh(force = true)
        coroutineScope.launch {
            LibraryRepository.pullFromServer(
                profileId = ProfileRepository.activeProfileId,
                refreshIntent = TrackingRefreshIntent.USER_INITIATED,
            )
        }
    }

    ScreenActivityEffect(networkStatusUiState.condition, isRemoteSource) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@ScreenActivityEffect
                observedOfflineState = false
                if (isRemoteSource) {
                    coroutineScope.launch {
                        LibraryRepository.pullFromServer(ProfileRepository.activeProfileId)
                    }
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    ScreenActivityEffect(scrollToTopRequests) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            listState.animateScrollToItem(0)
        }
    }

    val disintegration = remember { LibraryDisintegrationHolder() }
    val librarySectionsDisplay = if (
        displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL &&
        uiState.isLoaded &&
        sortedSections.isNotEmpty()
    ) {
        disintegration.sync(
            sourceMode = uiState.sourceMode,
            sections = sortedSections,
            previewLimit = LIBRARY_SECTION_PREVIEW_LIMIT,
            request = disintegrationRequest,
        )
    } else {
        disintegration.reset()
        emptyList()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gridColumns = remember(maxWidth) { posterGridColumnCountForWidth(maxWidth) }

        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = 0.dp,
            listState = listState,
        ) {
            stickyHeader {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.background)
                            .nuvioConsumePointerEvents(),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        NuvioScreenHeader(
                            title = when (uiState.sourceMode) {
                                LibrarySourceMode.LOCAL -> stringResource(Res.string.library_title)
                                LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_title)
                                LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_title)
                                LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_title)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            actions = {
                                LibraryListManagementButton()
                                val targetLayout = if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
                                    LibraryLayoutMode.VERTICAL
                                } else {
                                    LibraryLayoutMode.HORIZONTAL
                                }
                                IconButton(
                                    onClick = {
                                        LibraryDisplaySettingsRepository.setLayoutMode(targetLayout)
                                    },
                                ) {
                                    Crossfade(
                                        targetState = targetLayout,
                                        animationSpec = tween(durationMillis = 140),
                                        label = "libraryLayoutAction",
                                    ) { animatedTargetLayout ->
                                        Icon(
                                            imageVector = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                                Icons.Rounded.GridView
                                            } else {
                                                Icons.Rounded.ViewAgenda
                                            },
                                            contentDescription = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                                stringResource(Res.string.library_layout_show_vertical)
                                            } else {
                                                stringResource(Res.string.library_layout_show_horizontal)
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (onDownloadsClick != null) {
                                    LibraryDownloadsButton(onClick = onDownloadsClick)
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            when {
                !uiState.isLoaded || (uiState.isLoading && uiState.sections.isEmpty()) -> {
                    if (displaySettings.layoutMode == LibraryLayoutMode.VERTICAL) {
                        libraryVerticalSkeletonItems(gridColumns)
                    } else {
                        items(3) {
                            HomeSkeletonRow(
                                horizontalPadding = 16.dp,
                            )
                        }
                    }
                }

                !uiState.errorMessage.isNullOrBlank() && uiState.sections.isEmpty() -> {
                    item {
                        if (networkStatusUiState.isOfflineLike) {
                            NuvioNetworkOfflineCard(
                                condition = networkStatusUiState.condition,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onRetry = retryLibraryLoad,
                            )
                        } else {
                            HomeEmptyStateCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = when (uiState.sourceMode) {
                                    LibrarySourceMode.LOCAL -> stringResource(Res.string.library_load_failed)
                                    LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_load_failed)
                                    LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_load_failed)
                                    LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_load_failed)
                                },
                                message = uiState.errorMessage.orEmpty(),
                                actionLabel = stringResource(Res.string.action_retry),
                                onActionClick = retryLibraryLoad,
                            )
                        }
                    }
                }

                uiState.sections.isEmpty() -> {
                    item {
                        HomeEmptyStateCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = when (uiState.sourceMode) {
                                LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_title)
                                LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_title)
                                LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_title)
                                LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_empty_title)
                            },
                            message = when (uiState.sourceMode) {
                                LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_message)
                                LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_message)
                                LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_message)
                                LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_empty_message)
                            },
                        )
                    }
                }

                else -> {
                    item(
                        key = "library-saved-controls:${uiState.sourceMode}:" +
                            "${displaySettings.layoutMode}:$effectiveSortOption",
                    ) {
                        LibrarySavedControls(
                            layoutMode = displaySettings.layoutMode,
                            sourceMode = uiState.sourceMode,
                            sortOption = effectiveSortOption,
                            verticalProjection = verticalProjection,
                            onSectionSelected = { sectionKey ->
                                selectedLibrarySectionKey = sectionKey
                                selectedLibraryType = null
                            },
                            onTypeSelected = { type -> selectedLibraryType = type },
                            onSortSelected = LibraryDisplaySettingsRepository::setSortOption,
                            modifier = libraryContentTransitionModifier()
                                .padding(horizontal = 16.dp),
                        )
                    }
                    when (displaySettings.layoutMode) {
                        LibraryLayoutMode.HORIZONTAL -> librarySections(
                            displaySections = librarySectionsDisplay,
                            watchedKeys = watchedUiState.watchedKeys,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            sortOption = effectiveSortOption,
                            onPosterClick = onPosterClick,
                            onSectionViewAllClick = onSectionViewAllClick,
                            onPosterLongClick = onPosterLongClick,
                            onDisintegrated = disintegration::onExited,
                        )
                        LibraryLayoutMode.VERTICAL -> libraryVerticalContent(
                            projection = verticalProjection,
                            columns = gridColumns,
                            watchedKeys = watchedUiState.watchedKeys,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            onPosterClick = onPosterClick,
                            onPosterLongClick = onPosterLongClick,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.librarySections(
    displaySections: List<LibraryDisplaySection>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    sortOption: LibrarySortOption,
    onPosterClick: ((LibraryItem) -> Unit)?,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)?,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)?,
    onDisintegrated: (String) -> Unit,
) {
    items(
        items = displaySections,
        key = { section -> "library-horizontal:${section.type}" },
    ) { section ->
        NuvioShelfSection(
            title = section.displayTitle,
            entries = section.previewEntries,
            modifier = libraryContentTransitionModifier(),
            headerHorizontalPadding = 16.dp,
            rowContentPadding = PaddingValues(horizontal = 16.dp),
            onViewAllClick = section.source
                ?.takeIf { it.items.size > LIBRARY_SECTION_PREVIEW_LIMIT }
                ?.let { source -> onSectionViewAllClick?.let { { it(source, sortOption) } } },
            viewAllPillSize = NuvioViewAllPillSize.Compact,
            key = { entry -> entry.globalKey },
            animatePlacement = true,
        ) { entry ->
            val item = entry.item
            val posterItem = item.toMetaPreview()
            val entrySource = entry.section
            DisintegratingContainer(
                disintegrating = entry.exiting,
                onDisintegrated = { onDisintegrated(entry.globalKey) },
            ) {
                HomePosterCard(
                    item = posterItem,
                    isWatched = WatchingState.isPosterWatched(
                        watchedKeys = watchedKeys,
                        item = posterItem,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    ),
                    onClick = if (entry.exiting) null else onPosterClick?.let { { it(item) } },
                    onLongClick = if (entry.exiting || entrySource == null) {
                        null
                    } else {
                        onPosterLongClick?.let { { it(item, entrySource) } }
                    },
                )
            }
        }
    }
}

private const val LIBRARY_SECTION_PREVIEW_LIMIT = 18

private data class LibraryDisplayEntry(
    val globalKey: String,
    val item: LibraryItem,
    val section: LibrarySection?,
    val exiting: Boolean,
)

private data class LibraryDisplaySection(
    val source: LibrarySection?,
    val type: String,
    val displayTitle: String,
    val previewEntries: List<LibraryDisplayEntry>,
)

private class LibraryExitingEntry(
    val item: LibraryItem,
    val sectionType: String,
    val sectionTitle: String,
    val index: Int,
)

private class LibraryDisintegrationHolder {
    private val tracker = ScopedDisintegrationTracker<LibrarySourceMode, String, LibraryExitingEntry> { entry ->
        librarySectionItemKey(entry.sectionType, entry.item)
    }

    fun onExited(globalKey: String) {
        tracker.onDisintegrated(globalKey)
    }

    fun reset() {
        tracker.reset()
    }

    fun sync(
        sourceMode: LibrarySourceMode,
        sections: List<LibrarySection>,
        previewLimit: Int,
        request: DisintegrationRequest<String>?,
    ): List<LibraryDisplaySection> {
        val current = ArrayList<LibraryExitingEntry>()
        sections.forEach { section ->
            section.items.take(previewLimit).forEachIndexed { index, item ->
                current += LibraryExitingEntry(item, section.type, section.displayTitle, index)
            }
        }
        val exitingBySection = tracker.sync(sourceMode, current, request)
            .asSequence()
            .filter { entry -> entry.exiting }
            .map { entry -> entry.item }
            .groupBy { entry -> entry.sectionType }
        val seenTypes = HashSet<String>(sections.size)
        val result = ArrayList<LibraryDisplaySection>(sections.size + 1)

        for (section in sections) {
            seenTypes += section.type
            val entries = ArrayList<LibraryDisplayEntry>(previewLimit + 1)
            section.items.take(previewLimit).forEach { item ->
                entries += LibraryDisplayEntry(
                    globalKey = librarySectionItemKey(section.type, item),
                    item = item,
                    section = section,
                    exiting = false,
                )
            }
            exitingBySection[section.type]?.sortedBy { it.index }?.forEach { ex ->
                val key = librarySectionItemKey(section.type, ex.item)
                if (entries.none { it.globalKey == key }) {
                    entries.add(
                        ex.index.coerceIn(0, entries.size),
                        LibraryDisplayEntry(key, ex.item, section, exiting = true),
                    )
                }
            }
            result += LibraryDisplaySection(section, section.type, section.displayTitle, entries)
        }

        for ((type, list) in exitingBySection) {
            if (type in seenTypes) continue
            val sorted = list.sortedBy { it.index }
            val entries = sorted.map { ex ->
                LibraryDisplayEntry(librarySectionItemKey(type, ex.item), ex.item, section = null, exiting = true)
            }
            result += LibraryDisplaySection(null, type, sorted.first().sectionTitle, entries)
        }

        return result
    }
}
