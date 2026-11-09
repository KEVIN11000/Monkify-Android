package com.k11000.monkify.ui.components

import com.k11000.monkify.data.model.CosmosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmosAssetCatalogTest {

    @Test
    fun getAvailableThemes_returns_all_expected_variants() {
        val themes = CosmosAssetCatalog.getAvailableThemes()
        assertEquals(5, themes.size)
        assertTrue(themes.contains(CosmosTheme.DEEP_SPACE))
        assertTrue(themes.contains(CosmosTheme.ORION_NEBULA))
        assertTrue(themes.contains(CosmosTheme.ANDROMEDA))
        assertTrue(themes.contains(CosmosTheme.PILLARS_OF_CREATION))
        assertTrue(themes.contains(CosmosTheme.DEEP_FIELD))
    }

    @Test
    fun resolveTheme_maps_exact_and_case_insensitive_ids() {
        assertEquals(CosmosTheme.DEEP_SPACE, CosmosAssetCatalog.resolveTheme("DEEP_SPACE"))
        assertEquals(CosmosTheme.ORION_NEBULA, CosmosAssetCatalog.resolveTheme("ORION_NEBULA"))
        assertEquals(CosmosTheme.ORION_NEBULA, CosmosAssetCatalog.resolveTheme("orion_nebula"))
        assertEquals(CosmosTheme.ANDROMEDA, CosmosAssetCatalog.resolveTheme("Andromeda"))
        assertEquals(CosmosTheme.PILLARS_OF_CREATION, CosmosAssetCatalog.resolveTheme("pillars_of_creation"))
        assertEquals(CosmosTheme.DEEP_FIELD, CosmosAssetCatalog.resolveTheme("deep_field"))
    }

    @Test
    fun resolveTheme_unknown_or_null_id_falls_back_to_deep_space() {
        assertEquals(CosmosTheme.DEEP_SPACE, CosmosAssetCatalog.resolveTheme(null))
        assertEquals(CosmosTheme.DEEP_SPACE, CosmosAssetCatalog.resolveTheme(""))
        assertEquals(CosmosTheme.DEEP_SPACE, CosmosAssetCatalog.resolveTheme("NON_EXISTING_COSMOS"))
    }

    @Test
    fun getFallbackTheme_returns_deep_space() {
        assertEquals(CosmosTheme.DEEP_SPACE, CosmosAssetCatalog.getFallbackTheme())
    }

    @Test
    fun deepSpace_is_purely_procedural_without_asset() {
        assertNull(CosmosTheme.DEEP_SPACE.assetName)
    }
}
