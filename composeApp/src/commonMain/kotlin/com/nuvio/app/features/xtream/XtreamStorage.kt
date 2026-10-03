package com.nuvio.app.features.xtream

internal expect object XtreamStorage {
    fun loadServersJson(): String?
    fun saveServersJson(json: String)
    fun loadActiveServerId(): String?
    fun saveActiveServerId(serverId: String?)
}

