package com.nuvio.app.features.profiles

import co.touchlab.kermit.Logger
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.poster.CustomPosterUrlRepository
import com.nuvio.app.core.sync.ProfileSettingsSync
import com.nuvio.app.core.tracking.ensureTrackingProvidersRegistered
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.tracking.TrackingSettingsRepository
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.LibraryDisplaySettingsRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.HomeRepository
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.search.SearchHistoryRepository
import com.nuvio.app.features.search.SearchRepository
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.collection.CollectionMobileSettingsRepository
import com.nuvio.app.features.downloads.DownloadsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Serializable
private data class StoredProfilePayload(
    val userId: String,
    val activeProfileIndex: Int = 1,
    val hasEverSelectedProfile: Boolean = false,
    val rememberLastProfileEnabled: Boolean = false,
    val profiles: List<NuvioProfile> = emptyList(),
)

object ProfileRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("ProfileRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun localizedString(resource: StringResource): String = runBlocking { getString(resource) }

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var activeProfileIndex: Int = 1
    private var loadedCacheForUserId: String? = null

    val activeProfileId: Int get() = activeProfileIndex

    fun setRememberLastProfileEnabled(enabled: Boolean) {
        if (_state.value.rememberLastProfileEnabled == enabled) return

        _state.value = _state.value.copy(rememberLastProfileEnabled = enabled)
        persist()
    }

    fun loadCachedProfiles(): Boolean {
        val stored = decodeStoredPayload() ?: return false
        loadedCacheForUserId = stored.userId
        applyStoredPayload(stored)
        ThemeSettingsRepository.onProfileChanged()
        return _state.value.profiles.isNotEmpty()
    }

    fun ensureLoaded(userId: String) {
        if (loadedCacheForUserId == userId && _state.value.isLoaded) return

        val stored = decodeStoredPayload()
        loadedCacheForUserId = userId
        if (stored == null) {
            val defaultProfile = NuvioProfile(
                id = "default",
                userId = userId,
                profileIndex = 1,
                name = "neXStream",
                avatarColorHex = DEFAULT_PROFILE_COLOR_HEX,
            )
            _state.value = ProfileState(
                profiles = listOf(defaultProfile),
                activeProfile = defaultProfile,
                isLoaded = true,
                hasEverSelectedProfile = true,
            )
            activeProfileIndex = 1
            persist()
            return
        }

        applyStoredPayload(stored)
    }

    fun clearInMemory() {
        loadedCacheForUserId = null
        activeProfileIndex = 1
        _state.value = ProfileState()
    }

    suspend fun pullProfiles() {
        if (!_state.value.isLoaded) {
            val authState = AuthRepository.state.value as? AuthState.Authenticated
            val userId = authState?.userId ?: "local_user"
            ensureLoaded(userId)
        }
    }

    fun selectProfile(profileIndex: Int) {
        activeProfileIndex = profileIndex
        CustomPosterUrlRepository.onProfileChanged()
        val selectedProfile = _state.value.profiles.find { it.profileIndex == profileIndex }
        _state.value = _state.value.copy(
            activeProfile = selectedProfile,
            hasEverSelectedProfile = selectedProfile != null || _state.value.hasEverSelectedProfile,
        )
        persist()
        WatchedRepository.onProfileChanged(profileIndex)
        TrackingSettingsRepository.onProfileChanged()
        ensureTrackingProvidersRegistered()
        TrackingProviderRegistry.onProfileChanged()
        LibraryRepository.onProfileChanged(profileIndex)
        LibraryDisplaySettingsRepository.onProfileChanged()
        WatchProgressRepository.onProfileChanged(profileIndex)
        ThemeSettingsRepository.onProfileChanged()
        com.nuvio.app.core.ui.PosterCardStyleRepository.onProfileChanged()
        com.nuvio.app.core.ui.CardDepthStyleRepository.onProfileChanged()
        com.nuvio.app.features.player.PlayerSettingsRepository.onProfileChanged()
        com.nuvio.app.features.streams.StreamBadgeSettingsRepository.onProfileChanged()
        HomeCatalogSettingsRepository.onProfileChanged()
        val usesPrimary = selectedProfile?.usesPrimaryAddons ?: false
        com.nuvio.app.features.xtream.XtreamRepository.onProfileChanged(profileIndex, usesPrimary)
        HomeRepository.clear()
        MetaScreenSettingsRepository.onProfileChanged()
        com.nuvio.app.features.shuffle.EpisodeShuffleRepository.onProfileChanged()
        ContinueWatchingPreferencesRepository.onProfileChanged()
        com.nuvio.app.features.watchprogress.ContinueWatchingEnrichmentCache.onProfileChanged()
        EpisodeReleaseNotificationsRepository.onProfileChanged()
        TmdbSettingsRepository.onProfileChanged()
        MdbListSettingsRepository.onProfileChanged()
        SearchHistoryRepository.onProfileChanged()
        SearchRepository.reset()
        CollectionRepository.onProfileChanged()
        CollectionMobileSettingsRepository.onProfileChanged()
        DownloadsRepository.onProfileChanged()
        ProfileSettingsSync.onProfileChanged()
    }

    suspend fun pushProfiles(profiles: List<ProfilePushPayload>) {
        applyPayloadsLocally(profiles)
    }

    suspend fun createProfile(
        name: String,
        avatarColorHex: String,
        avatarId: String? = null,
        avatarUrl: String? = null,
        usesPrimaryAddons: Boolean = false,
    ) {
        val existing = _state.value.profiles
        val nextIndex = ((1..MAX_PROFILES).toSet() - existing.map { it.profileIndex }.toSet()).minOrNull() ?: return

        val allPayloads = existing.map { profile ->
            ProfilePushPayload(
                profileIndex = profile.profileIndex,
                name = profile.name,
                avatarColorHex = profile.avatarColorHex,
                usesPrimaryAddons = profile.usesPrimaryAddons,
                usesPrimaryPlugins = profile.usesPrimaryPlugins,
                avatarId = profile.avatarId,
                avatarUrl = profile.avatarUrl,
                profileBackgroundId = profile.profileBackgroundId,
                profileBackgroundUrl = profile.profileBackgroundUrl,
            )
        } + ProfilePushPayload(
            profileIndex = nextIndex,
            name = name,
            avatarColorHex = avatarColorHex,
            avatarId = avatarId,
            avatarUrl = avatarUrl,
            usesPrimaryAddons = usesPrimaryAddons,
            usesPrimaryPlugins = usesPrimaryAddons,
        )

        pushProfiles(allPayloads)
    }

    suspend fun updateProfile(
        profileIndex: Int,
        name: String,
        avatarColorHex: String,
        avatarId: String? = null,
        avatarUrl: String? = null,
        profileBackgroundId: String? = null,
        profileBackgroundUrl: String? = null,
        usesPrimaryAddons: Boolean = false,
        usesPrimaryPlugins: Boolean = false,
    ) {
        val existing = _state.value.profiles
        val allPayloads = existing.map { profile ->
            if (profile.profileIndex == profileIndex) {
                ProfilePushPayload(
                    profileIndex = profileIndex,
                    name = name,
                    avatarColorHex = avatarColorHex,
                    avatarId = avatarId,
                    avatarUrl = avatarUrl,
                    profileBackgroundId = profileBackgroundId,
                    profileBackgroundUrl = profileBackgroundUrl,
                    usesPrimaryAddons = usesPrimaryAddons,
                    usesPrimaryPlugins = usesPrimaryPlugins,
                )
            } else {
                ProfilePushPayload(
                    profileIndex = profile.profileIndex,
                    name = profile.name,
                    avatarColorHex = profile.avatarColorHex,
                    usesPrimaryAddons = profile.usesPrimaryAddons,
                    usesPrimaryPlugins = profile.usesPrimaryPlugins,
                    avatarId = profile.avatarId,
                    avatarUrl = profile.avatarUrl,
                    profileBackgroundId = profile.profileBackgroundId,
                    profileBackgroundUrl = profile.profileBackgroundUrl,
                )
            }
        }

        pushProfiles(allPayloads)
    }

    suspend fun deleteProfile(profileIndex: Int) {
        val remaining = _state.value.profiles.filter { it.profileIndex != profileIndex }
        ProfilePinCacheStorage.removePayload(profileIndex)
        _state.value = _state.value.copy(
            profiles = remaining,
            activeProfile = if (_state.value.activeProfile?.profileIndex == profileIndex) remaining.firstOrNull() else _state.value.activeProfile,
        )
        if (_state.value.activeProfile != null) {
            activeProfileIndex = _state.value.activeProfile!!.profileIndex
        }
        persist()
    }

    suspend fun verifyPin(profileIndex: Int, pin: String): PinVerifyResult {
        return verifyPinLocally(profileIndex, pin)
    }

    suspend fun setPin(profileIndex: Int, pin: String, currentPin: String? = null): PinVerifyResult {
        rememberVerifiedPin(profileIndex = profileIndex, pin = pin)
        val updated = _state.value.profiles.map { p ->
            if (p.profileIndex == profileIndex) p.copy(pinEnabled = true) else p
        }
        _state.value = _state.value.copy(profiles = updated)
        persist()
        return PinVerifyResult(unlocked = true)
    }

    suspend fun clearPin(profileIndex: Int, currentPin: String? = null): PinVerifyResult {
        ProfilePinCacheStorage.removePayload(profileIndex)
        val updated = _state.value.profiles.map { p ->
            if (p.profileIndex == profileIndex) p.copy(pinEnabled = false) else p
        }
        _state.value = _state.value.copy(profiles = updated)
        persist()
        return PinVerifyResult(unlocked = true)
    }

    suspend fun clearPinWithPassword(profileIndex: Int, accountPassword: String) {
        clearPin(profileIndex)
    }

    suspend fun pullProfileLocks(): List<ProfileLockState> {
        return _state.value.profiles.map {
            ProfileLockState(
                profileIndex = it.profileIndex,
                pinEnabled = it.pinEnabled,
            )
        }
    }

    private fun applyPayloadsLocally(payloads: List<ProfilePushPayload>) {
        val authState = AuthRepository.state.value as? AuthState.Authenticated
        val userId = authState?.userId ?: "local_user"
        val existingByIndex = _state.value.profiles.associateBy { it.profileIndex }
        val profiles = payloads.map { p ->
            val existing = existingByIndex[p.profileIndex]
            NuvioProfile(
                id = existing?.id.orEmpty(),
                userId = userId,
                pinEnabled = existing?.pinEnabled ?: false,
                pinLockedUntil = existing?.pinLockedUntil,
                createdAt = existing?.createdAt.orEmpty(),
                updatedAt = existing?.updatedAt.orEmpty(),
                profileIndex = p.profileIndex,
                name = p.name,
                avatarColorHex = p.avatarColorHex,
                avatarId = p.avatarId,
                avatarUrl = p.avatarUrl,
                profileBackgroundId = p.profileBackgroundId,
                profileBackgroundUrl = p.profileBackgroundUrl,
                usesPrimaryAddons = p.usesPrimaryAddons,
                usesPrimaryPlugins = p.usesPrimaryPlugins,
            )
        }.sortedBy { it.profileIndex }
        _state.value = _state.value.copy(
            profiles = profiles,
            isLoaded = true,
            activeProfile = profiles.find { it.profileIndex == activeProfileIndex } ?: profiles.firstOrNull(),
        )
        if (_state.value.activeProfile != null) {
            activeProfileIndex = _state.value.activeProfile!!.profileIndex
        }
        syncPinCache(profiles)
        persist()
    }

    private fun decodeStoredPayload(): StoredProfilePayload? {
        val payload = ProfileStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) return null

        return runCatching {
            json.decodeFromString<StoredProfilePayload>(payload)
        }.getOrNull()
    }

    private fun applyStoredPayload(stored: StoredProfilePayload) {
        val profiles = stored.profiles.sortedBy { it.profileIndex }
        activeProfileIndex = stored.activeProfileIndex
        _state.value = ProfileState(
            profiles = profiles,
            activeProfile = profiles.find { it.profileIndex == activeProfileIndex } ?: profiles.firstOrNull(),
            isLoaded = profiles.isNotEmpty(),
            hasEverSelectedProfile = stored.hasEverSelectedProfile,
            rememberLastProfileEnabled = stored.rememberLastProfileEnabled,
        )
        _state.value.activeProfile?.let {
            activeProfileIndex = it.profileIndex
            com.nuvio.app.features.xtream.XtreamRepository.onProfileChanged(it.profileIndex, it.usesPrimaryAddons)
        }
        syncPinCache(profiles)
    }

    private fun rememberVerifiedPin(profileIndex: Int, pin: String) {
        val profile = _state.value.profiles.find { it.profileIndex == profileIndex }
        val salt = generateProfilePinSalt()
        val payload = CachedProfilePinPayload(
            salt = salt,
            digest = hashProfilePin(profileIndex = profileIndex, salt = salt, pin = pin),
            profileUpdatedAt = profile?.updatedAt.orEmpty(),
        )
        ProfilePinCacheStorage.savePayload(profileIndex, json.encodeToString(payload))
    }

    private fun verifyPinLocally(profileIndex: Int, pin: String): PinVerifyResult {
        val profile = _state.value.profiles.find { it.profileIndex == profileIndex }
        if (profile?.pinEnabled != true) {
            return PinVerifyResult(unlocked = true)
        }

        val payload = ProfilePinCacheStorage.loadPayload(profileIndex).orEmpty().trim()
        if (payload.isEmpty()) {
            return PinVerifyResult(
                unlocked = false,
                message = localizedString(Res.string.profile_pin_offline_verification_requires_online),
            )
        }

        val cached = runCatching {
            json.decodeFromString<CachedProfilePinPayload>(payload)
        }.getOrNull() ?: return PinVerifyResult(
            unlocked = false,
            message = localizedString(Res.string.profile_pin_offline_verification_requires_online),
        )

        val digest = hashProfilePin(profileIndex = profileIndex, salt = cached.salt, pin = pin)
        return if (digest == cached.digest) {
            PinVerifyResult(unlocked = true)
        } else {
            PinVerifyResult(unlocked = false, message = localizedString(Res.string.pin_incorrect))
        }
    }

    private fun syncPinCache(profiles: List<NuvioProfile>) {
        val profilesByIndex = profiles.associateBy { it.profileIndex }
        for (profileIndex in 1..MAX_PROFILES) {
            val profile = profilesByIndex[profileIndex]
            if (profile == null || !profile.pinEnabled) {
                ProfilePinCacheStorage.removePayload(profileIndex)
                continue
            }
        }
    }

    private fun persist() {
        val authState = AuthRepository.state.value as? AuthState.Authenticated
        val userId = authState?.userId ?: "local_user"
        val state = _state.value
        ProfileStorage.savePayload(
            json.encodeToString(
                StoredProfilePayload(
                    userId = userId,
                    activeProfileIndex = activeProfileIndex,
                    hasEverSelectedProfile = state.hasEverSelectedProfile,
                    rememberLastProfileEnabled = state.rememberLastProfileEnabled,
                    profiles = state.profiles,
                ),
            ),
        )
    }
}

@kotlinx.serialization.Serializable
data class ProfileLockState(
    @kotlinx.serialization.SerialName("profile_index") val profileIndex: Int,
    @kotlinx.serialization.SerialName("pin_enabled") val pinEnabled: Boolean = false,
    @kotlinx.serialization.SerialName("pin_locked_until") val pinLockedUntil: String? = null,
)
