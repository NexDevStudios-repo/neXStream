package com.nuvio.app.features.search

import co.touchlab.kermit.Logger
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.xtream.XtreamApi
import com.nuvio.app.features.xtream.XtreamRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SearchRepository {
    private val log = Logger.withTag("SearchRepository")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private val _discoverUiState = MutableStateFlow(DiscoverUiState())
    val discoverUiState: StateFlow<DiscoverUiState> = _discoverUiState.asStateFlow()

    private var activeSearchJob: Job? = null
    private var activeDiscoverJob: Job? = null

    fun search(query: String, forceRefresh: Boolean = false) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            clear()
            return
        }

        activeSearchJob?.cancel()
        _uiState.value = SearchUiState(isLoading = true)
        activeSearchJob = scope.launch {
            try {
                val results = XtreamRepository.search(normalizedQuery)
                if (results.isEmpty()) {
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        emptyStateReason = SearchEmptyStateReason.NoResults,
                    )
                } else {
                    val section = HomeCatalogSection(
                        key = "xtream_search",
                        title = "Resultados",
                        subtitle = "Xtream Codes",
                        addonName = "Xtream",
                        target = CatalogTarget.XtreamCategory(
                            contentType = "search",
                            categoryId = "search"
                        ),
                        items = results,
                        availableItemCount = results.size,
                        hasMore = false,
                    )
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        sections = listOf(section),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Error searching Xtream" }
                _uiState.value = SearchUiState(
                    isLoading = false,
                    emptyStateReason = SearchEmptyStateReason.RequestFailed,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun clear() {
        activeSearchJob?.cancel()
        _uiState.value = SearchUiState()
    }

    fun reset() {
        activeSearchJob?.cancel()
        activeDiscoverJob?.cancel()
        _uiState.value = SearchUiState()
        refreshDiscover(forceRefresh = true)
    }

    fun refreshDiscover(forceRefresh: Boolean = false) {
        val server = XtreamRepository.activeServer.value
        if (server == null) {
            _discoverUiState.value = DiscoverUiState(
                typeOptions = listOf("channel", "movie", "series"),
                selectedType = "movie",
                emptyStateReason = DiscoverEmptyStateReason.NoDiscoverCatalogs,
                errorMessage = "No hay ningún servidor IPTV activo. Ve a Ajustes > Servidores IPTV para agregar uno."
            )
            return
        }

        val currentType = _discoverUiState.value.selectedType ?: "movie"
        loadDiscoverForType(currentType, forceRefresh = forceRefresh)
    }

    fun selectDiscoverType(type: String) {
        if (_discoverUiState.value.selectedType == type) return
        loadDiscoverForType(type, forceRefresh = false)
    }

    fun selectDiscoverCatalog(catalogKey: String) {
        val type = _discoverUiState.value.selectedType ?: "movie"
        loadDiscoverItems(type, catalogKey)
    }

    fun selectDiscoverGenre(genre: String?) {
        // Xtream categories are already direct genres/catalogs
    }

    fun loadMoreDiscover() {
        // No pagination needed for category listing
    }

    private fun loadDiscoverForType(type: String, forceRefresh: Boolean) {
        val server = XtreamRepository.activeServer.value ?: return
        activeDiscoverJob?.cancel()

        _discoverUiState.value = _discoverUiState.value.copy(
            isLoading = true,
            selectedType = type,
            typeOptions = listOf("channel", "movie", "series"),
            emptyStateReason = null,
            errorMessage = null,
        )

        activeDiscoverJob = scope.launch {
            try {
                val categories = when (type) {
                    "channel", "tv", "live" -> XtreamRepository.getLiveCategories(server)
                    "series" -> XtreamRepository.getSeriesCategories(server)
                    else -> XtreamRepository.getVodCategories(server)
                }

                val options = categories.map { cat ->
                    DiscoverCatalogOption(
                        key = cat.categoryId,
                        addonName = "Xtream",
                        manifestUrl = "",
                        type = type,
                        catalogId = cat.categoryId,
                        catalogName = cat.categoryName,
                    )
                }

                if (options.isEmpty()) {
                    _discoverUiState.value = _discoverUiState.value.copy(
                        isLoading = false,
                        catalogOptions = emptyList(),
                        items = emptyList(),
                        emptyStateReason = DiscoverEmptyStateReason.NoResults,
                    )
                    return@launch
                }

                val selectedCatalog = options.first()
                val items = XtreamRepository.fetchCategoryItems(
                    contentType = if (type == "channel") "tv" else type,
                    categoryId = selectedCatalog.catalogId
                )

                _discoverUiState.value = _discoverUiState.value.copy(
                    isLoading = false,
                    selectedType = type,
                    typeOptions = listOf("channel", "movie", "series"),
                    catalogOptions = options,
                    selectedCatalogKey = selectedCatalog.key,
                    items = items,
                    emptyStateReason = if (items.isEmpty()) DiscoverEmptyStateReason.NoResults else null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Error loading discover categories" }
                _discoverUiState.value = _discoverUiState.value.copy(
                    isLoading = false,
                    emptyStateReason = DiscoverEmptyStateReason.RequestFailed,
                    errorMessage = e.message ?: "Error al cargar categorías",
                )
            }
        }
    }

    private fun loadDiscoverItems(type: String, categoryId: String) {
        activeDiscoverJob?.cancel()
        _discoverUiState.value = _discoverUiState.value.copy(
            isLoading = true,
            selectedCatalogKey = categoryId,
        )

        activeDiscoverJob = scope.launch {
            try {
                val items = XtreamRepository.fetchCategoryItems(
                    contentType = if (type == "channel") "tv" else type,
                    categoryId = categoryId
                )
                _discoverUiState.value = _discoverUiState.value.copy(
                    isLoading = false,
                    selectedCatalogKey = categoryId,
                    items = items,
                    emptyStateReason = if (items.isEmpty()) DiscoverEmptyStateReason.NoResults else null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Error loading category items" }
                _discoverUiState.value = _discoverUiState.value.copy(
                    isLoading = false,
                    emptyStateReason = DiscoverEmptyStateReason.RequestFailed,
                    errorMessage = e.message,
                )
            }
        }
    }
}
