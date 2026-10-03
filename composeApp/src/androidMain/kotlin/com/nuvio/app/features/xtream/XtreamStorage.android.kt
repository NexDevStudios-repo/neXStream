package com.nuvio.app.features.xtream

import android.content.Context
import android.content.SharedPreferences

actual object XtreamStorage {
    private const val PREFS_NAME = "nexstream_xtream_servers"
    private const val KEY_SERVERS_JSON = "servers_json"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadServersJson(): String? =
        preferences?.getString(KEY_SERVERS_JSON, null)

    actual fun saveServersJson(json: String) {
        preferences?.edit()?.putString(KEY_SERVERS_JSON, json)?.apply()
    }
}
