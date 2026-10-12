package com.nuvio.app.features.xtream

internal expect object XtreamStorage {
    fun loadServersJson(profileKey: String = "1"): String?
    fun saveServersJson(json: String, profileKey: String = "1")
    fun loadActiveServerId(profileKey: String = "1"): String?
    fun saveActiveServerId(serverId: String?, profileKey: String = "1")
    fun saveM3uContent(serverId: String, content: String)
    fun loadM3uContent(serverId: String): String?
    fun deleteM3uContent(serverId: String)
    fun loadStreamOutputFormat(): String?
    fun saveStreamOutputFormat(format: String)
    fun loadBufferSeconds(): Int?
    fun saveBufferSeconds(seconds: Int)
    fun loadUserAgent(): String?
    fun saveUserAgent(userAgent: String)
    fun loadEpgTimeShift(): Int?
    fun saveEpgTimeShift(hours: Int)
    fun loadAutoSyncOnLaunch(): Boolean?
    fun saveAutoSyncOnLaunch(enabled: Boolean)
}
