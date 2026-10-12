package com.nuvio.app.features.home

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.nuvio.app.features.collection.Collection
import com.nuvio.app.features.collection.CollectionFolder
import com.nuvio.app.features.collection.CollectionRepository
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HomeCatalogSettingsRepositoryTest {
    private lateinit var preferences: RecordingPreferences
    private lateinit var originalLocale: Locale

    @BeforeTest
    fun initialize() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        val context = RuntimeEnvironment.getApplication()
        val stored = context.getSharedPreferences("nuvio_home_catalog_settings", Context.MODE_PRIVATE)
        stored.edit().clear().commit()
        preferences = RecordingPreferences(stored)
        HomeCatalogSettingsStorage.initialize(object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
        })
        CollectionRepository.clearLocalState()
        HomeRepository.clear()
        HomeCatalogSettingsRepository.clearLocalState()
        HomeCatalogSettingsRepository.setHeroEnabled(false)
    }

    @AfterTest
    fun clearState() {
        Locale.setDefault(originalLocale)
        HomeCatalogSettingsRepository.clearLocalState()
        CollectionRepository.clearLocalState()
        HomeRepository.clear()
    }

    @Test
    fun unchangedCollectionsDoNotRewriteSettings() {
        val collections = listOf(collection())
        HomeCatalogSettingsRepository.syncCollections(collections)
        val editCount = preferences.editCount

        HomeCatalogSettingsRepository.syncCollections(collections.map { it.copy() })

        assertEquals(editCount, preferences.editCount)
        assertEquals("Favorites", HomeCatalogSettingsRepository.uiState.value.items.single().defaultTitle)
    }

    @Test
    fun profileChangesRebuildTheSameCollectionInputs() {
        val collections = listOf(collection())
        HomeCatalogSettingsRepository.syncCollections(collections)
        val settings = HomeCatalogSettingsRepository.uiState.value

        HomeCatalogSettingsRepository.onProfileChanged()
        HomeCatalogSettingsRepository.syncCollections(collections)

        assertEquals(settings, HomeCatalogSettingsRepository.uiState.value)
    }

    private fun collection() = Collection(
        id = "favorites",
        title = "Favorites",
        folders = listOf(CollectionFolder(id = "movies", title = "Movies")),
    )

    private class RecordingPreferences(
        private val delegate: SharedPreferences,
    ) : SharedPreferences by delegate {
        var editCount = 0

        override fun edit(): SharedPreferences.Editor {
            editCount += 1
            return delegate.edit()
        }
    }
}

