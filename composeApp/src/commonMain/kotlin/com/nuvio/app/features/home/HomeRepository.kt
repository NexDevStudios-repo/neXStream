package com.nuvio.app.features.home

import com.nuvio.app.features.xtream.XtreamRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object HomeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var isInitialized = false

    init {
        initialize()
    }

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        scope.launch {
            XtreamRepository.initialize()
            com.nuvio.app.features.xtream.XtreamPreferencesRepository.ensureLoaded()
            val autoSync = com.nuvio.app.features.xtream.XtreamPreferencesRepository.state.value.autoSyncOnLaunch
            XtreamRepository.activeServer.collect { server ->
                if (server != null) {
                    if (autoSync) {
                        XtreamRepository.clearCache()
                    }
                    refreshXtream(force = true)
                } else {
                    _uiState.value = HomeUiState()
                }
            }
        }
    }

    fun refresh(force: Boolean = false) {
        refreshXtream(force)
    }

    fun applyCurrentSettings() {
        // No-op for single-mode Xtream
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        _uiState.value = HomeUiState()
    }

    fun refreshXtream(force: Boolean = false) {
        val server = XtreamRepository.activeServer.value
        if (server == null) {
            _uiState.value = HomeUiState()
            return
        }
        activeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        activeJob = scope.launch {
            try {
                val sections = XtreamRepository.fetchHomeCatalogSections()
                val heroItems = sections.flatMap { it.items.filter { item -> item.type == "movie" || item.type == "series" } }.take(8)
                _uiState.value = HomeUiState(
                    isLoading = false,
                    heroItems = heroItems,
                    sections = sections,
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    heroItems = emptyList(),
                    sections = emptyList(),
                    errorMessage = e.message ?: "Error cargando contenido de Xtream",
                )
            }
        }
    }
}
