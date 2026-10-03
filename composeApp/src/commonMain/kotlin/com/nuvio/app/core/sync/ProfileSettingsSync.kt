package com.nuvio.app.core.sync

import com.nuvio.app.features.collection.CollectionMobileSettingsRepository
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.core.ui.CardDepthStyleRepository
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.core.poster.CustomPosterUrlRepository
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.features.streams.StreamBadgeSettingsRepository
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tracking.TrackingSettingsRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository

object ProfileSettingsSync {
    fun startObserving() {
        ensureRepositoriesLoaded()
        ProviderCredentialSync.startObserving()
    }

    fun clearAccountState() {
        ProviderCredentialSync.clearAccountState()
    }

    fun onProfileChanged() {
        startObserving()
    }

    suspend fun syncNow(force: Boolean = false) {
        ensureRepositoriesLoaded()
    }

    suspend fun pull(profileId: Int) {
        // Local-only
    }

    private fun ensureRepositoriesLoaded() {
        // Local-only
    }
}
