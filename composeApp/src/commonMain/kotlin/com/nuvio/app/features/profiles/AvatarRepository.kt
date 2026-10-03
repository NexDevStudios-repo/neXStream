package com.nuvio.app.features.profiles

import co.touchlab.kermit.Logger
import com.nuvio.app.features.membership.CosmeticEntitlement
import com.nuvio.app.features.membership.MemberAccessRepository
import com.nuvio.app.features.membership.MemberAssetStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val AvatarCatalogRefreshInterval = 15.minutes

@Serializable
private data class StoredAvatarCatalogPayload(
    val items: List<AvatarCatalogItem> = emptyList(),
    val memberItems: List<MemberAvatarCatalogItem> = emptyList(),
)

@Serializable
private data class MemberAvatarCatalogItem(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("storage_path") val storagePath: String,
    val category: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("bg_color") val bgColor: String? = null,
    @SerialName("asset_version") val assetVersion: Int,
)

internal fun availableAvatarCatalog(
    standardCatalog: List<AvatarCatalogItem>,
    memberCatalog: List<AvatarCatalogItem>,
    hasMemberAccess: Boolean,
): List<AvatarCatalogItem> = standardCatalog + if (hasMemberAccess) memberCatalog else emptyList()

object AvatarRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("AvatarRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _avatars = MutableStateFlow<List<AvatarCatalogItem>>(emptyList())
    val avatars: StateFlow<List<AvatarCatalogItem>> = _avatars.asStateFlow()

    private var standardCatalog = emptyList<AvatarCatalogItem>()
    private var memberCatalog = emptyList<AvatarCatalogItem>()
    private var memberCatalogMetadata = emptyList<MemberAvatarCatalogItem>()
    private var standardLoaded = false
    private var cacheHydrated = false
    private var accessObserverStarted = false
    private var hasMemberAccess = true

    suspend fun fetchAvatars() {
        hydrateFromCacheIfNeeded()
        ensureMemberAccessObserver()
        if (standardCatalog.isEmpty()) {
            standardCatalog = defaultAvatars()
            standardLoaded = true
            publishCatalog()
        }
    }

    suspend fun refreshAvatars(force: Boolean = false) {
        fetchAvatars()
    }

    private fun defaultAvatars(): List<AvatarCatalogItem> = listOf(
        AvatarCatalogItem(id = "avatar_1", displayName = "Blue", storagePath = "", category = "Default", sortOrder = 1, bgColor = "#2563EB", isActive = true),
        AvatarCatalogItem(id = "avatar_2", displayName = "Purple", storagePath = "", category = "Default", sortOrder = 2, bgColor = "#7C3AED", isActive = true),
        AvatarCatalogItem(id = "avatar_3", displayName = "Emerald", storagePath = "", category = "Default", sortOrder = 3, bgColor = "#059669", isActive = true),
        AvatarCatalogItem(id = "avatar_4", displayName = "Amber", storagePath = "", category = "Default", sortOrder = 4, bgColor = "#D97706", isActive = true),
        AvatarCatalogItem(id = "avatar_5", displayName = "Rose", storagePath = "", category = "Default", sortOrder = 5, bgColor = "#E11D48", isActive = true),
        AvatarCatalogItem(id = "avatar_6", displayName = "Cyan", storagePath = "", category = "Default", sortOrder = 6, bgColor = "#0891B2", isActive = true),
    )

    private fun hydrateFromCacheIfNeeded() {
        if (cacheHydrated) return
        cacheHydrated = true

        val payload = AvatarStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) {
            standardCatalog = defaultAvatars()
            standardLoaded = true
            publishCatalog()
            return
        }

        val stored = runCatching {
            json.decodeFromString<StoredAvatarCatalogPayload>(payload)
        }.getOrNull()

        if (stored != null && stored.items.isNotEmpty()) {
            standardCatalog = stored.items
                .filter { it.isActive }
                .sortedWith(compareBy({ it.category }, { it.sortOrder }))
            memberCatalogMetadata = stored.memberItems
            memberCatalog = memberCatalogMetadata
                .mapNotNull(::loadCachedMemberAvatar)
                .sortedWith(compareBy({ it.category }, { it.sortOrder }))
            standardLoaded = standardCatalog.isNotEmpty()
        } else {
            standardCatalog = defaultAvatars()
            standardLoaded = true
        }
        publishCatalog()
    }

    private fun ensureMemberAccessObserver() {
        if (accessObserverStarted) return
        accessObserverStarted = true
        MemberAccessRepository.ensureStarted()
        hasMemberAccess = true
        publishCatalog()
    }

    private fun loadCachedMemberAvatar(item: MemberAvatarCatalogItem): AvatarCatalogItem? {
        val localImageUrl = MemberAssetStorage.loadProfileAvatar(memberAvatarCacheKey(item)) ?: return null
        return item.toAvatarCatalogItem(localImageUrl)
    }

    private fun memberAvatarCacheKey(item: MemberAvatarCatalogItem): String {
        val extension = item.storagePath.substringAfterLast('.', "img")
            .takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
            ?: "img"
        return "${item.id}-v${item.assetVersion}.$extension"
    }

    private fun MemberAvatarCatalogItem.toAvatarCatalogItem(localImageUrl: String): AvatarCatalogItem =
        AvatarCatalogItem(
            id = id,
            displayName = displayName,
            storagePath = storagePath,
            category = category,
            sortOrder = sortOrder,
            bgColor = bgColor,
            localImageUrl = localImageUrl,
            memberOnly = true,
        )

    private fun publishCatalog() {
        _avatars.value = availableAvatarCatalog(
            standardCatalog = standardCatalog,
            memberCatalog = memberCatalog,
            hasMemberAccess = hasMemberAccess,
        )
    }
}
