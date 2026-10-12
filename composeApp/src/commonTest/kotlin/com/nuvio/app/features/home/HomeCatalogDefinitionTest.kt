package com.nuvio.app.features.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCatalogDefinitionTest {
    private val definition = HomeCatalogDefinition(
        key = "xtream:movie:popular",
        defaultTitle = "Popular - Películas",
        catalogName = "Popular",
        addonName = "Xtream",
        manifestUrl = "xtream",
        type = "movie",
        catalogId = "popular",
        supportsPagination = true,
        descriptorSignature = "signature",
    )

    @Test
    fun `shows the type suffix by default`() {
        assertEquals("Popular - Películas", definition.titleFor(showCatalogType = true))
    }

    @Test
    fun `omits the type suffix when disabled`() {
        assertEquals("Popular", definition.titleFor(showCatalogType = false))
    }
}

