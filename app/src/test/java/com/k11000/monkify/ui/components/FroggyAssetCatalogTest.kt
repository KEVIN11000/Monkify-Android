package com.k11000.monkify.ui.components

import com.k11000.monkify.domain.model.FroggyScenery
import com.k11000.monkify.domain.model.WeatherCondition
import com.k11000.monkify.domain.model.ZenDayPart
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FroggyAssetCatalogTest {

    @Test
    fun all_sixty_scenery_combinations_resolve_to_valid_drawable_resources() {
        var count = 0
        for (scenery in FroggyScenery.values()) {
            for (dayPart in ZenDayPart.values()) {
                for (condition in WeatherCondition.values()) {
                    val resId = FroggyAssetCatalog.getBackgroundRes(scenery, dayPart, condition)
                    assertNotEquals(
                        "Resource for $scenery $dayPart $condition was 0",
                        0,
                        resId
                    )
                    count++
                }
            }
        }
        assertEquals(60, count)
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
