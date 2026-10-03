package com.nuvio.app.features.membership

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ProfileBackgroundCatalogItem(
    val id: String,
    val displayName: String,
    val landscapeImageBytes: ByteArray? = null,
    val portraitImageBytes: ByteArray? = null,
    val assetVersion: Int,
)

object ProfileBackgroundRepository {
    private val log = Logger.withTag("ProfileBackgroundRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _catalog = MutableStateFlow<List<ProfileBackgroundCatalogItem>>(emptyList())
    val catalog: StateFlow<List<ProfileBackgroundCatalogItem>> = _catalog.asStateFlow()

    fun ensureLoaded() {
        // Local no-op
    }

    fun loadSelectedAndPreload(id: String, portrait: Boolean) {
        // Local no-op
    }

    fun preloadLandscapeImages() {
        // Local no-op
    }

    fun preloadPortraitImages() {
        // Local no-op
    }

    fun invalidate() {
        // Local no-op
    }
}
