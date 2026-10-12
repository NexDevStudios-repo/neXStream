package com.nuvio.app.features.xtream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class XtreamPreferencesState(
    val outputFormat: String = "m3u8",
    val bufferSeconds: Int = 2,
    val userAgent: String = "neXStream",
    val epgTimeShiftHours: Int = 0,
    val autoSyncOnLaunch: Boolean = true,
)

object XtreamPreferencesRepository {
    private val _state = MutableStateFlow(XtreamPreferencesState())
    val state: StateFlow<XtreamPreferencesState> = _state.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        val format = XtreamStorage.loadStreamOutputFormat() ?: "m3u8"
        val buffer = XtreamStorage.loadBufferSeconds() ?: 2
        val ua = XtreamStorage.loadUserAgent() ?: "neXStream"
        val epgShift = XtreamStorage.loadEpgTimeShift() ?: 0
        val autoSync = XtreamStorage.loadAutoSyncOnLaunch() ?: true

        _state.value = XtreamPreferencesState(
            outputFormat = format,
            bufferSeconds = buffer,
            userAgent = ua,
            epgTimeShiftHours = epgShift,
            autoSyncOnLaunch = autoSync,
        )
    }

    fun setOutputFormat(format: String) {
        ensureLoaded()
        _state.value = _state.value.copy(outputFormat = format)
        XtreamStorage.saveStreamOutputFormat(format)
    }

    fun setBufferSeconds(seconds: Int) {
        ensureLoaded()
        _state.value = _state.value.copy(bufferSeconds = seconds)
        XtreamStorage.saveBufferSeconds(seconds)
    }

    fun setUserAgent(ua: String) {
        ensureLoaded()
        _state.value = _state.value.copy(userAgent = ua)
        XtreamStorage.saveUserAgent(ua)
    }

    fun setEpgTimeShiftHours(hours: Int) {
        ensureLoaded()
        _state.value = _state.value.copy(epgTimeShiftHours = hours)
        XtreamStorage.saveEpgTimeShift(hours)
    }

    fun setAutoSyncOnLaunch(enabled: Boolean) {
        ensureLoaded()
        _state.value = _state.value.copy(autoSyncOnLaunch = enabled)
        XtreamStorage.saveAutoSyncOnLaunch(enabled)
    }
}
