package com.nuvio.app.core.sync

import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.tmdb.TmdbSettingsRepository

object ProviderCredentialSync {
    fun startObserving() {
        ensureRepositoriesLoaded()
    }

    fun clearAccountState() {}

    internal fun onProfileChanged() {
        ensureRepositoriesLoaded()
    }

    suspend fun syncFromRemote(profileId: Int): Boolean {
        ensureRepositoriesLoaded()
        return false
    }

    private fun ensureRepositoriesLoaded() {
        TmdbSettingsRepository.ensureLoaded()
        MdbListSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.ensureLoaded()
    }
}
