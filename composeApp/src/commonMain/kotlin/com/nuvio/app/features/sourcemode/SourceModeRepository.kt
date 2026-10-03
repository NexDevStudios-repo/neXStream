package com.nuvio.app.features.sourcemode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SourceModeRepository {
    private val _mode = MutableStateFlow(SourceMode.PLUGINS)
    val mode: StateFlow<SourceMode> = _mode.asStateFlow()

    private val _activeXtreamServerId = MutableStateFlow<String?>(null)
    val activeXtreamServerId: StateFlow<String?> = _activeXtreamServerId.asStateFlow()

    fun initialize() {
        val storedModeStr = SourceModeStorage.loadSourceMode()
        val storedMode = when (storedModeStr) {
            SourceMode.XTREAM.name -> SourceMode.XTREAM
            else -> SourceMode.PLUGINS
        }
        _mode.value = storedMode
        _activeXtreamServerId.value = SourceModeStorage.loadActiveXtreamServerId()
    }

    fun setSourceMode(newMode: SourceMode) {
        if (_mode.value == newMode) return
        _mode.value = newMode
        SourceModeStorage.saveSourceMode(newMode.name)
    }

    fun setActiveXtreamServerId(serverId: String?) {
        _activeXtreamServerId.value = serverId
        SourceModeStorage.saveActiveXtreamServerId(serverId)
    }
}
