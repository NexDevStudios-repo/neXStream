package com.nuvio.app.features.sourcemode

import android.content.Context
import android.content.SharedPreferences

actual object SourceModeStorage {
    private const val PREFS_NAME = "nexstream_sourcemode"
    private const val KEY_SOURCE_MODE = "source_mode"
    private const val KEY_ACTIVE_XTREAM_SERVER_ID = "active_xtream_server_id"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadSourceMode(): String? =
        preferences?.getString(KEY_SOURCE_MODE, null)

    actual fun saveSourceMode(mode: String) {
        preferences?.edit()?.putString(KEY_SOURCE_MODE, mode)?.apply()
    }

    actual fun loadActiveXtreamServerId(): String? =
        preferences?.getString(KEY_ACTIVE_XTREAM_SERVER_ID, null)

    actual fun saveActiveXtreamServerId(serverId: String?) {
        if (serverId != null) {
            preferences?.edit()?.putString(KEY_ACTIVE_XTREAM_SERVER_ID, serverId)?.apply()
        } else {
            preferences?.edit()?.remove(KEY_ACTIVE_XTREAM_SERVER_ID)?.apply()
        }
    }
}
