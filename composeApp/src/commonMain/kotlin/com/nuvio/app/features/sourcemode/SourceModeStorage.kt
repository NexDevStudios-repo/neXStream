package com.nuvio.app.features.sourcemode

internal expect object SourceModeStorage {
    fun loadSourceMode(): String?
    fun saveSourceMode(mode: String)
    fun loadActiveXtreamServerId(): String?
    fun saveActiveXtreamServerId(serverId: String?)
}
