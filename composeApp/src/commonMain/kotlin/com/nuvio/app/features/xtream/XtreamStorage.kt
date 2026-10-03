package com.nuvio.app.features.xtream

internal expect object XtreamStorage {
    fun loadServersJson(): String?
    fun saveServersJson(json: String)
}
