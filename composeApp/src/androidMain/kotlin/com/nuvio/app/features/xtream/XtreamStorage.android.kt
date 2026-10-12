package com.nuvio.app.features.xtream

import android.content.Context
import android.content.SharedPreferences

actual object XtreamStorage {
    private const val PREFS_NAME = "nexstream_xtream_servers"
    private const val KEY_SERVERS_JSON = "servers_json"
    private const val KEY_ACTIVE_SERVER_ID = "active_server_id"
    private const val KEY_STREAM_FORMAT = "stream_output_format"
    private const val KEY_BUFFER_SECONDS = "buffer_seconds"
    private const val KEY_USER_AGENT = "user_agent"
    private const val KEY_EPG_TIMESHIFT = "epg_timeshift_hours"
    private const val KEY_AUTO_SYNC = "auto_sync_on_launch"

    private var preferences: SharedPreferences? = null
    private var filesDir: java.io.File? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        filesDir = context.filesDir
    }

    private fun serversKey(profileKey: String): String =
        if (profileKey == "1") KEY_SERVERS_JSON else "${KEY_SERVERS_JSON}_$profileKey"

    private fun activeServerKey(profileKey: String): String =
        if (profileKey == "1") KEY_ACTIVE_SERVER_ID else "${KEY_ACTIVE_SERVER_ID}_$profileKey"

    actual fun loadServersJson(profileKey: String): String? {
        val specific = preferences?.getString(serversKey(profileKey), null)
        if (!specific.isNullOrBlank()) return specific
        return preferences?.getString(KEY_SERVERS_JSON, null)
    }

    actual fun saveServersJson(json: String, profileKey: String) {
        preferences?.edit()?.putString(serversKey(profileKey), json)?.apply()
        if (profileKey == "1") {
            preferences?.edit()?.putString(KEY_SERVERS_JSON, json)?.apply()
        }
    }

    actual fun loadActiveServerId(profileKey: String): String? {
        val specific = preferences?.getString(activeServerKey(profileKey), null)
        if (!specific.isNullOrBlank()) return specific
        return preferences?.getString(KEY_ACTIVE_SERVER_ID, null)
    }

    actual fun saveActiveServerId(serverId: String?, profileKey: String) {
        preferences?.edit()?.putString(activeServerKey(profileKey), serverId)?.apply()
        if (profileKey == "1") {
            preferences?.edit()?.putString(KEY_ACTIVE_SERVER_ID, serverId)?.apply()
        }
    }

    actual fun saveM3uContent(serverId: String, content: String) {
        val dir = filesDir ?: return
        try {
            java.io.File(dir, "m3u_$serverId.m3u").writeText(content)
        } catch (_: Exception) {}
    }

    actual fun loadM3uContent(serverId: String): String? {
        val dir = filesDir ?: return null
        val file = java.io.File(dir, "m3u_$serverId.m3u")
        return if (file.exists()) {
            try { file.readText() } catch (_: Exception) { null }
        } else null
    }

    actual fun deleteM3uContent(serverId: String) {
        val dir = filesDir ?: return
        try {
            java.io.File(dir, "m3u_$serverId.m3u").delete()
        } catch (_: Exception) {}
    }

    actual fun loadStreamOutputFormat(): String? =
        preferences?.getString(KEY_STREAM_FORMAT, null)

    actual fun saveStreamOutputFormat(format: String) {
        preferences?.edit()?.putString(KEY_STREAM_FORMAT, format)?.apply()
    }

    actual fun loadBufferSeconds(): Int? =
        if (preferences?.contains(KEY_BUFFER_SECONDS) == true) preferences?.getInt(KEY_BUFFER_SECONDS, 2) else null

    actual fun saveBufferSeconds(seconds: Int) {
        preferences?.edit()?.putInt(KEY_BUFFER_SECONDS, seconds)?.apply()
    }

    actual fun loadUserAgent(): String? =
        preferences?.getString(KEY_USER_AGENT, null)

    actual fun saveUserAgent(userAgent: String) {
        preferences?.edit()?.putString(KEY_USER_AGENT, userAgent)?.apply()
    }

    actual fun loadEpgTimeShift(): Int? =
        if (preferences?.contains(KEY_EPG_TIMESHIFT) == true) preferences?.getInt(KEY_EPG_TIMESHIFT, 0) else null

    actual fun saveEpgTimeShift(hours: Int) {
        preferences?.edit()?.putInt(KEY_EPG_TIMESHIFT, hours)?.apply()
    }

    actual fun loadAutoSyncOnLaunch(): Boolean? =
        if (preferences?.contains(KEY_AUTO_SYNC) == true) preferences?.getBoolean(KEY_AUTO_SYNC, true) else null

    actual fun saveAutoSyncOnLaunch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_AUTO_SYNC, enabled)?.apply()
    }
}
